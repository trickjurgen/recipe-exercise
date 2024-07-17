package nl.trickjurgen.recipes.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.dto.RecipeHeaderDto;
import nl.trickjurgen.recipes.exception.RecipeNotFoundException;
import nl.trickjurgen.recipes.mapper.RecepAndIngrMapper;
import nl.trickjurgen.recipes.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recipes")
@Tag(name = "recipe api", description = "Recipe Endpoint/Rest Api")
public class RecipeController {

    private final RecipeService recipeService;

    @Autowired
    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    // CRUD - Read All
    @GetMapping()
    @Operation(summary = "Returns list of info for all recipes in system", description = "Get list of all recipe headers.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Not Found")
    @ApiResponse(responseCode = "500", description = "Failure")
    public ResponseEntity<List<RecipeHeaderDto>> getAllRecipeNames() {
        final List<RecipeDto> allRecipes = recipeService.findAllRecipes();
        if (allRecipes.isEmpty()) throw new RecipeNotFoundException("DB empty");
        return ResponseEntity.ok(allRecipes.stream().map(RecepAndIngrMapper::RecipeDtoToHeader).toList());
    }

    // CRUD - Read 1
    @GetMapping("/{recipeId}")
    @Operation(summary = "Find a recipes by stored id", description = "Get recipe by identifier.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Not Found")
    public ResponseEntity<RecipeDto> getRecipeById(
            @Parameter(description = "ID of the recipe to get", required = true) @PathVariable Long recipeId) {
        return ResponseEntity.ok(recipeService.findRecipeById(recipeId));
    }

    // CRUD - Delete
    @DeleteMapping("/{recipeId}")
    @Operation(summary = "Delete a recipe", description = "Deletes the specified recipe by ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Recipe successfully deleted"),
                    @ApiResponse(responseCode = "404", description = "Recipe not found"),
                    @ApiResponse(responseCode = "500", description = "delete failed internally")
            })
    public ResponseEntity<Void> deleteRecipe(
            @Parameter(description = "ID of the recipe to delete", required = true) @PathVariable Long recipeId) {
        final boolean deleteSuccess = recipeService.deleteRecipe(recipeId);
        if (!deleteSuccess) return ResponseEntity.internalServerError().build();
        return ResponseEntity.noContent().build();
    }

    // CRUD - Create
    @PostMapping
    @Operation(summary = "Create a new recipe", description = "Add a new recipe to the database including ingredients.")
    @ApiResponse(responseCode = "201", description = "Recipe created successfully")
    @ApiResponse(responseCode = "400", description = "Recipe has an ID, it should not have one yet")
    @ApiResponse(responseCode = "409", description = "Recipe with name already exits")
    public ResponseEntity<RecipeDto> createRecipe(
            @Parameter(description = "Recipe information", required = true) @RequestBody RecipeDto recipeDto) {
        return new ResponseEntity<>(recipeService.saveNewRecipe(recipeDto), HttpStatus.CREATED);
    }

    // CRUD - Update
    @PutMapping("/{recipeId}") // do a full object update (PUT) of a DTO and no partial update (PATCH)
    @Operation(summary = "Update a  recipe", description = "Change/update a recipe in the database with changed properties and/or ingredients.")
    @ApiResponse(responseCode = "200", description = "Recipe updated successfully")
    @ApiResponse(responseCode = "400", description = "(Part of) the update failed")
    @ApiResponse(responseCode = "404", description = "Recipe not found")
    public ResponseEntity<RecipeDto> updateRecipe(
            @Parameter(description = "ID of the recipe to delete", required = true) @PathVariable Long recipeId,
            @Parameter(description = "Updated recipe information", required = true) @RequestBody RecipeDto recipeDto) {
        return ResponseEntity.ok(recipeService.updateRecipe(recipeId, recipeDto));
    }

}
