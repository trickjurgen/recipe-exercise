package nl.trickjurgen.recipes.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        for (String input : inputs) {
            String titleCase = StringHelper.toTitleCase(input);
            assertThat(titleCase).isNotEqualTo(input);
            assertThat(titleCase).isEqualToIgnoringCase(input);
        }
        assertThat(StringHelper.toTitleCase("")).isEqualTo("");
        assertThat(StringHelper.toTitleCase(" ")).isEqualTo("");
        assertThat(StringHelper.toTitleCase("    ")).isEqualTo("");
        assertThat(StringHelper.toTitleCase(" \t")).isEqualTo(""); // tab
    }

    @Test
    void failOnNullInput() {
        assertThatThrownBy(() -> StringHelper.toTitleCase(null)).isInstanceOf(IllegalArgumentException.class);
    }

}