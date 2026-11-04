package za.co.espaza.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.espaza.backend.dto.request.UpdateStoreSettingsRequest;
import za.co.espaza.backend.dto.response.StoreSettingsResponse;

@Service
public class StoreSettingsService {
    private static final String DEFAULT_NAME = "e-SpazaSethu";
    private static final String DEFAULT_CURRENCY = "ZAR";
    private final JdbcTemplate jdbc;

    public StoreSettingsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void ensureTable() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS store_settings (
                    settings_id INT NOT NULL PRIMARY KEY,
                    shop_name VARCHAR(120) NOT NULL,
                    currency_code VARCHAR(3) NOT NULL
                )
                """);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM store_settings WHERE settings_id = 1", Integer.class);
        if (count != null && count == 0) {
            jdbc.update("INSERT INTO store_settings (settings_id, shop_name, currency_code) VALUES (1, ?, ?)",
                    DEFAULT_NAME, DEFAULT_CURRENCY);
        }
    }

    @Transactional(readOnly = true)
    public StoreSettingsResponse get() {
        return jdbc.queryForObject(
                "SELECT shop_name, currency_code FROM store_settings WHERE settings_id = 1",
                (result, row) -> new StoreSettingsResponse(result.getString("shop_name"),
                        result.getString("currency_code")));
    }

    @Transactional
    public StoreSettingsResponse update(UpdateStoreSettingsRequest request) {
        jdbc.update("UPDATE store_settings SET shop_name = ?, currency_code = ? WHERE settings_id = 1",
                request.shopName().trim(), request.currencyCode().trim().toUpperCase());
        return get();
    }
}
