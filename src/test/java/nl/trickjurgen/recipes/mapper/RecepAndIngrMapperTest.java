package nl.trickjurgen.recipes.mapper;

import nl.trickjurgen.recipes.datamodel.Ingredient;
import nl.trickjurgen.recipes.datamodel.IngredientType;
import nl.trickjurgen.recipes.datamodel.Recipe;
import nl.trickjurgen.recipes.dto.IngredientDto;
import nl.trickjurgen.recipes.dto.RecipeDto;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RecepAndIngrMapperTest {

    @Test
    void entityToDto() {
        Set<Ingredient> ingredientSet = Set.of(
                Ingredient.builder().id(1001L).ingredientType(IngredientType.builder().id(1L)
                        .name("Butter").build()).volume("1 tsp").build(),
                Ingredient.builder().id(1002L).ingredientType(IngredientType.builder().id(2L)
                        .name("Egg").build()).volume("2").remark("fresh").build()
        );
        Recipe recipe = Recipe.builder().id(100L).name("Fried Egg").isVegetarian(false)
                .servings(1).instructions("Add to pan and heat").ingredients(ingredientSet).build();

        RecipeDto recipeDto = RecepAndIngrMapper.recipeToDto(recipe);

        assertThat(recipeDto).isNotNull();
        assertThat(recipeDto.getIngredients()).hasSize(2);
        assertThat(recipeDto.getName()).contains("Egg");
    }

    @Test
    void dtoToEntity() {
        IngredientDto ing1 = IngredientDto.builder().name("Butter").volume("1 tsp").remark("cold").build();
        IngredientDto ing2 = IngredientDto.builder().name("Egg").volume("2").build();
        RecipeDto recipeDto = RecipeDto.builder().name("Fried Egg").isVegetarian(false).servings(1)
                .instructions("Add to pan and heat").ingredients(Set.of(ing1, ing2)).build();

        Recipe recipe = RecepAndIngrMapper.dtoToRecipeNoIngr(recipeDto);
        Set<Ingredient> ingredients = recipeDto.getIngredients().stream()
                .map(id -> RecepAndIngrMapper.dtoToIngredientWithType(id, IngredientType.builder().name(id.getName()).build()))
                .collect(Collectors.toSet());
        recipe.setIngredients(ingredients);

        assertThat(recipe).isNotNull();
        assertThat(recipe.getName()).contains("Egg");
        assertThat(recipe.getIngredients()).hasSize(2);
        assertThat(recipe.getIngredients()).extracting(i -> i.getIngredientType().getName()).contains("Egg", "Butter");
    }

}