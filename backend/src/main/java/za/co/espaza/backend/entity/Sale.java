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
import za.co.espaza.backend.enums.PaymentMethod;
import za.co.espaza.backend.enums.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Sale header — one row per transaction (table {@code sale}).
 *
 * <p>Backend Issue 3. The owning {@code user} is currently held as the
 * {@code userId} foreign-key column. Once Backend Issue 1 provides the
 * {@code User} entity this field must be replaced by
 * {@code @ManyToOne @JoinColumn(name = "userId")} as required by the issue.
 * No placeholder {@code User} type is introduced here.</p>
 */
@Entity
@Table(name = "sale")
public class Sale {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "saleId", length = 36, nullable = false, updatable = false)
    private UUID saleId;

    // TODO (Backend Issue 1): replace with @ManyToOne @JoinColumn(name = "userId")
    // once the User entity exists. Stored as the raw FK column until then.
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "userId", length = 36, nullable = false)
    private UUID userId;

    @Column(name = "saleDateTime", nullable = false)
    private LocalDateTime saleDateTime;

    @Column(name = "totalAmount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "paymentMethod", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SaleStatus status = SaleStatus.COMPLETED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SaleItem> items = new ArrayList<>();

    /** Required by JPA. */
    protected Sale() {
    }

    public Sale(UUID userId, PaymentMethod paymentMethod, SaleStatus status, String notes) {
        this.saleId = UUID.randomUUID();
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.notes = notes;
        this.saleDateTime = LocalDateTime.now();
    }

    @PrePersist
    void onPrePersist() {
        if (saleId == null) {
            saleId = UUID.randomUUID();
        }
        if (saleDateTime == null) {
            saleDateTime = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SaleStatus.COMPLETED;
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }

    /** Adds a line item and wires the back-reference so cascade persists it. */
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }

    /** Recomputes {@link #totalAmount} from the current line items. */
    public BigDecimal calculateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleItem item : items) {
            total = total.add(item.getSubtotal());
        }
        this.totalAmount = total;
        return total;
    }

    public void complete() {
        if (status == SaleStatus.CANCELLED) {
            throw new IllegalStateException("A cancelled sale cannot be completed");
        }
        this.status = SaleStatus.COMPLETED;
    }

    public void cancel() {
        if (status == SaleStatus.CANCELLED) {
            throw new IllegalStateException("A sale that is already cancelled cannot be cancelled again");
        }
        this.status = SaleStatus.CANCELLED;
    }

    public UUID getSaleId() {
        return saleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDateTime getSaleDateTime() {
        return saleDateTime;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public SaleStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<SaleItem> getItems() {
        return items;
    }
}
