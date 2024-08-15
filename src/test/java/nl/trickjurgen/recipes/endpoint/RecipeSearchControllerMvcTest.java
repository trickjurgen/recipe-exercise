package nl.trickjurgen.recipes.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import nl.trickjurgen.recipes.datamodel.Ingredient;
import nl.trickjurgen.recipes.datamodel.IngredientType;
import nl.trickjurgen.recipes.datamodel.Recipe;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.dto.RecipeHeaderDto;
import nl.trickjurgen.recipes.mapper.RecepAndIngrMapper;
import nl.trickjurgen.recipes.repo.RecipeRepo;
import nl.trickjurgen.recipes.utils.NameStringHelper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Tag("integration-test")
public class RecipeSearchControllerMvcTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceLoader resourceLoader;

    @MockBean
    private RecipeRepo recipeRepo;

    private final Logger logger = LoggerFactory.getLogger(RecipeSearchControllerMvcTest.class);

    private final static String ENDPOINT_SEARCH_PATH = "/recipesearch";

    @BeforeEach
    public void initialiseRestAssuredMockMvcWebApplicationContext() {
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
    }

    private List<RecipeDto> load10RecipesFromFile() throws IOException {
        String fileName = "batch1-10-recipes.json";
        File file = resourceLoader.getResource("classpath:recipes/" + fileName).getFile();
        byte[] bytes = Files.readAllBytes(file.toPath());
        RecipeDto[] values = objectMapper.readValue(bytes, RecipeDto[].class);
        logger.info("loaded test data from {}", fileName);
        return Arrays.asList(values);
    }

    private void prepareRepoMockReturnData() throws IOException {
        List<RecipeDto> dtoListFromFile = load10RecipesFromFile();
        assertThat(dtoListFromFile).hasSize(10);
        List<Recipe> recipes = Lists.newArrayList();
        long id = 101L;
        for (RecipeDto dto : dtoListFromFile) {
            recipes.add(convertDtoToRecipe(dto, id++));
        }
        Mockito.when(recipeRepo.findAll()).thenReturn(recipes);
    }

    private Recipe convertDtoToRecipe(RecipeDto rDto, Long id) {
        Recipe recipe = RecepAndIngrMapper.dtoToRecipeNoIngr(rDto);
        recipe.setId(id);
        Set<Ingredient> newIngredients = rDto.getIngredients().stream().map(iDto ->
                RecepAndIngrMapper.dtoToIngredientWithType(iDto,
                        IngredientType.builder().name(NameStringHelper.toTitleCase(iDto.getName())).build())
        ).collect(Collectors.toSet());
        recipe.setIngredients(newIngredients);
        return recipe;
    }

    @Test
    void testSearchCall() throws IOException {
        prepareRepoMockReturnData();

        RecipeHeaderDto[] recipeHeaderDtos = RestAssuredMockMvc.given()
                .get(ENDPOINT_SEARCH_PATH + "?isVegetarian=true")
                .then().assertThat()
                .statusCode(200).extract().response().as(RecipeHeaderDto[].class);

        assertThat(recipeHeaderDtos).hasSize(5);
        assertThat(recipeHeaderDtos).extracting("name").contains("Mushroom Risotto", "Lentil Soup", "Quinoa Salad", "Stuffed Peppers", "Pancakes");
    }

}
