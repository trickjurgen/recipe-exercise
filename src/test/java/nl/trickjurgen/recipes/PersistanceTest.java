package nl.trickjurgen.recipes;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.trickjurgen.recipes.datamodel.Recipe;
import nl.trickjurgen.recipes.repo.RecipeRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PersistanceTest {

    private final RecipeRepo recipeRepo;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Autowired
    public PersistanceTest (RecipeRepo recipeRepo, ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.recipeRepo = recipeRepo;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @Test
    void attemptLoadRecipeFromFile () {
        final String fileName = "r1-chicken-curry.json";
        final String path = "classpath:recipes" + File.separator + fileName;

        List<Recipe> recipeList = recipeRepo.findAll();
        assertThat(recipeList).isEmpty();

        // TODO the rest
    }

}
