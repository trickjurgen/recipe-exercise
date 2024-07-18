package nl.trickjurgen.recipes.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.service.RecipeService;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecipeControllerTest {

    private final Logger logger = LoggerFactory.getLogger(RecipeControllerTest.class);

    // TODO setup some test data; as test run on H2 database, that is initially empty, load some data to be not empty
    // FIXME this test could also use test-containers instead of H2

    @Autowired
    private RecipeService recipeService;

    @LocalServerPort
    private int port;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ObjectMapper objectMapper;

    final static String BASE_PATH = "/recipes";

    @BeforeEach
    public void setUpEach() {
        RestAssured.port = port;
    }

    private List<Long> savedDbIds = Lists.newArrayList();

    @Test
    @Order(1)
    void getRecipeById_bad_id() {
        given().when().get("/recipes/null")
                .then().assertThat().statusCode(400);
        given().when().get("/recipes/-1")
                .then().assertThat().statusCode(404);
    }

    @Test
    @Order(2)
    void getRecipeById_not_found() {
        given().when().get(BASE_PATH + "/404")
                .then().assertThat().statusCode(404);
    }

    private void loadRecipeFromFile(final String filename) throws IOException {
        File file = resourceLoader.getResource("classpath:recipes/" + filename).getFile();
        byte[] bytes = Files.readAllBytes(file.toPath());
        RecipeDto recipeDto = objectMapper.readValue(bytes, RecipeDto.class);
        RecipeDto saved = recipeService.saveNewRecipe(recipeDto);
        savedDbIds.add(saved.getId());
        logger.info("Stored recipe from {} as id {}", filename, saved.getId());
    }

    @Test
    @Order(3)
    void getRecipeById_ok() throws IOException {
        loadRecipeFromFile("r1-chicken-curry.json");
        assertThat(savedDbIds).isNotEmpty();

        given().when().get(BASE_PATH + "/" + savedDbIds.getLast())
                .then().assertThat().statusCode(200);
        // TODO verify some content
    }

    @Test
    void createRecipe() {
    }

    @Test
    void updateRecipe() {
    }

    @Test
    void deleteRecipe() {
    }

    @Test
    void getAllRecipeNames() {
    }

}