package nl.trickjurgen.recipes.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NameStringHelper {

    private static final String SPLIT_WORD_SEP = "\\s+"; // regex for any whitespaces
    private static final String COMBINE_WORD_SEP = " ";
    private static final String COMMA = ",";


    /**
     * 'correct' the case usage of a string to title case; each word starts with a capital and the rest is lower case
     *
     * @param input give string
     * @return reformatted version of input
     * @throws IllegalArgumentException if input is null
     */
    public static String toTitleCase(final String input) {
        Optional.ofNullable(input).orElseThrow(() -> new IllegalArgumentException("bad input"));
        return Arrays.stream(input.split(SPLIT_WORD_SEP))
                .map(word -> word.isEmpty() ? word : singleWordToTitleCase(word))
                .collect(Collectors.joining(COMBINE_WORD_SEP));
    }

    private static String singleWordToTitleCase(final String word) {
        return Character.toTitleCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }

    public static List<String> mapCsvToList(final String csvString) {
        if (null == csvString) return Collections.emptyList();
        return Stream.of(csvString.split(COMMA))
                .map(String::trim)
                .filter(str->!str.isBlank())
                .collect(Collectors.toList());
    }
}
