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
