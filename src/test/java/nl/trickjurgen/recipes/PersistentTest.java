package nl.trickjurgen.recipes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.trickjurgen.recipes.datamodel.Ingredient;
import nl.trickjurgen.recipes.datamodel.IngredientType;
import nl.trickjurgen.recipes.datamodel.Recipe;
import nl.trickjurgen.recipes.dto.IngredientDto;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.mapper.RecepAndIngrMapper;
import nl.trickjurgen.recipes.repo.IngredientRepo;
import nl.trickjurgen.recipes.repo.IngredientTypeRepo;
import nl.trickjurgen.recipes.repo.RecipeRepo;
import nl.trickjurgen.recipes.utils.NameStringHelper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
public class PersistentTest {

    private final RecipeRepo recipeRepo;
    private final IngredientTypeRepo ingredientTypeRepo;
    private final IngredientRepo ingredientRepo;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    Logger logger = LoggerFactory.getLogger(PersistentTest.class);

    @Autowired
    public PersistentTest(RecipeRepo recipeRepo, ResourceLoader resourceLoader, ObjectMapper objectMapper,
                          IngredientRepo ingredientRepo, IngredientTypeRepo ingredientTypeRepo) {
        this.recipeRepo = recipeRepo;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.ingredientRepo = ingredientRepo;
        this.ingredientTypeRepo = ingredientTypeRepo;
    }

    @Test
    void attemptLoadRecipeFromFile () {
        final String fileName = "r1-chicken-curry.json";
        final String path = "classpath:recipes" + File.separator + fileName;

        List<Recipe> recipeList = recipeRepo.findAll();
        assertThat(recipeList).isEmpty();
        assertThat(recipeRepo.count()).isEqualTo(0);

        RecipeDto recipeDto;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(resourceLoader.getResource(path).getURI()));
            recipeDto = objectMapper.readValue(bytes, new TypeReference<>() {});
            assertThat(recipeDto).isNotNull();
            logger.info("recipe read from {}", path);
        } catch (Exception e) {
            fail("file load issue: " + e.getMessage());
            throw new AssertionError(e);
        }
        createRecipe(recipeDto);

        assertThat(recipeRepo.count()).isEqualTo(1);
    }

    // this functionality will be in service later I guess
    private void createRecipe(RecipeDto recipeDto){
        Recipe newRecipe = RecepAndIngrMapper.dtoToRecipeNoIngr(recipeDto);
        if (recipeRepo.findByName(newRecipe.getName()).isPresent()){
            logger.warn("already exists! {}", newRecipe.getName());
            return;
        }
        Set<IngredientDto> ingredients = recipeDto.getIngredients();
        for (IngredientDto ing: ingredients){
            String correctedName = NameStringHelper.toTitleCase(ing.getName());
            Optional<IngredientType> typeByName = ingredientTypeRepo.findByName(correctedName);
            IngredientType ingType = typeByName.orElseGet(() -> ingredientTypeRepo.save(IngredientType.builder().name(correctedName).build()));
            Ingredient savedIngredient = ingredientRepo.save(RecepAndIngrMapper.dtoToIngredientWithType(ing, ingType));
            newRecipe.getIngredients().add(savedIngredient);
        }
        recipeRepo.save(newRecipe);
    }

}
