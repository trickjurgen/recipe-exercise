package nl.trickjurgen.recipes.mapper;

import nl.trickjurgen.recipes.datamodel.Ingredient;
import nl.trickjurgen.recipes.datamodel.IngredientType;
import nl.trickjurgen.recipes.datamodel.Recipe;
import nl.trickjurgen.recipes.dto.IngredientDto;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.utils.NameStringHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

public class RecepAndIngrMapper {

    public static Recipe dtoToRecipeNoIngr(final RecipeDto recipeDto) {
        final String recipeName = NameStringHelper.toTitleCase(recipeDto.getName());
        return Recipe.builder()
                .name(recipeName)
                .instructions(recipeDto.getInstructions())
                .isVegetarian(recipeDto.isVegetarian())
                .servings(recipeDto.getServings())
                .ingredients(new HashSet<>())
                .build();
    }

    public static Ingredient dtoToIngredientWithType(final IngredientDto ingredientDto, final IngredientType ingredientType) {
        return Ingredient.builder()
                .ingredientType(ingredientType)
                .volume(ingredientDto.getVolume())
                .remark(ingredientDto.getRemark())
                .build();
    }

    public static RecipeDto recipeToDto(final Recipe recipe) {
        return RecipeDto.builder()
                .id(recipe.getId())
                .name(recipe.getName())
                .isVegetarian(recipe.isVegetarian())
                .servings(recipe.getServings())
                .instructions(recipe.getInstructions())
                .ingredients(recipe.getIngredients() == null ? Collections.emptySet() : recipe.getIngredients().stream()
                        .map(RecepAndIngrMapper::ingredientToDto).collect(Collectors.toSet()))
                .build();
    }

    public static IngredientDto ingredientToDto(final Ingredient ingredient) {
        return IngredientDto.builder()
                .id(ingredient.getId())
                .name(ingredient.getIngredientType().getName())
                .volume(ingredient.getVolume())
                .remark(ingredient.getRemark())
                .build();
    }

}
