package com.fxdb.mapping;

/**
 * Jaro-Winkler string similarity for comparing column names, with a touch of normalization
 * (lowercase, underscores/spaces/dashes stripped) so "customer_id" and "Customer Id" score
 * as identical before any fuzzy matching even kicks in.
 */
public final class NameSimilarity {

    private static final double PREFIX_SCALE = 0.1;
    private static final int MAX_PREFIX_LENGTH = 4;

    private NameSimilarity() {
    }

    public static double score(String a, String b) {
        String na = normalize(a);
        String nb = normalize(b);
        if (na.equals(nb)) {
            return 1.0;
        }
        return jaroWinkler(na, nb);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[_\\s-]", "");
    }

    private static double jaroWinkler(String s1, String s2) {
        double jaro = jaro(s1, s2);
        int prefixLength = commonPrefixLength(s1, s2);
        return jaro + (prefixLength * PREFIX_SCALE * (1 - jaro));
    }

    private static double jaro(String s1, String s2) {
        if (s1.isEmpty() && s2.isEmpty()) {
            return 1.0;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        int matchDistance = Math.max(s1.length(), s2.length()) / 2 - 1;
        boolean[] s1Matches = new boolean[s1.length()];
        boolean[] s2Matches = new boolean[s2.length()];

        int matches = 0;
        for (int i = 0; i < s1.length(); i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, s2.length());
            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) {
                    continue;
                }
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        double transpositions = 0;
        int k = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (!s1Matches[i]) {
                continue;
            }
            while (!s2Matches[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }
        transpositions /= 2;

        return ((matches / (double) s1.length())
                + (matches / (double) s2.length())
                + ((matches - transpositions) / matches)) / 3.0;
    }

    private static int commonPrefixLength(String s1, String s2) {
        int max = Math.min(MAX_PREFIX_LENGTH, Math.min(s1.length(), s2.length()));
        int i = 0;
        while (i < max && s1.charAt(i) == s2.charAt(i)) {
            i++;
        }
        return i;
    }
}
