package nl.trickjurgen.recipes.repo;

import nl.trickjurgen.recipes.datamodel.IngredientType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientTypeRepo  extends JpaRepository<IngredientType, Long> {
    Optional<IngredientType> findByName(String name);
}
