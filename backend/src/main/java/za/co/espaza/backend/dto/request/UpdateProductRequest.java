package za.co.espaza.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class UpdateProductRequest {
    @Pattern(regexp = ".*\\S.*", message = "Product name cannot be blank") @Size(max = 200) private String name;
    @Size(max = 100) private String barcode;
    @DecimalMin(value = "0.01", message = "Selling price must be greater than zero") private BigDecimal sellingPrice;
    @DecimalMin(value = "0.0", message = "Cost price cannot be negative") private BigDecimal costPrice;
    @Min(value = 0, message = "Low-stock threshold cannot be negative") private Integer lowStockThreshold;
    @Size(max = 36) private String categoryId;
    @Size(max = 2000) private String description;
    public String getName(){return name;} public void setName(String value){name=value;}
    public String getBarcode(){return barcode;} public void setBarcode(String value){barcode=value;}
    public BigDecimal getSellingPrice(){return sellingPrice;} public void setSellingPrice(BigDecimal value){sellingPrice=value;}
    public BigDecimal getCostPrice(){return costPrice;} public void setCostPrice(BigDecimal value){costPrice=value;}
    public Integer getLowStockThreshold(){return lowStockThreshold;} public void setLowStockThreshold(Integer value){lowStockThreshold=value;}
    public String getCategoryId(){return categoryId;} public void setCategoryId(String value){categoryId=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
}
