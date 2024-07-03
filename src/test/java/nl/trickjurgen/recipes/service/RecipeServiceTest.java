package nl.trickjurgen.recipes.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.trickjurgen.recipes.datamodel.Ingredient;
import nl.trickjurgen.recipes.datamodel.IngredientType;
import nl.trickjurgen.recipes.datamodel.Recipe;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.exception.RecipeNotFoundException;
import nl.trickjurgen.recipes.mapper.RecepAndIngrMapper;
import nl.trickjurgen.recipes.repo.IngredientRepo;
import nl.trickjurgen.recipes.repo.IngredientTypeRepo;
import nl.trickjurgen.recipes.repo.RecipeRepo;
import nl.trickjurgen.recipes.utils.NameStringHelper;
import org.assertj.core.util.Lists;
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
import java.util.stream.Collectors;

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
        long i = 11L;
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

    final String storedRecipe = """
            {"id": 101, "name": "Vegetable Stir Fry", "vegetarian": true, "servings": 4,
                        "instructions": "Heat oil in a large pan. Add chopped vegetables and stir-fry for 5-7 minutes. Add soy sauce and cook for another 2 minutes.",
                        "ingredients": [
                    {"id": 551, "ingredientType": {"id":1010, "name": "Broccoli"}, "volume": "1 head", "remark": "cut into florets"},
                    {"id": 552, "ingredientType": {"id":1011, "name": "Soy Sauce"}, "volume": "3 tablespoons"},
                    {"id": 553, "ingredientType": {"id":1012, "name": "Olive Oil"}, "volume": "2 tablespoons"}
              ]}
            """;

    final String updateRecipeDto = """
            {"id": 101, "name": "Vegetable Stir Fry", "isVegetarian": true, "servings": 3,
                        "instructions": "Heat oil in a large pan. Add chopped vegetables and stir-fry for 5-7 minutes. Add soy sauce and cook for another 2 minutes. Serve with rice",
                        "ingredients": [
                    {"name": "Broccoli", "volume": "1 head", "remark": "cut into florets"},
                    {"name": "Carrots", "volume": "1", "remark": "sliced"},
                    {"name": "Bell Pepper", "volume": "1", "remark": "sliced"},
                    {"name": "Dark Soy Sauce", "volume": "3 tablespoons"},
                    {"name": "Olive Oil", "volume": "2 tablespoons"},
                    {"name": "Rice", "volume": "300 grams", "remark": "steamed"}
              ]}
            """;

    private Optional<IngredientType> getIngTypeFromTestData(Recipe dbRecipe, String name) {
        return dbRecipe.getIngredients().stream()
                .map(Ingredient::getIngredientType)
                .filter(type -> type.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Test
    void updateRecipe() throws JsonProcessingException {
        // setup data
        final Long dbId = 101L;
        Recipe dbRecipe = objectMapper.readValue(storedRecipe, Recipe.class);
        assertThat(dbRecipe).isNotNull();
        RecipeDto recipeDto = objectMapper.readValue(updateRecipeDto, RecipeDto.class);
        assertThat(recipeDto).isNotNull();
        // feed the dummies (mocks)
        when(recipeRepo.save(any())).then(returnsFirstArg());
        when(recipeRepo.existsById(dbId)).thenReturn(true);
        when(recipeRepo.getReferenceById(dbId)).thenReturn(dbRecipe);
        when(ingredientRepo.save(any())).then(returnsFirstArg());
        // mock will take care of call to ingredientRepo.delete()
        when(ingredientTypeRepo.findByName(anyString()))
                .thenAnswer(i -> {
                    String name = i.getArgument(0, String.class);
                    return getIngTypeFromTestData(dbRecipe, name);
                });
        when(ingredientTypeRepo.save(any())).then(returnsFirstArg());

        RecipeDto updatedRecipe = recipeService.updateRecipe(recipeDto);

        assertThat(updatedRecipe.getIngredients()).hasSize(6); // 2 old, 1 deleted, 4 new
        assertThat(updatedRecipe.getServings()).isEqualTo(3);
        assertThat(updatedRecipe.getInstructions()).contains("Serve with rice");
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

    private List<RecipeDto> readManyDtoFromFile() {
        try {
            RecipeDto[] recipeDtos = objectMapper.readValue(getBytesFromJsonFile("batch1-10-recipes.json"), RecipeDto[].class);
            assertThat(recipeDtos).isNotNull();
            return List.of(recipeDtos);
        } catch (Exception e) {
            fail("file {} load issue: {}", "batch1-10-recipes.json", e.getMessage());
            throw new AssertionError(e);
        }
    }

    private Recipe convertDtoToRecipe(RecipeDto rDto) {
        Recipe recipe = RecepAndIngrMapper.dtoToRecipeNoIngr(rDto);
        Set<Ingredient> newIngredients = rDto.getIngredients().stream().map(iDto ->
                RecepAndIngrMapper.dtoToIngredientWithType(iDto,
                        IngredientType.builder().name(NameStringHelper.toTitleCase(iDto.getName())).build())
        ).collect(Collectors.toSet());
        recipe.setIngredients(newIngredients);
        return recipe;
    }

    @Test
    void findRecipesWithSpecificDetails() {
        List<RecipeDto> recipeDtoList = readManyDtoFromFile();
        List<Recipe> readRecipesFromFile = recipeDtoList.stream().map(this::convertDtoToRecipe).toList();

        assertThat(readRecipesFromFile).hasSize(10);

        when(recipeRepo.findAll()).thenReturn(readRecipesFromFile);

        Boolean veggie = true;
        Integer minServ = 0;
        Integer maxServ = 8;
        List<String> incl = Lists.newArrayList();
        List<String> excl = Lists.newArrayList();
        List<RecipeDto> foundItems = recipeService.findRecipesWithSpecificDetails(veggie, minServ, maxServ, incl, excl);

        assertThat(foundItems).hasSize(5);
        assertThat(foundItems).extracting("name")
                .containsOnly("Pancakes", "Stuffed Peppers", "Quinoa Salad", "Lentil Soup", "Mushroom Risotto");

        minServ = null;
        maxServ = 6;
        foundItems = recipeService.findRecipesWithSpecificDetails(veggie, minServ, maxServ, incl, excl);
        assertThat(foundItems).hasSize(4);
        assertThat(foundItems).extracting("name")
                .containsOnly("Stuffed Peppers", "Quinoa Salad", "Lentil Soup", "Mushroom Risotto");

        veggie = null;
        maxServ = null;
        incl = List.of("Onion"); //  onion in recipes[Mushroom Risotto, Beef Stroganoff, Lentil Soup, Chili Con Carne]
        excl = List.of("Bell Peppers"); // bell peppers in recipes[Chili Con Carne, Stuffed Peppers]
        foundItems = recipeService.findRecipesWithSpecificDetails(veggie, minServ, maxServ, incl, excl);
        assertThat(foundItems).hasSize(3);
        assertThat(foundItems).extracting("name")
                        .containsOnly("Mushroom Risotto", "Beef Stroganoff", "Lentil Soup");

        incl = List.of("Onion", "Arborio Rice");
        excl = Lists.newArrayList();
        foundItems = recipeService.findRecipesWithSpecificDetails(veggie, minServ, maxServ, incl, excl);
        assertThat(foundItems).hasSize(1);
        assertThat(foundItems).extracting("name").containsOnly("Mushroom Risotto");

        excl = List.of("mushrooms");
        foundItems = recipeService.findRecipesWithSpecificDetails(veggie, minServ, maxServ, incl, excl);
        assertThat(foundItems).isEmpty();
    }

    @Test
    void flatListIngredients() {
        Recipe noIngredients = Recipe.builder().name("bad example").servings(1).isVegetarian(false).instructions("no intel found").build();
        Recipe recipeBeef = convertDtoToRecipe(readDtoFromFile("r4-beef-tacos.json"));

        assertThat(recipeService.flatListIngredients(noIngredients)).isEmpty();

        List<String> flatted = recipeService.flatListIngredients(recipeBeef);
        assertThat(flatted).isNotEmpty();
        assertThat(flatted).contains("Lettuce");
        assertThat(flatted).contains("Cheddar Cheese");
        assertThat(flatted).contains("Water");
        assertThat(flatted).contains("Taco Shells");
    }
}