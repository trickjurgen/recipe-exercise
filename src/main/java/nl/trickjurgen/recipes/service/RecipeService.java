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

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
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
        Recipe newRecipe = RecepAndIngrMapper.dtoToRecipeNoIngr(recipeDto);
        if (recipeRepo.findByName(newRecipe.getName()).isPresent()) {
            logger.warn("already exists! {}", newRecipe.getName());
            throw new DuplicateRecipeException("already exist and not update call");
        }
        Set<IngredientDto> ingredients = recipeDto.getIngredients();
        for (IngredientDto ingredientDto : ingredients) {
            newRecipe.getIngredients().add(createAndSaveIngredient(ingredientDto));
        }
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
        HashSet<Ingredient> target = new HashSet<>();
        List<String> newIngNames = newIngredients.stream().map(IngredientDto::getName).map(NameStringHelper::toTitleCase).toList();
        // update or remove existing
        for (Ingredient ing: savedIngredients) {
            if (newIngNames.contains(ing.getIngredientType().getName())) {
                try {
                    Ingredient changed = updateIngredient(ing, newIngredients);
                    ingredientRepo.save(changed);
                    target.add(changed);
                } catch (NoSuchElementException e) {
                    logger.error("failed to update ingredient {}", ing.getIngredientType().getName());
                    throw new RecipeParameterException("internal update issue");
                }
            } else {
                ingredientRepo.delete(ing);
            }
        }
        // add all the new ones
        List<String> curIngrList = target.stream().map(i -> i.getIngredientType().getName()).toList();
        newIngredients.stream()
                .filter(dto -> !curIngrList.contains(NameStringHelper.toTitleCase(dto.getName())))
                .forEach(dto -> {
                    Ingredient andSaveIngredient = createAndSaveIngredient(dto);
                    target.add(andSaveIngredient);
                });
        return target;
    }

    private Ingredient updateIngredient(Ingredient ingredient, Set<IngredientDto> newIngredients) throws NoSuchElementException {
        final String findName = ingredient.getIngredientType().getName();
        IngredientDto updatedIngredientDto = newIngredients.stream()
                .filter(i -> i.getName().equalsIgnoreCase(findName))
                .findFirst().orElseThrow();
        ingredient.setVolume(updatedIngredientDto.getVolume());
        ingredient.setRemark(updatedIngredientDto.getRemark());
        return ingredient;
    }

    public boolean deleteRecipe(final Long recipeId) {
        if (recipeId == null || recipeId < 1L || !recipeRepo.existsById(recipeId)) {
            throw new RecipeNotFoundException("bad recipe id");
        }
        try {
            Recipe storedRecipe = recipeRepo.getReferenceById(recipeId);
            storedRecipe.getIngredients().forEach(ingredientRepo::delete);
            // TODO this could leave ingredientTypes unused, maybe cleanup of those here?
            recipeRepo.delete(storedRecipe);
        } catch (IllegalArgumentException e) {
            logger.error("failed to delete (part of) recipe with ID {}", recipeId);
            return false;
        }
        return true;
    }

    public List<RecipeDto> findRecipesWithSpecificIngredients(final List<String> includes, final List<String> excludes) {
        // TODO this
    }

}
