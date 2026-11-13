package com.civicsphere.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility for natural language text normalization and similarity computation.
 * Uses tokenization, stopword removal, and Jaccard token similarity.
 */
public class TextSimilarity {

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "as", "at", "be", "because", "been", "before", "being", "below",
        "between", "both", "but", "by", "could", "did", "do", "does", "doing", "down",
        "during", "each", "few", "for", "from", "further", "had", "has", "have", "having",
        "he", "her", "here", "hers", "herself", "him", "himself", "his", "how", "i",
        "if", "in", "into", "is", "it", "its", "itself", "me", "more", "most", "my",
        "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other",
        "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "she", "should",
        "so", "some", "such", "than", "that", "the", "their", "theirs", "them", "themselves",
        "then", "there", "these", "they", "this", "those", "through", "to", "too", "under",
        "until", "up", "very", "was", "we", "were", "what", "when", "where", "which",
        "while", "who", "whom", "why", "with", "would", "you", "your", "yours", "yourself"
    ));

    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s]");

    /**
     * Normalizes text by lowercasing, stripping punctuation, and trimming extra spaces.
     */
    public static String normalize(String text) {
        if (text == null) return "";
        return CLEAN_PATTERN.matcher(text.toLowerCase()).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Extracts meaningful keyword tokens from text (excluding stopwords and short tokens).
     */
    public static Set<String> extractTokens(String text) {
        Set<String> tokens = new HashSet<>();
        String normalized = normalize(text);
        if (normalized.isEmpty()) return tokens;

        String[] parts = normalized.split("\\s+");
        for (String part : parts) {
            if (part.length() > 2 && !STOPWORDS.contains(part)) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    /**
     * Calculates Jaccard token similarity between two texts.
     * Range: 0.0 to 1.0
     */
    public static double jaccardSimilarity(String text1, String text2) {
        Set<String> tokens1 = extractTokens(text1);
        Set<String> tokens2 = extractTokens(text2);

        if (tokens1.isEmpty() && tokens2.isEmpty()) return 1.0;
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        return (double) intersection.size() / (double) union.size();
    }

    /**
     * Checks if two normalized location strings match or have significant token overlap.
     */
    public static boolean isLocationMatch(String loc1, String loc2) {
        if (loc1 == null || loc2 == null) return false;
        String n1 = normalize(loc1);
        String n2 = normalize(loc2);
        if (n1.isEmpty() || n2.isEmpty()) return false;
        if (n1.equals(n2) || n1.contains(n2) || n2.contains(n1)) return true;

        Set<String> t1 = extractTokens(n1);
        Set<String> t2 = extractTokens(n2);
        if (t1.isEmpty() || t2.isEmpty()) return false;

        Set<String> inter = new HashSet<>(t1);
        inter.retainAll(t2);
        return !inter.isEmpty() && ((double) inter.size() / Math.min(t1.size(), t2.size()) >= 0.5);
    }
}

