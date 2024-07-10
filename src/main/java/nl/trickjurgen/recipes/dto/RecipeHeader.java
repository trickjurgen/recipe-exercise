package nl.trickjurgen.recipes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Short DTO for Recipe")
public class RecipeHeader {
    @Schema(example = "404", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    private Long id;

    @Schema(example = "Cottage Pie", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    @NotNull
    @Size(min = 1, max = 255)
    @NotEmpty
    @NotBlank
    private String name;

    @Schema(example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("isVegetarian")
    @NotNull
    private boolean isVegetarian;

    @Schema(example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("servings")
    @Positive
    @NotNull
    private int servings;

}
