package nl.trickjurgen.recipes.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class NameStringHelper {

    private static final String WORD_SEP = " ";

    /**
     * 'correct' the case usage of a string to title case; each word starts with a capital and the rest is lower case
     *
     * @param input give string
     * @return reformatted version of input
     * @throws IllegalArgumentException if input is null
     */
    public static String toTitleCase(final String input) {
        if (null == input) {
            throw new IllegalArgumentException("bad input");
        }
        return Arrays.stream(input.trim().split(WORD_SEP))
                .map(word -> word.isEmpty() ? word : singleWordToTitleCase(word))
                .collect(Collectors.joining(WORD_SEP));
    }

    private static String singleWordToTitleCase(final String word) {
        return Character.toTitleCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }

}
