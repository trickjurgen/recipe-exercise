package nl.trickjurgen.recipes.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringHelperTest {

    final static String[] inputs = {"CORiander", "BLUE cheese", "MILK", "cHEESE", "OLD CHEDDAR"};
    final static String[] expected = {"Coriander", "Blue Cheese", "Milk", "Cheese", "Old Cheddar"};

    @Test
    void verifyStaticValues() {
        for (int i = 0; i < inputs.length; i++) {
            assertThat(StringHelper.toTitleCase(inputs[i])).isEqualTo(expected[i]);
        }
    }

    @Test
    void verifyStaticValuesNegativeCompare() {
        for (int i = 0; i < inputs.length; i++) {
            String titleCase = StringHelper.toTitleCase(inputs[i]);
            assertThat(titleCase).isNotEqualTo(inputs[i]);
            assertThat(titleCase).isEqualToIgnoringCase(inputs[i]);
        }
    }

}