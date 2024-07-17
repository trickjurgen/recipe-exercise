package nl.trickjurgen.recipes.utils;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NameStringHelperTest {

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

    @Test
    void verifyCsvInStrings() {
        String emptyStr1 = "";
        String emptyStr2 = "  ";
        String singleValue = "Mustard";
        String multiples = "Collard,Greens,Cheese";
        String multiplesWithDefect = "Collard,Greens,";
        String wrongDelimiter = "Pepper.Salt";

        assertThat(NameStringHelper.mapCsvToList(null)).isEqualTo(Lists.newArrayList());
        assertThat(NameStringHelper.mapCsvToList(emptyStr1)).isEqualTo(Lists.newArrayList());
        assertThat(NameStringHelper.mapCsvToList(emptyStr2)).isEqualTo(Lists.newArrayList());

        assertThat(NameStringHelper.mapCsvToList(singleValue)).isEqualTo(List.of("Mustard"));
        assertThat(NameStringHelper.mapCsvToList(multiples)).isEqualTo(List.of("Collard","Greens","Cheese"));
        assertThat(NameStringHelper.mapCsvToList(multiplesWithDefect)).isEqualTo(List.of("Collard","Greens"));
        assertThat(NameStringHelper.mapCsvToList(wrongDelimiter)).isEqualTo(List.of("Pepper.Salt"));
    }

}