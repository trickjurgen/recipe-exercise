package nl.trickjurgen.recipes.service;

import nl.trickjurgen.recipes.datamodel.Ingredient;
import nl.trickjurgen.recipes.datamodel.IngredientType;
import nl.trickjurgen.recipes.datamodel.Recipe;
import nl.trickjurgen.recipes.dto.IngredientDto;
import nl.trickjurgen.recipes.dto.RecipeDto;
import nl.trickjurgen.recipes.dto.RecipeHeaderDto;
import nl.trickjurgen.recipes.exception.DuplicateRecipeException;
import nl.trickjurgen.recipes.exception.RecipeNotFoundException;
import nl.trickjurgen.recipes.exception.RecipeParameterException;
import nl.trickjurgen.recipes.mapper.RecepAndIngrMapper;
import nl.trickjurgen.recipes.repo.IngredientRepo;
import nl.trickjurgen.recipes.repo.IngredientTypeRepo;
import nl.trickjurgen.recipes.repo.RecipeRepo;
import nl.trickjurgen.recipes.utils.NameStringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private final RecipeRepo recipeRepo;
    private final IngredientTypeRepo ingredientTypeRepo;
    private final IngredientRepo ingredientRepo;

    private final Logger logger = LoggerFactory.getLogger(RecipeService.class);

    @Autowired
    public RecipeService(RecipeRepo recipeRepo, IngredientTypeRepo ingredientTypeRepo, IngredientRepo ingredientRepo) {
        this.recipeRepo = recipeRepo;
        this.ingredientTypeRepo = ingredientTypeRepo;
        this.ingredientRepo = ingredientRepo;
    }

    public List<RecipeDto> findAllRecipes() {
        return recipeRepo.findAll()
                .stream()
                .map(RecepAndIngrMapper::recipeToDto)
                .collect(Collectors.toList());
    }

    public RecipeDto findRecipeById(final Long id) {
        verifyIdValidAndExists(id);
        return RecepAndIngrMapper.recipeToDto(recipeRepo.getReferenceById(id));
    }

    private void verifyIdValidAndExists(final Long id) {
        if (id == null || id < 1L || !recipeRepo.existsById(id)) {
            throw new RecipeNotFoundException("bad recipe id");
        }
    }

    public RecipeDto saveNewRecipe(final RecipeDto recipeDto) {
        if (recipeDto.getId() != null) {
            throw new RecipeParameterException("new recipe should not have an ID");
        }
        Recipe newRecipe = RecepAndIngrMapper.dtoToRecipeNoIngr(recipeDto); // this includes title-case fix on name
        if (recipeRepo.findByName(newRecipe.getName()).isPresent()) {
            logger.warn("already exists! {}", newRecipe.getName());
            throw new DuplicateRecipeException("already exists and this is not an update call");
        }
        newRecipe.getIngredients().addAll(
                recipeDto.getIngredients().stream().map(this::createAndSaveIngredient).toList()
        );
        Recipe saved = recipeRepo.save(newRecipe);
        return RecepAndIngrMapper.recipeToDto(saved);
    }

    private Ingredient createAndSaveIngredient(IngredientDto ing) {
        String correctedName = NameStringHelper.toTitleCase(ing.getName());
        Optional<IngredientType> typeByName = ingredientTypeRepo.findByName(correctedName);
        IngredientType ingType = typeByName.orElseGet(() -> ingredientTypeRepo.save(IngredientType.builder().name(correctedName).build()));
        return ingredientRepo.save(RecepAndIngrMapper.dtoToIngredientWithType(ing, ingType));
    }

    public RecipeDto updateRecipe(final Long recipeId, final RecipeDto recipeDto) {
        verifyIdValidAndExists(recipeId);
        logger.info("id verified");
        if (!recipeId.equals(recipeDto.getId())) {
            throw new RecipeParameterException("Bad ID or does not match data");
        }
        Recipe newVersionRecipe = RecepAndIngrMapper.dtoToRecipeNoIngr(recipeDto);
        // get stored
        Recipe storedRecipe = recipeRepo.getReferenceById(recipeDto.getId());
        // override fields with changed data, ID can't have changed
        storedRecipe.setName(newVersionRecipe.getName());
        storedRecipe.setVegetarian(newVersionRecipe.isVegetarian());
        storedRecipe.setServings(newVersionRecipe.getServings());
        storedRecipe.setInstructions(newVersionRecipe.getInstructions());
        final List<Ingredient> ingredientsToBeDeleted = findRedundantIngredients(storedRecipe.getIngredients(), recipeDto.getIngredients());
        storedRecipe.setIngredients(createMergedIngredients(storedRecipe.getIngredients(), recipeDto.getIngredients()));
        // overwrite mutations in repo and be happy
        Recipe saved = recipeRepo.save(storedRecipe);
        ingredientsToBeDeleted.forEach(ingredientRepo::delete);
        return RecepAndIngrMapper.recipeToDto(saved);
    }

    private List<Ingredient> findRedundantIngredients(Set<Ingredient> savedIngredients, Set<IngredientDto> newIngredients) {
        final List<String> newIngredientNames = newIngredients.stream().map(IngredientDto::getName).map(NameStringHelper::toTitleCase).toList();
        Predicate<Ingredient> oldIngredientIsNotInNewOnes = ingredient -> !newIngredientNames.contains(ingredient.getIngredientType().getName());
        return savedIngredients.stream().filter(oldIngredientIsNotInNewOnes).toList();
    }

    private Set<Ingredient> createMergedIngredients(Set<Ingredient> savedIngredients, Set<IngredientDto> newIngredients) {
        final HashSet<Ingredient> mergedIngredients = new HashSet<>();
        final List<String> newIngredientNames = newIngredients.stream().map(IngredientDto::getName).map(NameStringHelper::toTitleCase).toList();
        // update existing, ignore delete-able ones
        for (Ingredient ingredient : savedIngredients) {
            final boolean oldIngredientIsInNewOnes = newIngredientNames.contains(ingredient.getIngredientType().getName());
            if (oldIngredientIsInNewOnes) {
                Ingredient changed = updateIngredientWithDtoFields(ingredient, newIngredients);
                ingredientRepo.save(changed);
                mergedIngredients.add(changed);
            } // else ignore, it will be deleted after saving merge
        }
        // add all the new ones
        final List<String> curIngrList = mergedIngredients.stream().map(i -> i.getIngredientType().getName()).toList();
        final Predicate<IngredientDto> nameIsNotInCurrentIngredientList =
                dto -> !curIngrList.contains(NameStringHelper.toTitleCase(dto.getName()));
        mergedIngredients.addAll(newIngredients.stream()
                .filter(nameIsNotInCurrentIngredientList)
                .map(this::createAndSaveIngredient)
                .toList());
        return mergedIngredients;
    }

    private Ingredient updateIngredientWithDtoFields(Ingredient ingredient, Set<IngredientDto> newIngredients) {
        final IngredientDto updatedIngredientDto = findMatchingDtoByName(newIngredients, ingredient.getIngredientType().getName());
        ingredient.setVolume(updatedIngredientDto.getVolume());
        ingredient.setRemark(updatedIngredientDto.getRemark());
        return ingredient;
    }

    private IngredientDto findMatchingDtoByName(Set<IngredientDto> newIngredients, String nameToFind) {
        final Predicate<IngredientDto> matchByName = ingredientDto -> ingredientDto.getName().equalsIgnoreCase(nameToFind);
        return newIngredients.stream()
                .filter(matchByName)
                .findFirst()
                .orElseThrow(() -> {
                    // this is very unlikely to happen as we did a range check in 'newIngredients' before starting the update
                    logger.error("failed to update ingredient {}", nameToFind);
                    return new RecipeParameterException("internal update issue");
                });
    }

    public boolean deleteRecipe(final Long recipeId) {
        verifyIdValidAndExists(recipeId);
        try {
            Recipe storedRecipe = recipeRepo.getReferenceById(recipeId);
            recipeRepo.delete(storedRecipe);
        } catch (IllegalArgumentException e) {
            logger.error("failed to delete (part of) recipe with ID {}", recipeId);
            return false;
        }
        return true;
    }

    private void addFilterIfObjNotNull(List<Predicate<Recipe>> filters, final Object obj, final Predicate<Recipe> predicate) {
        if (obj != null) filters.add(predicate);
    }

    public List<RecipeDto> findRecipesWithSpecificDetails(final Boolean isVeggie, final Integer minServing,
                                                          final Integer maxServing, final List<String> includes,
                                                          final List<String> excludes, final String partOfInstructions) {
        List<Recipe> allRecipes = recipeRepo.findAll();

        final List<Predicate<Recipe>> filters = new ArrayList<>();
        addFilterIfObjNotNull(filters, isVeggie, recipe -> recipe.isVegetarian() == isVeggie);
        addFilterIfObjNotNull(filters, minServing, recipe -> recipe.getServings() >= minServing);
        addFilterIfObjNotNull(filters, maxServing, recipe -> recipe.getServings() <= maxServing);
        Optional.ofNullable(includes).ifPresent(list -> list.forEach(str -> filters.add(recipe -> flattenIngredients(recipe).contains(str.toLowerCase()))));
        Optional.ofNullable(excludes).ifPresent(list -> list.forEach(str -> filters.add(recipe -> !flattenIngredients(recipe).contains(str.toLowerCase()))));
        addFilterIfObjNotNull(filters, partOfInstructions,
                recipe -> recipe.getInstructions().toLowerCase().contains(partOfInstructions.toLowerCase()));

        final Predicate<Recipe> combinedFilter = filters.stream().reduce(Predicate::and).orElse(x -> true);
        final List<Recipe> filteredRecipes = allRecipes.stream().filter(combinedFilter).toList();
        return filteredRecipes.stream()
                .map(RecepAndIngrMapper::recipeToDto)
                .collect(Collectors.toList());
    }

    /*default (for test)*/ String flattenIngredients(final Recipe recipe) {
        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) return "";
        final String delimiter = " ";
        return recipe.getIngredients().stream()
                .map(ingredient -> ingredient.getIngredientType().getName().toLowerCase())
                .collect(Collectors.joining(delimiter));
    }

    public List<RecipeHeaderDto> findRecipeHeadersWithGivenParams(final Boolean isVeggie, final Integer minServing,
                                                                  final Integer maxServing, final List<String> includes,
                                                                  final List<String> excludes, final String instruction) {
        final List<RecipeDto> fullRecipes = findRecipesWithSpecificDetails(isVeggie, minServing, maxServing, includes, excludes, instruction);
        return fullRecipes.stream()
                .filter(recipeDto -> recipeDto.getId() != null)
                .map(RecepAndIngrMapper::RecipeDtoToHeader).toList();
    }

    public RecipeDto findRecipeByName(final String name) {
        Optional<Recipe> byName = recipeRepo.findByName(name);
        return byName.map(RecepAndIngrMapper::recipeToDto).orElse(null);
    }

}
