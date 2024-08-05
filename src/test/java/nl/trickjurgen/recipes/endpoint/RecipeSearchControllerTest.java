package nl.trickjurgen.recipes.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.dto.RecipeHeaderDto;
import nl.trickjurgen.recipes.service.RecipeService;
import nl.trickjurgen.recipes.utils.NameStringHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestConfiguration
class RecipeSearchControllerTest {

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
    void setUp() {
        RestAssured.port = port;
    }

    private List<RecipeDto> load10RecipesFromFile() throws IOException {
        String fileName = "batch1-10-recipes.json";
        File file = resourceLoader.getResource("classpath:recipes/" + fileName).getFile();
        byte[] bytes = Files.readAllBytes(file.toPath());
        RecipeDto[] values = objectMapper.readValue(bytes, RecipeDto[].class);
        logger.info("loaded test data from {}", fileName);
        return Arrays.asList(values);
    }

    private String assembleSearchParams(Boolean vegetarian, Integer minServings, Integer maxServings, String includesCsv,
                                        String excludesCsv, String partOfInstructions) {
        // isVegetarian=true&minServings=1&maxServings=10&inclusions=carrot&exclusions=sausage,ice&instruction=stew
        String retVal = "";
        retVal = addSearchParam(retVal, "isVegetarian", vegetarian);
        retVal = addSearchParam(retVal, "minServings", minServings);
        retVal = addSearchParam(retVal, "maxServings", maxServings);
        retVal = addSearchParam(retVal, "inclusions", includesCsv);
        retVal = addSearchParam(retVal, "exclusions", excludesCsv);
        retVal = addSearchParam(retVal, "instruction", partOfInstructions);
        if (retVal.isBlank()) return "";
        return "?" + retVal;
    }

    private String addSearchParam(String current, String prefix, Object value) {
        if (null != value) {
            String coupler = current.isBlank() ? "" : "&";
            return current + coupler + prefix + "=" + value;
        }
        return current;
    }

    @Test
    @Transactional
    @Disabled // TODO fix this!
    void getMatchingRecipesForAspects_no_props() throws IOException {
        List<RecipeDto> dtoListFromFile = load10RecipesFromFile();
        assertThat(dtoListFromFile).hasSize(10);

        for (RecipeDto dto : dtoListFromFile) {
            logger.info("loading recipe {}", dto.getName());
            RecipeDto saved = recipeService.saveNewRecipe(dto);
            assertThat(saved.getId()).isNotNull();
        }

        // check that database is filled
        assertThat(recipeService.findAllRecipes()).hasSize(10);

        Boolean vegetarian = null;
        Integer minServings = null;
        Integer maxServings = null;
        String includesCsv = null;
        String excludesCsv = null;
        String partOfInstructions = null;
        int expResults = 10;

        logger.info("search via service");
        final List<String> includes = NameStringHelper.mapCsvToList(includesCsv);
        final List<String> excludes = NameStringHelper.mapCsvToList(excludesCsv);
        List<RecipeHeaderDto> headersFromService = recipeService.findRecipeHeadersWithGivenParams(vegetarian, minServings, maxServings, includes, excludes, partOfInstructions);
        assertThat(headersFromService).hasSize(expResults);

        // check that inside has right answer
        logger.info("search via controller");
        ResponseEntity<List<RecipeHeaderDto>> matchingRecipesForAspects = recipeSearchController.getMatchingRecipesForAspects(vegetarian, minServings, maxServings, includesCsv, excludesCsv, partOfInstructions);
        List<RecipeHeaderDto> recipesFromController = matchingRecipesForAspects.getBody();
        assertThat(recipesFromController).hasSize(expResults);

        String searchValues = assembleSearchParams(vegetarian, minServings, maxServings, includesCsv, excludesCsv, partOfInstructions);
        logger.info("search via api with values = '" + searchValues + "'");
        RecipeHeaderDto[] recipeHeaderDtos = RestAssured.given().when()
                .get(ENDPOINT_SEARCH_PATH + searchValues)
                .then().assertThat()
                .statusCode(200)
                .extract().response().as(RecipeHeaderDto[].class);

        // check that outside is properly serialized
        assertThat(recipeHeaderDtos.length).isEqualTo(expResults);
    }


}