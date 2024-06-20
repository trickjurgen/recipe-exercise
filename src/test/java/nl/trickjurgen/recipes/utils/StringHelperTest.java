package nl.trickjurgen.recipes.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringHelperTest {

    final static String[] INPUTS = {"CORiander", "BLUE cheese", "MILK", "cHEESE", "OLD CHEDDAR"};
    final static String[] EXPECTED = {"Coriander", "Blue Cheese", "Milk", "Cheese", "Old Cheddar"};

    @Test
    void verifyStaticValues() {
        for (int i = 0; i < INPUTS.length; i++) {
            assertThat(NameStringHelper.toTitleCase(INPUTS[i])).isEqualTo(EXPECTED[i]);
        }
    }

    @Test
    void verifyStaticValuesNegativeCompareAndEmptyTypes() {
        for (String input : INPUTS) {
            String titleCase = NameStringHelper.toTitleCase(input);
            assertThat(titleCase).isNotEqualTo(input);
            assertThat(titleCase).isEqualToIgnoringCase(input);
        }
        // empties
        assertThat(NameStringHelper.toTitleCase("")).isEqualTo("");
        assertThat(NameStringHelper.toTitleCase(" ")).isEqualTo("");
        assertThat(NameStringHelper.toTitleCase("    ")).isEqualTo("");
        assertThat(NameStringHelper.toTitleCase(" \t")).isEqualTo(""); // tab
    }

    @Test
    void failOnNullInput() {
        assertThatThrownBy(() -> NameStringHelper.toTitleCase(null)).isInstanceOf(IllegalArgumentException.class);
    }

}