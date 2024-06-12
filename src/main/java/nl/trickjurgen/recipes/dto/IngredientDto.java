package nl.trickjurgen.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description ="DTO for Ingredient")
public class IngredientDto {

    @Schema(example = "Optional id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("id")
    private Long id;

    @Schema(example = "Mashed potatoes", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    @NotNull
    @Size(min = 1, max = 100)
    @NotEmpty
    @NotBlank
    private String name;

    @Schema(example = "300 grams", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("volume")
    @NotNull
    @Size(min = 1, max = 100)
    @NotEmpty
    @NotBlank
    private String volume;

    @Schema(example = "Enough to cover dish", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("remark")
    @NotNull
    @Size(min = 1, max = 100)
    @NotEmpty
    @NotBlank
    private String remark;

}
