package za.co.espaza.backend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import za.co.espaza.backend.entity.SaleItem;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SaleCostSnapshotService {
    private final JdbcTemplate jdbc;

    public SaleCostSnapshotService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void ensureTable() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS sale_item_cost_snapshot (
                    sale_item_id VARCHAR(36) NOT NULL PRIMARY KEY,
                    cost_price DECIMAL(10, 2) NOT NULL
                )
                """);
    }

    public void record(SaleItem item, BigDecimal costPrice) {
        jdbc.update("INSERT INTO sale_item_cost_snapshot (sale_item_id, cost_price) VALUES (?, ?)",
                item.getSaleItemId().toString(), costPrice == null ? BigDecimal.ZERO : costPrice);
    }

    public BigDecimal costFor(SaleItem item, BigDecimal fallback) {
        List<BigDecimal> costs = jdbc.query("SELECT cost_price FROM sale_item_cost_snapshot WHERE sale_item_id = ?",
                (result, row) -> result.getBigDecimal(1), item.getSaleItemId().toString());
        return costs.isEmpty() ? fallback : costs.get(0);
    }
}
