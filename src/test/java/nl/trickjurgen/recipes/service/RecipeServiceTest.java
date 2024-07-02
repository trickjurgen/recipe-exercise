package nl.trickjurgen.recipes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.trickjurgen.recipes.datamodel.Ingredient;
import nl.trickjurgen.recipes.datamodel.IngredientType;
import nl.trickjurgen.recipes.datamodel.Recipe;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.exception.RecipeNotFoundException;
import nl.trickjurgen.recipes.repo.IngredientRepo;
import nl.trickjurgen.recipes.repo.IngredientTypeRepo;
import nl.trickjurgen.recipes.repo.RecipeRepo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    static Logger log = LoggerFactory.getLogger(RecipeService.class);

    @Mock
    private RecipeRepo recipeRepo;

    @Mock
    private IngredientRepo ingredientRepo;

    @Mock
    private IngredientTypeRepo ingredientTypeRepo;

    // class under test
    public RecipeService recipeService;

    private static List<Recipe> baseRecipeData;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @BeforeAll
    static void setup() {
        long i = 1L;
        baseRecipeData = new ArrayList<>();
        baseRecipeData.add(Recipe.builder().id(i++).name("Chicken Curry")
                .isVegetarian(false).servings(4)
                .instructions("Boil rice in water with stock cube. Cook chicken, add spices and coconut milk. Simmer for 30 minutes.")
                .ingredients(Set.of(
                        Ingredient.builder().id(i++).ingredientType(IngredientType.builder().id(i++).name("Chicken Breast").build())
                                .volume("1 pound").remark("Can subst with Turkey").build(),
                        Ingredient.builder().id(i++).ingredientType(IngredientType.builder().id(i++).name("Coconut Milk").build())
                                .volume("1 can").build(),
                        Ingredient.builder().id(i++).ingredientType(IngredientType.builder().id(i++).name("Curry Powder").build())
                                .volume("2 tsp.").build(),
                        Ingredient.builder().id(i++).ingredientType(IngredientType.builder().id(i++).name("Rice").build())
                                .volume("300 grams").remark("Basmati?").build(),
                        Ingredient.builder().id(i++).ingredientType(IngredientType.builder().id(i++).name("Stock Cube").build())
                                .volume("1").build()
                ))
                .build());
        log.info("basic setup done");
    }

    @BeforeEach
    void init() {
        recipeService = new RecipeService(recipeRepo, ingredientTypeRepo, ingredientRepo);
    }

    @Test
    void findAllRecipes() {
        when(recipeRepo.findAll()).thenReturn(baseRecipeData);

        List<RecipeDto> allRecipes = recipeService.findAllRecipes();

        assertThat(allRecipes).hasSize(1);
    }

    @Test
    void findRecipeById() {
        when(recipeRepo.existsById(anyLong())).thenReturn(false);
        when(recipeRepo.existsById(100L)).thenReturn(true);
        when(recipeRepo.getReferenceById(100L)).thenReturn(baseRecipeData.getFirst());

        assertThatThrownBy(() -> recipeService.findRecipeById(101L)).isInstanceOf(RecipeNotFoundException.class);
        assertThatThrownBy(() -> recipeService.findRecipeById(-1L)).isInstanceOf(RecipeNotFoundException.class);
        assertThatThrownBy(() -> recipeService.findRecipeById(null)).isInstanceOf(RecipeNotFoundException.class);

        RecipeDto recipeById = recipeService.findRecipeById(100L);
        assertThat(recipeById).isNotNull();
        assertThat(recipeById.getName()).contains("Curry");
    }

    private RecipeDto readDtoFromFile(final String fileName) {
        RecipeDto recipeDto;
        try {
            byte[] bytes = getBytesFromJsonFile(fileName);
            recipeDto = objectMapper.readValue(bytes, RecipeDto.class);
            assertThat(recipeDto).isNotNull();
        } catch (Exception e) {
            fail("file {} load issue: {}", fileName, e.getMessage());
            throw new AssertionError(e);
        }
        return recipeDto;
    }

    private byte[] getBytesFromJsonFile(String fileName) throws IOException {
        final String filePath = "recipes/";
        String loadPath = Objects.requireNonNull(getClass().getClassLoader().getResource(filePath + fileName)).getPath();
        File file = new File(loadPath);
        log.info("test-data file exists? {} : {}", loadPath, file.exists());
        return Files.readAllBytes(file.toPath());
    }

    @Test
    void saveNewRecipe() {
        RecipeDto dto = readDtoFromFile("r2-spagetti-bol.json");
        when(recipeRepo.findByName(anyString())).thenReturn(Optional.empty());
        when(recipeRepo.save(any())).then(i -> {
            Recipe retVal = i.getArgument(0, Recipe.class);
            retVal.setId(100L); // part of the saving process is DB assigning an id
            return retVal;
        });
        when(ingredientRepo.save(any())).then(returnsFirstArg());
        when(ingredientTypeRepo.findByName(anyString())).thenReturn(Optional.empty());
        when(ingredientTypeRepo.save(any())).then(returnsFirstArg());

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getName()).contains("Bolognese");
        assertThat(dto.getIngredients()).hasSize(6);

        RecipeDto savedRecipe = recipeService.saveNewRecipe((dto));

        assertThat(savedRecipe.getName()).contains("Bolognese");
        assertThat(savedRecipe.getId()).isNotNull();
        assertThat(savedRecipe.getIngredients()).hasSize(6);
    }

    @Test
    void updateRecipe() throws IOException {
        RecipeDto recipeDto1 = readDtoFromFile("r3-veg-stirfry.json");

        // TODO this !
    }

    @Test
    void deleteRecipe() {
        when(recipeRepo.existsById(anyLong())).thenReturn(false);
        when(recipeRepo.existsById(404L)).thenReturn(true);
        when(recipeRepo.getReferenceById(404L)).thenReturn(baseRecipeData.getFirst());
        // mock will take care of call to recipeRepo.delete(recipe)

        assertThat(recipeService.deleteRecipe(404L)).isEqualTo(true);
        assertThatThrownBy(() -> recipeService.deleteRecipe(405L)).isInstanceOf(RecipeNotFoundException.class);
    }

    private List<RecipeDto> readManyDtoFromFile(final String fileName) {
        List<RecipeDto> retVal = new ArrayList<>();
        try {
            byte[] bytes = getBytesFromJsonFile(fileName);
            RecipeDto[] recipeDtos = objectMapper.readValue(bytes, RecipeDto[].class);
            assertThat(recipeDtos).isNotNull();
            retVal.addAll(List.of(recipeDtos));
        } catch (Exception e) {
            fail("file {} load issue: {}", fileName, e.getMessage());
            throw new AssertionError(e);
        }
        return retVal;
    }

    @Test
    void findRecipesWithSpecificDetails() {
        // TODO this !
    }

    @Test
    void flatListIngredients() {
        // TODO this !
    }
}