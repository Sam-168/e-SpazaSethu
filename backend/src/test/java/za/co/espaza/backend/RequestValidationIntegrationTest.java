package za.co.espaza.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
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

    private String adminToken;

    @BeforeEach
    void setUpAdmin() throws Exception {
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
    void createProductRejectsMissingPrices() throws Exception {
        expect400(mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chips\"}")),
                "sellingPrice: Selling price is required",
                "costPrice: Cost price is required");
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
}
