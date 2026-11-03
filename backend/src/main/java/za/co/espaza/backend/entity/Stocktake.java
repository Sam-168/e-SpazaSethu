package za.co.espaza.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import za.co.espaza.backend.enums.StocktakeStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A stocktake session — one row per count (table {@code stocktake}).
 *
 * <p>Backend Issue 15. The conducting {@code user} is currently held as the
 * {@code conductedBy} foreign-key column. Once Backend Issue 1 provides the
 * {@code User} entity this must become {@code @ManyToOne @JoinColumn(name = "conductedBy")}.
 * No placeholder {@code User} type is introduced here.</p>
 */
@Entity
@Table(name = "stocktake")
public class Stocktake {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "stocktakeId", length = 36, nullable = false, updatable = false)
    private UUID stocktakeId;

    // TODO (Backend Issue 1): replace with @ManyToOne @JoinColumn(name = "conductedBy")
    // once the User entity exists. Stored as the raw FK column until then.
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "conductedBy", length = 36, nullable = false)
    private UUID conductedBy;

    @Column(name = "startedAt", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completedAt")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StocktakeStatus status = StocktakeStatus.IN_PROGRESS;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "stocktake", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StocktakeItem> items = new ArrayList<>();

    /** Required by JPA. */
    protected Stocktake() {
    }

    public Stocktake(UUID conductedBy, String notes) {
        this.stocktakeId = UUID.randomUUID();
        this.conductedBy = conductedBy;
        this.notes = notes;
        this.startedAt = LocalDateTime.now();
    }

    @PrePersist
    void onPrePersist() {
        if (stocktakeId == null) {
            stocktakeId = UUID.randomUUID();
        }
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = StocktakeStatus.IN_PROGRESS;
        }
    }

    /** Adds an item and wires the back-reference so cascade persists it. */
    public void addItem(StocktakeItem item) {
        items.add(item);
        item.setStocktake(this);
    }

    /** Marks the stocktake completed and stamps {@link #completedAt}. */
    public void complete() {
        this.status = StocktakeStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public UUID getStocktakeId() {
        return stocktakeId;
    }

    public UUID getConductedBy() {
        return conductedBy;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public StocktakeStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public List<StocktakeItem> getItems() {
        return items;
    }
}
