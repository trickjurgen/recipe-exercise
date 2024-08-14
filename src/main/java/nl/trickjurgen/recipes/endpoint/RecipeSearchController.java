package nl.trickjurgen.recipes.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.trickjurgen.recipes.dto.RecipeHeaderDto;
import nl.trickjurgen.recipes.service.RecipeService;
import nl.trickjurgen.recipes.utils.NameStringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recipesearch")
@Tag(name = "recipe search api", description = "Recipe Search Endpoint/Rest Api")
public class RecipeSearchController {

    private final RecipeService recipeService;

    @Autowired
    public RecipeSearchController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    // find recipes by specific properties
    @GetMapping() // /recipesearch?isVegetarian=true&minServings=1&maxServings=10&inclusions=carrot&exclusions=sausage,ice&instruction=stew
    @Operation(summary = "Returns list of info for relevant recipes in system", description = """
            Get list of matching recipe headers, based on criteria: \n
            isVegetarian: true, false \n
            minServings: minimum number \n
            maxServings: maximum number \n
            inclusions: (csv 'list' of) ingredients that need to be in it \n
            exclusions: (csv 'list' of) ingredients that are not allowed to be in it \n
            instruction: text that has to be in the instructions \n
            all parts are optional.
            """)
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Not Found")
    @ApiResponse(responseCode = "500", description = "Failure")
    public ResponseEntity<List<RecipeHeaderDto>> getMatchingRecipesForAspects(
            @Parameter(description = "vegetarian: true/false")
            @RequestParam(name = "isVegetarian", required = false) Boolean isVegetarian,
            @Parameter(description = "min. # servings; if limited")
            @RequestParam(name = "minServings", required = false) Integer minServings,
            @Parameter(description = "max. # servings; if limited")
            @RequestParam(name = "maxServings", required = false) Integer maxServings,
            @Parameter(description = "comma-separated list of ingredients to be included")
            @RequestParam(name = "includedIngredients", required = false) String inclusions,
            @Parameter(description = "comma-separated list of ingredients to be excluded")
            @RequestParam(name = "excludedIngredients", required = false) String exclusions,
            @Parameter(description = "text that has to be in instructions")
            @RequestParam(name = "instruction", required = false) String instruction
    ) {
        final List<String> includes = NameStringHelper.mapCsvToList(inclusions);
        final List<String> excludes = NameStringHelper.mapCsvToList(exclusions);
        final List<RecipeHeaderDto> recipeHeaders = recipeService.findRecipeHeadersWithGivenParams(isVegetarian, minServings, maxServings, includes, excludes, instruction);
        return ResponseEntity.ok(recipeHeaders);
    }

}
