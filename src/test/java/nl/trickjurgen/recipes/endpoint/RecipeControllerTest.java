package nl.trickjurgen.recipes.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import nl.trickjurgen.recipes.dto.IngredientDto;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.service.RecipeService;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RecipeControllerTest {

    private final Logger logger = LoggerFactory.getLogger(RecipeControllerTest.class);

    // TODO setup some test data; as test run on H2 database, that is initially empty, load some data to be not empty
    // FIXME this test could also use test-containers instead of H2


    @LocalServerPort
    private int port;

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final RecipeService recipeService;

    final static String ENDPOINT_BASE_PATH = "/recipes";

    @Autowired
    public RecipeControllerTest(ResourceLoader resourceLoader, ObjectMapper objectMapper, RecipeService recipeService) {
        this.recipeService = recipeService;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @BeforeEach
    public void setUpEach() {
        RestAssured.port = port;
    }

    private final List<Long> savedDbIds = Lists.newArrayList();

    @Test
    @Order(1)
    void getRecipeById_bad_id() {
        RestAssured.given().when().get("/recipes/null")
                .then().assertThat().statusCode(400);
        RestAssured.given().when().get("/recipes/-1")
                .then().assertThat().statusCode(404);
    }

    @Test
    @Order(2)
    void getRecipeById_not_found() {
        RestAssured.given().when().get(ENDPOINT_BASE_PATH + "/404")
                .then().assertThat().statusCode(404);
        RestAssured.given().when().get(ENDPOINT_BASE_PATH + "/606")
                .then().assertThat().statusCode(404);
    }

    private void loadRecipeFromFileAndPutInDb(final String filename) throws IOException {
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
        loadRecipeFromFileAndPutInDb("r1-chicken-curry.json");
        assertThat(savedDbIds).isNotEmpty();

        Response response = RestAssured.given().when().get(ENDPOINT_BASE_PATH + "/" + savedDbIds.getLast())
                .then().assertThat()
                .statusCode(200)
                .extract().response();
        assertThat(response).isNotNull();
        RecipeDto recipeDto = response.as(RecipeDto.class);

        assertThat(recipeDto).isNotNull();
        assertThat(recipeDto.getId()).isEqualTo(savedDbIds.getLast());
        assertThat(recipeDto.getName()).contains("Curry");
    }

    @Test
    @Order(4)
    @Transactional
    void createRecipe() {
        RecipeDto hotToddyRecipe = createHotToddyRecipe();

        RecipeDto returned =
                RestAssured.given()
                        .contentType(ContentType.JSON)
                        .body(hotToddyRecipe)
                        .when()
                        .post(ENDPOINT_BASE_PATH)
                        .then()
                        .statusCode(201)
                        .extract().response().as(RecipeDto.class);
        savedDbIds.add(returned.getId());

        assertThat(returned).isNotNull();
        assertThat(returned.getId()).isNotNull();
        assertThat(returned.getName()).contains("Toddy");
        assertThat(returned.getIngredients()).extracting("id")
                .hasSize(4)
                .doesNotContainNull();

        RecipeDto recipeById = recipeService.findRecipeById(savedDbIds.getLast());
        assertThat(recipeById.getName()).contains("Hot");
    }

    private static RecipeDto createHotToddyRecipe() {
        return RecipeDto.builder() // skip the id
                .name("Hot Toddy")
                .isVegetarian(true)
                .servings(1)
                .instructions("Boil water, add to cup, add lemon juice + whiskey + honey. Drink very warm.")
                .ingredients(Set.of( // skip the id
                        IngredientDto.builder().name("Water").volume("2 DL.").build(),
                        IngredientDto.builder().name("whiskey").volume("1 shot").remark("any blend will do").build(),
                        IngredientDto.builder().name("lemon juice").volume("3 tsp.").build(),
                        IngredientDto.builder().name("honey").volume("1 dollop").build()
                ))
                .build();
    }

    @Test
    @Order(5)
    @Transactional
    void updateRecipe() {
        // recipe can be created in previous test, if this test is run in isolation then create it
        RecipeDto hotToddy = recipeService.findRecipeByName("Hot Toddy");
        if (null == hotToddy) {
            hotToddy = RestAssured.given()
                            .contentType(ContentType.JSON).body(createHotToddyRecipe())
                            .when().post(ENDPOINT_BASE_PATH)
                            .then().statusCode(201).extract().response().as(RecipeDto.class);
        }
        assertThat(recipeService.findRecipeByName("Hot Toddy")).isNotNull();
        final long savedId = hotToddy.getId();
        assertThat(savedId).isPositive();

        // create "changed" contents
        RecipeDto newVersion = RecipeDto.builder()
                .id(savedId) // now ID is important!
                .name(hotToddy.getName())
                .isVegetarian(true)
                .instructions("Boil water, add to cup, add lime juice + bourbon + sugar. Drink very hot.")
                .ingredients(Set.of(
                        IngredientDto.builder().name("Water").volume("2 DL.").build(),
                        IngredientDto.builder().name("bourbon").volume("4 cl").remark("any brand will do").build(),
                        IngredientDto.builder().name("lime juice").volume("3 tsp.").build(),
                        IngredientDto.builder().name("sugar").volume("1 scoop").build()
                ))
                .build();

        RecipeDto resultingRecipe = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(newVersion)
                .when()
                .put(ENDPOINT_BASE_PATH + "/" + savedId)
                .then()
                .statusCode(200)
                .extract().response().as(RecipeDto.class);


        // TODO verify changed data

    }

    @Test
    @Transactional
    void deleteRecipe() throws IOException {
        loadRecipeFromFileAndPutInDb("r2-spagetti-bol.json");
        long storedRecipeId = savedDbIds.getLast();

        assertThat(storedRecipeId).isPositive();
        assertThat(recipeService.findRecipeById(storedRecipeId)).isNotNull();

        // tODO call delete

        // tODO assert it can't be found anymore (2 ways?)

    }

    @Test
    void getAllRecipeNames() {
    }

}