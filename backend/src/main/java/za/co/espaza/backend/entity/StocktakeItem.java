package za.co.espaza.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * A per-product count entry within a {@link Stocktake} (table {@code stocktake_item}).
 *
 * <p>Backend Issue 15. {@code countedQuantity} and {@code discrepancy} are nullable:
 * a product that has not been counted yet has {@code null} for both (never {@code 0}).
 * {@code adjusted} flips to {@code true} once the correction has been applied.</p>
 *
 * <p>The {@code product} association cannot be mapped yet — Backend Issue 2 must
 * create the {@code Product} entity first. Until then the foreign key is stored as
 * the raw {@code productId} column.</p>
 */
@Entity
@Table(name = "stocktake_item")
public class StocktakeItem {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "stocktakeItemId", length = 36, nullable = false, updatable = false)
    private UUID stocktakeItemId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stocktakeId", nullable = false)
    private Stocktake stocktake;

    /**
     * Read-only view of the owning stocktake's foreign key. It exists alongside the
     * {@link #stocktake} association so the derived query {@code findByStocktakeId(...)}
     * resolves directly to this column without needing a join. Only the association writes it.
     */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "stocktakeId", length = 36, nullable = false, insertable = false, updatable = false)
    private UUID stocktakeId;

    // TODO (Backend Issue 2): add the lazy @ManyToOne Product association here.
    @Column(name = "productId", length = 36, nullable = false)
    private String productId;

    @Column(name = "systemQuantity", nullable = false)
    private int systemQuantity;

    /** Nullable — a product not counted yet has no value (must not default to 0). */
    @Column(name = "countedQuantity")
    private Integer countedQuantity;

    /** Nullable — only set once {@link #countedQuantity} is known (counted - system). */
    @Column(name = "discrepancy")
    private Integer discrepancy;

    @Column(name = "adjusted", nullable = false)
    private boolean adjusted = false;

    /** Required by JPA. */
    protected StocktakeItem() {
    }

    public StocktakeItem(String productId, int systemQuantity) {
        this.stocktakeItemId = UUID.randomUUID();
        this.productId = productId;
        this.systemQuantity = systemQuantity;
    }

    @PrePersist
    void onPrePersist() {
        if (stocktakeItemId == null) {
            stocktakeItemId = UUID.randomUUID();
        }
    }

    public void setStocktake(Stocktake stocktake) {
        this.stocktake = stocktake;
    }

    /**
     * Records the physical count and derives the discrepancy.
     * A {@code null} count clears the discrepancy (the item was skipped).
     */
    public void setCountedQuantity(Integer countedQuantity) {
        this.countedQuantity = countedQuantity;
        this.discrepancy = (countedQuantity == null)
                ? null
                : countedQuantity - this.systemQuantity;
    }

    /** Marks this item as corrected (called when the adjustment is applied). */
    public void applyAdjustment() {
        this.adjusted = true;
    }

    public UUID getStocktakeItemId() {
        return stocktakeItemId;
    }

    public UUID getStocktakeId() {
        return stocktake != null ? stocktake.getStocktakeId() : stocktakeId;
    }

    public String getProductId() {
        return productId;
    }

    public int getSystemQuantity() {
        return systemQuantity;
    }

    public Integer getCountedQuantity() {
        return countedQuantity;
    }

    public Integer getDiscrepancy() {
        return discrepancy;
    }

    public boolean isAdjusted() {
        return adjusted;
    }
}
