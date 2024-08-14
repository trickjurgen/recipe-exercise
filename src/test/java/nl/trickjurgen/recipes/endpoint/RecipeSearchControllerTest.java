package nl.trickjurgen.recipes.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import nl.trickjurgen.recipes.RecipeAppApplication;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.dto.RecipeHeaderDto;
import nl.trickjurgen.recipes.service.RecipeService;
import nl.trickjurgen.recipes.utils.NameStringHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = {RecipeAppApplication.class})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class RecipeSearchControllerTest {

    public static final String URL_PARAM_SEPARATOR = "?";
    private final Logger logger = LoggerFactory.getLogger(RecipeSearchControllerTest.class);

    // FIXME this test could also use test-containers instead of H2

    @SuppressWarnings("unused") // tell intellij not to complain about injected value
    @LocalServerPort
    private int port;

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final RecipeSearchController recipeSearchController;
    private final RecipeService recipeService;

    final static String ENDPOINT_SEARCH_PATH = "/recipesearch";

    @Autowired
    public RecipeSearchControllerTest(RecipeService recipeService, ObjectMapper objectMapper, ResourceLoader resourceLoader,
                                      RecipeSearchController recipeSearchController) {
        this.recipeService = recipeService;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.recipeSearchController = recipeSearchController;
    }

    @BeforeEach
    void setUp() throws IOException {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        loadTestData();
    }

    private List<RecipeDto> load10RecipesFromFile() throws IOException {
        String fileName = "batch1-10-recipes.json";
        File file = resourceLoader.getResource("classpath:recipes/" + fileName).getFile();
        byte[] bytes = Files.readAllBytes(file.toPath());
        RecipeDto[] values = objectMapper.readValue(bytes, RecipeDto[].class);
        logger.info("loaded test data from {}", fileName);
        return Arrays.asList(values);
    }

    private void loadTestData() throws IOException {
        List<RecipeDto> dtoListFromFile = load10RecipesFromFile();
        assertThat(dtoListFromFile).hasSize(10);

        for (RecipeDto dto : dtoListFromFile) {
            logger.info("loading recipe {}", dto.getName());
            RecipeDto saved = recipeService.saveNewRecipe(dto);
            assertThat(saved.getId()).isNotNull();
        }
    }

    private String assembleSearchParams(final Boolean vegetarian, final Integer minServings, final Integer maxServings,
                                        final String includesCsv, final String excludesCsv, final String partOfInstructions) {
        // ?isVegetarian=true&minServings=1&maxServings=10&inclusions=carrot&exclusions=sausage,ice&instruction=stew
        String paramString = "";
        paramString = addSearchParam(paramString, "isVegetarian", vegetarian);
        paramString = addSearchParam(paramString, "minServings", minServings);
        paramString = addSearchParam(paramString, "maxServings", maxServings);
        paramString = addSearchParam(paramString, "inclusions", includesCsv);
        paramString = addSearchParam(paramString, "exclusions", excludesCsv);
        paramString = addSearchParam(paramString, "instruction", partOfInstructions);
        if (paramString.isBlank()) return ""; // avoid dangling question mark
        return URL_PARAM_SEPARATOR + paramString;
    }

    private String addSearchParam(String paramstring, String prefix, Object value) {
        if (null == value) return paramstring;
        final String coupler = paramstring.isBlank() ? "" : "&";
        return paramstring + coupler + prefix + "=" + value;
    }

    @Test
    @DisplayName("search result for no search filters")
    @Transactional
    void getMatchingRecipesForAspects_no_props() {
        // check that database is filled
        assertThat(recipeService.findAllRecipes()).hasSize(10);

        Boolean vegetarian = null;
        Integer minServings = null;
        Integer maxServings = null;
        String includesCsv = null;
        String excludesCsv = null;
        String partOfInstructions = null;
        int expectedResultSize = 10;

        // search via service
        final List<String> includes = NameStringHelper.mapCsvToList(includesCsv);
        final List<String> excludes = NameStringHelper.mapCsvToList(excludesCsv);
        List<RecipeHeaderDto> headersFromService = recipeService.findRecipeHeadersWithGivenParams(vegetarian, minServings, maxServings, includes, excludes, partOfInstructions);
        assertThat(headersFromService).hasSize(expectedResultSize);

        // search via controller
        ResponseEntity<List<RecipeHeaderDto>> matchingRecipesForAspects = recipeSearchController.getMatchingRecipesForAspects(vegetarian, minServings, maxServings, includesCsv, excludesCsv, partOfInstructions);
        List<RecipeHeaderDto> recipesFromController = matchingRecipesForAspects.getBody();

        assertThat(recipesFromController).hasSize(expectedResultSize);

        // request via api must be successful / 200
        String searchValues = assembleSearchParams(vegetarian, minServings, maxServings, includesCsv, excludesCsv, partOfInstructions);
        RestAssured.given().when()
                .get(ENDPOINT_SEARCH_PATH + searchValues)
                .then().assertThat()
                .statusCode(200);
    }

    @Test
    @DisplayName("search result for vegetarian")
    @Transactional
    void getMatchingRecipesForAspects_veggie() {
        // check that database is filled
        assertThat(recipeService.findAllRecipes()).hasSize(10);

        Boolean vegetarian = true;
        Integer minServings = null;
        Integer maxServings = null;
        String includesCsv = null;
        String excludesCsv = null;
        String partOfInstructions = null;
        int expectedResultSize = 5;

        ResponseEntity<List<RecipeHeaderDto>> matchingRecipesForAspects = recipeSearchController.getMatchingRecipesForAspects(vegetarian, minServings, maxServings, includesCsv, excludesCsv, partOfInstructions);
        List<RecipeHeaderDto> recipesFromController = matchingRecipesForAspects.getBody();

        assertThat(recipesFromController).hasSize(expectedResultSize);
        assertThat(recipesFromController).extracting("name").contains("Mushroom Risotto", "Lentil Soup", "Quinoa Salad", "Stuffed Peppers", "Pancakes");
    }

    @Test
    @DisplayName("search result for servings")
    @Transactional
    void getMatchingRecipesForAspects_servings() {
        // check that database is filled
        assertThat(recipeService.findAllRecipes()).hasSize(10);

        Boolean vegetarian = null;
        Integer minServings = 2;
        Integer maxServings = 5;
        String includesCsv = null;
        String excludesCsv = null;
        String partOfInstructions = null;
        int expectedResultSize = 9;

        ResponseEntity<List<RecipeHeaderDto>> matchingRecipesForAspects = recipeSearchController.getMatchingRecipesForAspects(vegetarian, minServings, maxServings, includesCsv, excludesCsv, partOfInstructions);
        List<RecipeHeaderDto> recipesFromController = matchingRecipesForAspects.getBody();

        assertThat(recipesFromController).hasSize(expectedResultSize);
        assertThat(recipesFromController).extracting("name").contains("Mushroom Risotto", "Beef Stroganoff", "Lentil Soup",
                "Shrimp Scampi", "Quinoa Salad", "Chicken Alfredo", "Stuffed Peppers", "Salmon Teriyaki", "Chili Con Carne");
        assertThat(recipesFromController).extracting("name").doesNotContain("Pancakes");
    }

    @Test
    @DisplayName("search result for includes, excludes, text")
    @Transactional
    void getMatchingRecipesForAspects_combo() {
        // check that database is filled
        assertThat(recipeService.findAllRecipes()).hasSize(10);

        Boolean vegetarian = null;
        Integer minServings = null;
        Integer maxServings = null;
        String includesCsv = "rice"; // Mushroom Risotto, Stuffed Peppers, Salmon Teriyaki
        String excludesCsv = "peppers,olive"; // Chili Con Carne, Stuffed Peppers - Quinoa Salad, Shrimp Scampi, Lentil Soup
        String partOfInstructions = "mix"; // Stuffed Peppers, Salmon Teriyaki, Pancakes
        int expectedResultSize = 1; // Salmon Teriyaki

        ResponseEntity<List<RecipeHeaderDto>> matchingRecipesForAspects = recipeSearchController.getMatchingRecipesForAspects(vegetarian, minServings, maxServings, includesCsv, excludesCsv, partOfInstructions);
        List<RecipeHeaderDto> recipesFromController = matchingRecipesForAspects.getBody();

        assertThat(recipesFromController).hasSize(expectedResultSize);
        assertThat(recipesFromController).extracting("name").contains("Salmon Teriyaki");
    }

}