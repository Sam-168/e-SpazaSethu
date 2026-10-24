package za.co.espaza.backend.dto.request;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public class UpdateCategoryRequest {
    @Pattern(regexp=".*\\S.*", message="Category name cannot be blank") @Size(max=100) private String name;
    @Size(max=500) private String description;
    public String getName(){return name;} public void setName(String value){name=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
}
