package za.co.espaza.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String username;
    private String productId;
    private String userId;

    @BeforeEach
    void setUpCheckoutData() {
        username = "cashier-" + UUID.randomUUID();
        userId = UUID.randomUUID().toString();
        productId = UUID.randomUUID().toString();

        jdbcTemplate.update("""
                insert into user
                    (user_id, username, password_hash, role, is_active, created_at)
                values (?, ?, ?, 'CASHIER', true, current_timestamp)
                """, userId, username, passwordEncoder.encode("checkout-password"));

        jdbcTemplate.update("""
                insert into product
                    (product_id, name, selling_price, cost_price, stock_quantity,
                     low_stock_threshold, is_active, created_at, updated_at)
                values (?, 'Test Bread', 12.50, 8.00, 10, 2, true,
                        current_timestamp, current_timestamp)
                """, productId);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void loginCheckoutAndDashboardWorkEndToEnd() throws Exception {
        String loginBody = objectMapper.createObjectNode()
                .put("username", username)
                .put("password", "checkout-password")
                .toString();

        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.role").value("CASHIER"))
                .andReturn().getResponse().getContentAsString();

        JsonNode login = objectMapper.readTree(loginJson);
        String bearerToken = "Bearer " + login.get("token").asText();

        var checkout = objectMapper.createObjectNode();
        checkout.put("paymentMethod", "CASH");
        checkout.putArray("items")
                .addObject()
                .put("productId", productId)
                .put("quantity", 2);
        String checkoutBody = checkout.toString();

        mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cashierName").value(username))
                .andExpect(jsonPath("$.totalAmount").value(25.0))
                .andExpect(jsonPath("$.items[0].productName").value("Test Bread"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        assertThat(jdbcTemplate.queryForObject(
                "select stock_quantity from product where product_id = ?", Integer.class, productId))
                .isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "select quantity_change from stock_movement where product_id = ?",
                Integer.class, productId)).isEqualTo(-2);
        assertThat(jdbcTemplate.queryForObject(
                "select created_by from stock_movement where product_id = ?",
                String.class, productId)).isEqualTo(userId);

        mockMvc.perform(get("/api/v1/reports/dashboard")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSales").value(25.0))
                .andExpect(jsonPath("$.totalProfit").value(9.0))
                .andExpect(jsonPath("$.transactionCount").value(1));
    }

    @Test
    void checkoutRejectsInsufficientStockWithoutChangingInventory() throws Exception {
        var checkout = objectMapper.createObjectNode();
        checkout.put("paymentMethod", "CARD");
        checkout.putArray("items").addObject().put("productId", productId).put("quantity", 11);

        mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", login(username, "checkout-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient stock")));

        assertThat(jdbcTemplate.queryForObject(
                "select stock_quantity from product where product_id = ?", Integer.class, productId))
                .isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from stock_movement where product_id = ?", Integer.class, productId))
                .isZero();
    }

    @Test
    void retryingTheSameCheckoutDoesNotDuplicateTheSaleOrStockMovement() throws Exception {
        String requestId = UUID.randomUUID().toString();
        var checkout = objectMapper.createObjectNode().put("clientRequestId", requestId);
        checkout.put("paymentMethod", "CARD");
        checkout.putArray("items").addObject().put("productId", productId).put("quantity", 2);
        String token = login(username, "checkout-password");

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/sales").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(checkout.toString()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.saleId").value(requestId));
        }

        assertThat(jdbcTemplate.queryForObject(
                "select stock_quantity from product where product_id = ?", Integer.class, productId)).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from stock_movement where reference_id = ?", Integer.class, requestId)).isEqualTo(1);
    }

    @Test
    void adminCanDeactivateListAndReactivateAProduct() throws Exception {
        String adminUsername = "lifecycle-admin-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into user (user_id, username, password_hash, role, is_active, created_at)
                values (?, ?, ?, 'ADMIN', true, current_timestamp)
                """, UUID.randomUUID().toString(), adminUsername, passwordEncoder.encode("admin-password"));
        String token = login(adminUsername, "admin-password");

        mockMvc.perform(patch("/api/v1/products/{id}/deactivate", productId).header("Authorization", token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/products").param("search", "Test Bread").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == '" + productId + "')]").isEmpty());
        mockMvc.perform(get("/api/v1/products/admin/all").param("search", "Test Bread")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == '" + productId + "')].isActive")
                        .value(org.hamcrest.Matchers.contains(false)));
        mockMvc.perform(patch("/api/v1/products/{id}/reactivate", productId).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isActive").value(true));
        mockMvc.perform(get("/api/v1/products").param("search", "Test Bread").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == '" + productId + "')].isActive")
                        .value(org.hamcrest.Matchers.contains(true)));
    }

    @Test
    void storeSettingsPersistAndCashiersCannotChangeThem() throws Exception {
        String adminUsername = "settings-admin-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into user (user_id, username, password_hash, role, is_active, created_at)
                values (?, ?, ?, 'ADMIN', true, current_timestamp)
                """, UUID.randomUUID().toString(), adminUsername, passwordEncoder.encode("admin-password"));
        String adminToken = login(adminUsername, "admin-password");
        String cashierToken = login(username, "checkout-password");
        String settings = objectMapper.createObjectNode()
                .put("shopName", "Test Spaza").put("currencyCode", "ZAR").toString();

        mockMvc.perform(put("/api/v1/settings/store").header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON).content(settings))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/settings/store").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(settings))
                .andExpect(status().isOk()).andExpect(jsonPath("$.shopName").value("Test Spaza"));
        mockMvc.perform(get("/api/v1/settings/store").header("Authorization", cashierToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currencyCode").value("ZAR"));
    }
    @Test
    void adminCancellationRestoresStockOnceAndCreatesAnAuditMovement() throws Exception {
        String adminUsername = "cancel-admin-" + UUID.randomUUID();
        String adminId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into user (user_id, username, password_hash, role, is_active, created_at)
                values (?, ?, ?, 'ADMIN', true, current_timestamp)
                """, adminId, adminUsername, passwordEncoder.encode("admin-password"));

        var checkout = objectMapper.createObjectNode();
        checkout.put("paymentMethod", "CASH");
        checkout.putArray("items").addObject().put("productId", productId).put("quantity", 3);
        String response = mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", login(username, "checkout-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(checkout.toString()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String saleId = objectMapper.readTree(response).get("saleId").asText();

        mockMvc.perform(patch("/api/v1/sales/{id}/cancel", saleId)
                        .header("Authorization", login(username, "checkout-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Customer return\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/sales/{id}/cancel", saleId)
                        .header("Authorization", login(adminUsername, "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Customer return\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.notes").value(org.hamcrest.Matchers.containsString("Customer return")));

        assertThat(jdbcTemplate.queryForObject(
                "select stock_quantity from product where product_id = ?", Integer.class, productId)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject("""
                select quantity_change from stock_movement
                where product_id = ? and reference_id = ? and quantity_change > 0
                """, Integer.class, productId, saleId)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("""
                select created_by from stock_movement
                where product_id = ? and reference_id = ? and quantity_change > 0
                """, String.class, productId, saleId)).isEqualTo(adminId);

        mockMvc.perform(patch("/api/v1/sales/{id}/cancel", saleId)
                        .header("Authorization", login(adminUsername, "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Second attempt\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbcTemplate.queryForObject(
                "select stock_quantity from product where product_id = ?", Integer.class, productId)).isEqualTo(10);
    }
    @Test
    void adminCanAdjustStockAndCashierCannotAccessTheAuditTrail() throws Exception {
        String adminUsername = "stock-admin-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into user (user_id, username, password_hash, role, is_active, created_at)
                values (?, ?, ?, 'ADMIN', true, current_timestamp)
                """, UUID.randomUUID().toString(), adminUsername, passwordEncoder.encode("admin-password"));
        String adminToken = login(adminUsername, "admin-password");
        String cashierToken = login(username, "checkout-password");
        var adjustment = objectMapper.createObjectNode()
                .put("productId", productId).put("quantityChange", 5).put("notes", "Delivery received");

        mockMvc.perform(get("/api/v1/movements").header("Authorization", cashierToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/movements/adjustment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(adjustment.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Test Bread"))
                .andExpect(jsonPath("$.username").value(adminUsername))
                .andExpect(jsonPath("$.quantityChange").value(5));
        assertThat(jdbcTemplate.queryForObject(
                "select stock_quantity from product where product_id = ?", Integer.class, productId)).isEqualTo(15);

        adjustment.put("quantityChange", -20);
        mockMvc.perform(post("/api/v1/movements/adjustment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(adjustment.toString()))
                .andExpect(status().isUnprocessableEntity());
        adjustment.put("quantityChange", 0);
        mockMvc.perform(post("/api/v1/movements/adjustment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(adjustment.toString()))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbcTemplate.queryForObject(
                "select stock_quantity from product where product_id = ?", Integer.class, productId)).isEqualTo(15);

        mockMvc.perform(get("/api/v1/movements")
                        .param("productId", productId).param("type", "ADJUSTMENT")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].notes").value("Delivery received"));
    }
    @Test
    void salesHistoryRejectsAnInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/sales")
                        .param("from", "2026-10-24")
                        .param("to", "2026-10-01")
                        .header("Authorization", login(username, "checkout-password")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("'from' date cannot be after 'to' date"));
    }
    @Test
    void registrationIsPrivateAndAdminCanCreateAWorkingCashierAccount() throws Exception {
        mockMvc.perform(get("/api/v1/auth/setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(false));

        mockMvc.perform(post("/api/v1/auth/setup-owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("username", "public-admin")
                                .put("password", "not-allowed")
                                .toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Owner setup is already complete"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("username", "anonymous-user")
                                .put("password", "Password123")
                                .put("role", "CASHIER")
                                .toString()))
                .andExpect(status().isUnauthorized());

        String adminUsername = "admin-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into user
                    (user_id, username, password_hash, role, is_active, created_at)
                values (?, ?, ?, 'ADMIN', true, current_timestamp)
                """, UUID.randomUUID().toString(), adminUsername, passwordEncoder.encode("admin-password"));

        String adminToken = login(adminUsername, "admin-password");
        String cashierToken = login(username, "checkout-password");
        String newUsername = "new-cashier-" + UUID.randomUUID();
        String createUserBody = objectMapper.createObjectNode()
                .put("username", newUsername)
                .put("password", "Cashier@123")
                .put("role", "CASHIER")
                .toString();

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(newUsername))
                .andExpect(jsonPath("$.role").value("CASHIER"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        login(newUsername, "Cashier@123");

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserBody))
                .andExpect(status().isConflict());
    }

    @Test
    void loginAndAccountCreationValidateInput() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        String adminUsername = "validation-admin-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into user
                    (user_id, username, password_hash, role, is_active, created_at)
                values (?, ?, ?, 'ADMIN', true, current_timestamp)
                """, UUID.randomUUID().toString(), adminUsername, passwordEncoder.encode("admin-password"));

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", login(adminUsername, "admin-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("username", "x")
                                .put("password", "short")
                                .put("role", "CASHIER")
                                .toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void authenticatedUserCanChangeTheirOwnPassword() throws Exception {
        String token = login(username, "checkout-password");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("currentPassword", "checkout-password")
                                .put("newPassword", "ChangedPassword@2026")
                                .toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("username", username)
                                .put("password", "checkout-password")
                                .toString()))
                .andExpect(status().isUnauthorized());

        login(username, "ChangedPassword@2026");
    }

    @Test
    void adminCanManageCatalogueAndCashierCanSearchByBarcode() throws Exception {
        String adminUsername = "catalogue-admin-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into user (user_id, username, password_hash, role, is_active, created_at)
                values (?, ?, ?, 'ADMIN', true, current_timestamp)
                """, UUID.randomUUID().toString(), adminUsername, passwordEncoder.encode("admin-password"));
        String adminToken = login(adminUsername, "admin-password");
        String cashierToken = login(username, "checkout-password");

        String categoryJson = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode().put("name", "Cold Drinks").toString()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String categoryId = objectMapper.readTree(categoryJson).get("categoryId").asText();
        String barcode = "600" + System.nanoTime();
        var product = objectMapper.createObjectNode()
                .put("name", "Ginger Drink").put("barcode", barcode)
                .put("sellingPrice", 18.50).put("costPrice", 11.00)
                .put("stockQuantity", 2).put("lowStockThreshold", 3)
                .put("categoryId", categoryId);

        mockMvc.perform(post("/api/v1/products").header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON).content(product.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/products").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(product.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ginger Drink"))
                .andExpect(jsonPath("$.lowStock").value(true));
        mockMvc.perform(get("/api/v1/products").param("search", barcode)
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].barcode").value(barcode));
        mockMvc.perform(get("/api/v1/products").param("search", barcode.substring(0, 6))
                        .header("Authorization", cashierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].barcode").value(barcode));
        mockMvc.perform(post("/api/v1/products").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"sellingPrice\":0}"))
                .andExpect(status().isBadRequest());
    }
    private String login(String loginUsername, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("username", loginUsername)
                                .put("password", password)
                                .toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return "Bearer " + objectMapper.readTree(response).get("token").asText();
    }

}
