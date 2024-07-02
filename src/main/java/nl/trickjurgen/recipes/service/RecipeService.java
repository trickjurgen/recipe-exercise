package nl.trickjurgen.recipes.service;

import nl.trickjurgen.recipes.datamodel.Ingredient;
import nl.trickjurgen.recipes.datamodel.IngredientType;
import nl.trickjurgen.recipes.datamodel.Recipe;
import nl.trickjurgen.recipes.dto.IngredientDto;
import nl.trickjurgen.recipes.dto.RecipeDto;
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
import java.util.Collections;
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
        if (id == null || id < 1L || !recipeRepo.existsById(id)) {
            throw new RecipeNotFoundException("bad recipe id");
        }
        return RecepAndIngrMapper.recipeToDto(recipeRepo.getReferenceById(id));
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

    public RecipeDto updateRecipe(final RecipeDto recipeDto) {
        if (recipeDto.getId() == null || recipeDto.getId() < 1L || !recipeRepo.existsById(recipeDto.getId())) {
            throw new RecipeNotFoundException("bad recipe id");
        }
        Recipe newVersionRecipe = RecepAndIngrMapper.dtoToRecipeNoIngr(recipeDto);
        // get stored
        Recipe storedRecipe = recipeRepo.getReferenceById(recipeDto.getId());
        // override fields with changed data, ID can't have changed
        storedRecipe.setName(newVersionRecipe.getName());
        storedRecipe.setVegetarian(newVersionRecipe.isVegetarian());
        storedRecipe.setServings(newVersionRecipe.getServings());
        storedRecipe.setInstructions(newVersionRecipe.getInstructions());
        storedRecipe.setIngredients(mergeIngredients(storedRecipe.getIngredients(), recipeDto.getIngredients()));
        // overwrite mutations in repo and be happy
        Recipe saved = recipeRepo.save(storedRecipe);
        return RecepAndIngrMapper.recipeToDto(saved);
    }

    private Set<Ingredient> mergeIngredients(Set<Ingredient> savedIngredients, Set<IngredientDto> newIngredients) {
        final HashSet<Ingredient> mergedIngredients = new HashSet<>();
        final List<String> newIngrNames = newIngredients.stream().map(IngredientDto::getName).map(NameStringHelper::toTitleCase).toList();
        // update or remove existing
        for (Ingredient ing : savedIngredients) {
            if (newIngrNames.contains(ing.getIngredientType().getName())) {
                Ingredient changed = updateIngredientWithDtoFields(ing, newIngredients);
                ingredientRepo.save(changed);
                mergedIngredients.add(changed);
            } else {
                ingredientRepo.delete(ing);
            }
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
        if (recipeId == null || recipeId < 1L || !recipeRepo.existsById(recipeId)) {
            throw new RecipeNotFoundException("bad recipe id");
        }
        try {
            Recipe storedRecipe = recipeRepo.getReferenceById(recipeId);
            storedRecipe.getIngredients().forEach(ingredientRepo::delete);
            // FIXME this could leave ingredientTypes unused, maybe cleanup of those here?
            recipeRepo.delete(storedRecipe);
        } catch (IllegalArgumentException e) {
            logger.error("failed to delete (part of) recipe with ID {}", recipeId);
            return false;
        }
        return true;
    }

    public List<RecipeDto> findRecipesWithSpecificDetails(final Boolean isVeggie, final Integer minServing, final Integer maxServing,
                                                          final List<String> includes, final List<String> excludes) {
        List<Recipe> allRecipes = recipeRepo.findAll();

        List<Predicate<Recipe>> filters = new ArrayList<>();
        Optional.ofNullable(isVeggie).ifPresent(bool -> filters.add(recipe -> recipe.isVegetarian() == bool));
        Optional.ofNullable(minServing).ifPresent(minVal -> filters.add(recipe -> recipe.getServings() >= minVal));
        Optional.ofNullable(maxServing).ifPresent(maxVal -> filters.add(recipe -> recipe.getServings() <= maxVal));

        if (null != includes) {
            filters.addAll(
                    includes.stream()
                            .map(NameStringHelper::toTitleCase)
                            .map(titleCase -> (Predicate<Recipe>) recipe -> flatListIngredients(recipe).contains(titleCase))
                            .toList()
            );
        }

        if (null != excludes) {
            for (String ingredientName : excludes) {
                final String titleCase = NameStringHelper.toTitleCase(ingredientName);
                filters.add(recipe -> !flatListIngredients(recipe).contains(titleCase));
            }
        }

        final Predicate<Recipe> combinedFilter = filters.stream().reduce(Predicate::and).orElse(x -> true);
        final List<Recipe> filteredRecipes = allRecipes.stream().filter(combinedFilter).toList();
        return filteredRecipes.stream()
                .map(RecepAndIngrMapper::recipeToDto)
                .collect(Collectors.toList());
    }

    /*default*/ List<String> flatListIngredients(final Recipe recipe) {
        if (recipe.getIngredients() == null) return Collections.emptyList();
        return recipe.getIngredients().stream().map(ingredient -> ingredient.getIngredientType().getName()).toList();
    }

}
