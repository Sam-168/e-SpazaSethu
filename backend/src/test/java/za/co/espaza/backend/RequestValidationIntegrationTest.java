package za.co.espaza.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import za.co.espaza.backend.controller.ProductController;
import za.co.espaza.backend.exception.GlobalExceptionHandler;
import za.co.espaza.backend.service.ProductService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BE-07/09/11/13/15: invalid input on any endpoint accepting a request body
 * must return 400 Bad Request with field-level messages (field: message),
 * never a 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private String adminToken;
    private String cashierToken;

    @BeforeEach
    void setUpAdmin() throws Exception {
        if (adminToken != null) {
            return;
        }
        String username = "admin-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into user
                    (user_id, username, password_hash, role, is_active, created_at)
                values (?, ?, ?, 'ADMIN', true, current_timestamp)
                """, UUID.randomUUID().toString(), username,
                passwordEncoder.encode("admin-password"));

        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"admin-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        adminToken = "Bearer " + objectMapper.readTree(loginJson).get("token").asText();
    }

    /** Every validation failure must be 400 + field-level message, never 500. */
    private ResultActions expect400(ResultActions actions, String... messageFragments) throws Exception {
        actions.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
        for (String fragment : messageFragments) {
            actions.andExpect(jsonPath("$.message", containsString(fragment)));
        }
        return actions;
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Test
    void loginRejectsBlankUsername() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"   \",\"password\":\"admin-password\"}")),
                "username: Username is required");
    }

    @Test
    void loginRejectsShortPassword() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"someone\",\"password\":\"123\"}")),
                "password: Password must be at least 6 characters");
    }

    @Test
    void loginWithMalformedJsonReturns400() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json")));
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @Test
    void createUserRejectsBlankUsernameAndShortPassword() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"123\",\"role\":\"CASHIER\"}")),
                "username: Username is required",
                "password: Password must be at least 6 characters");
    }

    @Test
    void createUserRejectsMissingRole() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newcashier\",\"password\":\"secret123\"}")),
                "role: Role is required");
    }

    @Test
    void updateUserWithUnknownRoleReturns400() throws Exception {
        expect400(mockMvc.perform(put("/api/v1/users/" + UUID.randomUUID())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPERVISOR\"}")));
    }

    @Test
    void resetPasswordRejectsShortPassword() throws Exception {
        expect400(mockMvc.perform(patch("/api/v1/users/" + UUID.randomUUID() + "/password")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"123\"}")),
                "newPassword: Password must be at least 6 characters");
    }

    // ── Categories ────────────────────────────────────────────────────────────

    @Test
    void createCategoryRejectsBlankName() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}")),
                "name: Category name is required");
    }

    @Test
    void createCategoryRejectsExcessiveNameLength() throws Exception {
        String longName = "C".repeat(101);
        expect400(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\"}")),
                "name: Category name must not exceed 100 characters");
    }

    @Test
    void updateCategoryRejectsBlankName() throws Exception {
        expect400(mockMvc.perform(put("/api/v1/categories/" + UUID.randomUUID())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}")),
                "name: Category name must not be blank");
    }

    @Test
    void updateCategoryWithOmittedNameStillPassesValidation() throws Exception {
        // Partial update: name absent (null) is valid — reaches the service and
        // fails only because the category does not exist.
        mockMvc.perform(put("/api/v1/categories/" + UUID.randomUUID())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"only the description\"}"))
                .andExpect(status().isNotFound());
    }

    // ── Products ──────────────────────────────────────────────────────────────

    @Test
    void createProductRejectsBlankNameAndNegativePrice() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"sellingPrice\":-5,\"costPrice\":10}")),
                "name: Product name is required",
                "sellingPrice: Selling price must be 0 or more");
    }

    @Test
    void createProductRejectsMissingSellingPrice() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chips\"}")),
                "sellingPrice: Selling price is required");
    }

    @Test
    void createProductAcceptsOmittedCostPrice() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Product Without Cost Price\",\"sellingPrice\":19.99}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Product Without Cost Price"))
                .andExpect(jsonPath("$.sellingPrice").value(19.99))
                .andExpect(jsonPath("$.costPrice").doesNotExist());
    }

    @Test
    void createProductRejectsNegativeCostPrice() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Negative Cost Item\",\"sellingPrice\":10,\"costPrice\":-5}")),
                "costPrice: Cost price must be 0 or more");
    }

    @Test
    void createProductRejectsExcessiveNameLength() throws Exception {
        String longName = "A".repeat(201);
        expect400(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\",\"sellingPrice\":10}")),
                "name: Product name must not exceed 200 characters");
    }

    @Test
    void createProductRejectsExcessiveBarcodeLength() throws Exception {
        String longBarcode = "1".repeat(101);
        expect400(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Long Barcode Item\",\"sellingPrice\":10,\"barcode\":\"" + longBarcode + "\"}")),
                "barcode: Barcode must not exceed 100 characters");
    }

    @Test
    void createProductRejectsInvalidBarcodeFormat() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad Barcode Item\",\"sellingPrice\":10,\"barcode\":\"INVALID BARCODE!\"}")),
                "barcode: Barcode must be alphanumeric");
    }

    @Test
    void createProductPreservesLeadingZerosOnBarcode() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Leading Zero Item\",\"sellingPrice\":15.00,\"barcode\":\"000123456\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.barcode").value("000123456"));
    }

    @Test
    void updateProductRejectsNegativePrice() throws Exception {
        expect400(mockMvc.perform(put("/api/v1/products/" + UUID.randomUUID())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sellingPrice\":-1}")),
                "sellingPrice: Selling price must be 0 or more");
    }

    @Test
    void updateProductWithOmittedNameStillPassesValidation() throws Exception {
        // Partial update: name absent (null) is valid — reaches the service and
        // fails only because the product does not exist.
        mockMvc.perform(put("/api/v1/products/" + UUID.randomUUID())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"restocked\"}"))
                .andExpect(status().isNotFound());
    }

    // ── Sales ─────────────────────────────────────────────────────────────────

    @Test
    void createSaleRejectsEmptyItems() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CASH\",\"items\":[]}")),
                "items: A sale must contain at least one item");
    }

    @Test
    void createSaleRejectsMissingPaymentMethod() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":1}]}")),
                "paymentMethod: Payment method is required");
    }

    @Test
    void createSaleRejectsZeroQuantity() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CASH\",\"items\":[{\"productId\":\""
                                + UUID.randomUUID() + "\",\"quantity\":0}]}")),
                "items[0].quantity: Quantity must be at least 1");
    }

    @Test
    void createSaleWithUnknownPaymentMethodReturns400() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/sales")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"BITCOIN\",\"items\":[{\"productId\":\""
                                + UUID.randomUUID() + "\",\"quantity\":1}]}")));
    }

    // ── Stock movements ───────────────────────────────────────────────────────

    @Test
    void manualAdjustmentRejectsBlankFields() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/movements/adjustment")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"\",\"quantityChange\":null,\"notes\":\"  \"}")),
                "productId: Product id is required",
                "quantityChange: Quantity change is required",
                "notes: Adjustment notes are required");
    }

    // ── Stocktake ─────────────────────────────────────────────────────────────

    @Test
    void submitStocktakeCountsRejectsNullItemIdAndNegativeCount() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/stocktakes/" + UUID.randomUUID() + "/items")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"countedQuantity\":-1}]")),
                "stocktakeItemId: Stocktake item id is required",
                "countedQuantity: Counted quantity must be 0 or more");
    }

    // ── Reports (query parameter validation) ──────────────────────────────────

    @Test
    void summaryRejectsMissingDateParameters() throws Exception {
        expect400(mockMvc.perform(get("/api/v1/reports/summary")
                        .header("Authorization", adminToken)),
                "from: parameter is required");
    }

    // ── 401 Unauthorized ──────────────────────────────────────────────────────

    @Test
    void getProductsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/products"));
    }

    @Test
    void getProductsWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer invalid-token-string"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void loginWithWrongCredentialsReturns401Json() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unknown-user\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Incorrect username or password"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── 403 Forbidden ─────────────────────────────────────────────────────────

    private String createCashierToken() throws Exception {
        if (cashierToken != null) {
            return cashierToken;
        }
        String username = "cashier-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into user
                    (user_id, username, password_hash, role, is_active, created_at)
                values (?, ?, ?, 'CASHIER', true, current_timestamp)
                """, UUID.randomUUID().toString(), username,
                passwordEncoder.encode("cashier-password"));

        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"cashier-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        cashierToken = "Bearer " + objectMapper.readTree(loginJson).get("token").asText();
        return cashierToken;
    }

    @Test
    void cashierCallingCreateProductReturns403() throws Exception {
        String cashierToken = createCashierToken();
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unauthorized Product\",\"sellingPrice\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void cashierCallingCreateCategoryReturns403() throws Exception {
        String cashierToken = createCashierToken();
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unauthorized Category\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action"));
    }

    @Test
    void cashierCallingCreateUserReturns403() throws Exception {
        String cashierToken = createCashierToken();
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", cashierToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cashier2\",\"password\":\"pass123\",\"role\":\"CASHIER\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // ── 404 Not Found ─────────────────────────────────────────────────────────

    @Test
    void getProductWithUnknownIdReturns404() throws Exception {
        String unknownId = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/v1/products/" + unknownId)
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString(unknownId)))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/products/" + unknownId));
    }

    @Test
    void updateCategoryWithUnknownIdReturns404() throws Exception {
        String unknownId = UUID.randomUUID().toString();
        mockMvc.perform(put("/api/v1/categories/" + unknownId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Nonexistent Category\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString(unknownId)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createProductWithUnknownCategoryReturns404() throws Exception {
        String unknownCatId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Product with Nonexistent Category\",\"sellingPrice\":10,\"categoryId\":\"" + unknownCatId + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString(unknownCatId)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── 409 Conflict (Duplicates) ─────────────────────────────────────────────

    @Test
    void createProductWithDuplicateBarcodeReturns409() throws Exception {
        String barcode = "600" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Product Alpha\",\"sellingPrice\":10,\"barcode\":\"" + barcode + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Product Beta\",\"sellingPrice\":20,\"barcode\":\"" + barcode + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", containsString(barcode)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createUserWithDuplicateUsernameReturns409() throws Exception {
        String dupUsername = "user-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + dupUsername + "\",\"password\":\"pass123\",\"role\":\"CASHIER\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + dupUsername + "\",\"password\":\"pass456\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message", containsString(dupUsername)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── Invalid date ranges on sales and reports endpoints ────────────────────

    @Test
    void salesRejectsReversedDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/sales?from=2026-12-31&to=2026-01-01")
                        .header("Authorization", adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message", containsString("'from' date cannot be after 'to' date")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void salesRejectsMalformedDateParameter() throws Exception {
        mockMvc.perform(get("/api/v1/sales?from=not-a-valid-date")
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("from: Invalid value")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void reportSummaryRejectsReversedDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary?from=2026-12-31&to=2026-01-01")
                        .header("Authorization", adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message", containsString("'from' date cannot be after 'to' date")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void reportSummaryRejectsMalformedDateParameter() throws Exception {
        mockMvc.perform(get("/api/v1/reports/summary?from=not-a-valid-date&to=2026-01-01")
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("from: Invalid value")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void reportTopProductsByQuantityRejectsReversedDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-products/by-quantity?from=2026-12-31&to=2026-01-01")
                        .header("Authorization", adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message", containsString("'from' date cannot be after 'to' date")));
    }

    @Test
    void reportTopProductsByRevenueRejectsReversedDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-products/by-revenue?from=2026-12-31&to=2026-01-01")
                        .header("Authorization", adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message", containsString("'from' date cannot be after 'to' date")));
    }

    // ── Error Response Contract Shape & Security ──────────────────────────────

    // ── Persistence Guard: Invalid Input Fails Before Anything Is Saved ───────

    @Test
    void invalidCreateProductDoesNotPersistToDatabase() throws Exception {
        int initialCount = jdbcTemplate.queryForObject("select count(*) from product", Integer.class);
        expect400(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rollback Item\",\"sellingPrice\":15,\"costPrice\":-10}")),
                "costPrice: Cost price must be 0 or more");
        int afterCount = jdbcTemplate.queryForObject("select count(*) from product", Integer.class);
        assertThat(afterCount).isEqualTo(initialCount);
    }

    @Test
    void duplicateBarcodeCreateProductDoesNotPersistToDatabase() throws Exception {
        String barcode = "600" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Persisted Product\",\"sellingPrice\":25,\"barcode\":\"" + barcode + "\"}"))
                .andExpect(status().isCreated());

        int countBefore = jdbcTemplate.queryForObject("select count(*) from product", Integer.class);
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Duplicate Rejected Product\",\"sellingPrice\":30,\"barcode\":\"" + barcode + "\"}"))
                .andExpect(status().isConflict());

        int countAfter = jdbcTemplate.queryForObject("select count(*) from product", Integer.class);
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void invalidCreateCategoryDoesNotPersistToDatabase() throws Exception {
        int initialCount = jdbcTemplate.queryForObject("select count(*) from category", Integer.class);
        expect400(mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}")),
                "name: Category name is required");
        int afterCount = jdbcTemplate.queryForObject("select count(*) from category", Integer.class);
        assertThat(afterCount).isEqualTo(initialCount);
    }

    @Test
    void invalidUpdateProductDoesNotModifyDatabase() throws Exception {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into product (product_id, name, selling_price, cost_price, stock_quantity, is_active, created_at, updated_at)
                values (?, 'Unchanged Bread', 50.00, 30.00, 10, true, current_timestamp, current_timestamp)
                """, id);

        expect400(mockMvc.perform(put("/api/v1/products/" + id)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sellingPrice\":-1}")),
                "sellingPrice: Selling price must be 0 or more");

        String name = jdbcTemplate.queryForObject("select name from product where product_id = ?", String.class, id);
        java.math.BigDecimal price = jdbcTemplate.queryForObject("select selling_price from product where product_id = ?", java.math.BigDecimal.class, id);
        assertThat(name).isEqualTo("Unchanged Bread");
        assertThat(price).isEqualByComparingTo(new java.math.BigDecimal("50.00"));
    }

    // ── Acceptance Criterion: No Stack Trace, Class Names, SQL, or Secrets Leaked ──

    private void assertNoLeaks(String responseBody) {
        assertThat(responseBody)
                .doesNotContain("\tat ")
                .doesNotContain("at za.co")
                .doesNotContain("Exception")
                .doesNotContain("java.")
                .doesNotContain("org.hibernate")
                .doesNotContain("org.springframework")
                .doesNotContain("SQLException")
                .doesNotContain("ConstraintViolation")
                .doesNotContain("SELECT")
                .doesNotContain("INSERT")
                .doesNotContain("jdbc:")
                .doesNotContain("admin123")
                .doesNotContain(jwtSecret);
    }

    @Test
    void noLeaksOnMalformedJson400() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andReturn().getResponse().getContentAsString();
        assertNoLeaks(body);
    }

    @Test
    void noLeaksOnInvalidField400() throws Exception {
        String body = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"sellingPrice\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andReturn().getResponse().getContentAsString();
        assertNoLeaks(body);
    }

    @Test
    void noLeaksOnUnauthorized401() throws Exception {
        String body = mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andReturn().getResponse().getContentAsString();
        assertNoLeaks(body);
    }

    @Test
    void noLeaksOnForbidden403() throws Exception {
        String cashier = createCashierToken();
        String body = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cashier Attempt\",\"sellingPrice\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andReturn().getResponse().getContentAsString();
        assertNoLeaks(body);
    }

    @Test
    void noLeaksOnNotFound404() throws Exception {
        String body = mockMvc.perform(get("/api/v1/products/" + UUID.randomUUID())
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andReturn().getResponse().getContentAsString();
        assertNoLeaks(body);
    }

    @Test
    void noLeaksOnConflict409() throws Exception {
        String dupUsername = "leak-user-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + dupUsername + "\",\"password\":\"pass123\",\"role\":\"CASHIER\"}"))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + dupUsername + "\",\"password\":\"pass456\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andReturn().getResponse().getContentAsString();
        assertNoLeaks(body);
    }

    @Test
    void noLeaksOnInternalServerError500() throws Exception {
        ProductService mockService = mock(ProductService.class);
        when(mockService.getProductById(anyString()))
                .thenThrow(new RuntimeException("Sensitive database failure: SELECT * FROM secrets WHERE key = 'admin123' and jdbc:mysql://internal"));

        MockMvc standaloneMockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(mockService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String body500 = standaloneMockMvc.perform(get("/api/v1/products/force-500-leak-check"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Something went wrong on our end. Please try again."))
                .andReturn().getResponse().getContentAsString();

        assertNoLeaks(body500);
    }
}
