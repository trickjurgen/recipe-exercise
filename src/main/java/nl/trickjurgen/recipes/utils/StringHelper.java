package nl.trickjurgen.recipes.utils;

import org.apache.logging.log4j.util.Strings;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringHelper {

    private static final String WORD_SEP = " ";

    public static String toTitleCase(final String input) {
        if (null == input || Strings.isBlank(input)) {
            throw new IllegalArgumentException("bad input");
        }
        return Arrays.stream(input.trim().split(WORD_SEP))
                .map(word -> word.isEmpty() ? word : singleWordToTitleCase(word))
                .collect(Collectors.joining(WORD_SEP));
    }

    private static String singleWordToTitleCase(final String word){
        return Character.toTitleCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }

}
