package utils;

import org.apache.commons.lang3.StringUtils;

public class StringSimilarity {
    private double calculateWorstCaseDistance(String source, String target) {
        double sourceLen = source.length();
        double targetLen = target.length();
        double maxDistance = 0d;

        maxDistance += Math.max(sourceLen, targetLen);

        return maxDistance;
    }

    public double calculateSimilarity(String left, String right) {
        double levenshteinDistance = StringUtils.getLevenshteinDistance(left, right);
        double worstCaseDistance = calculateWorstCaseDistance(left, right);
        if (worstCaseDistance != 0d) {
            return (worstCaseDistance - levenshteinDistance) / worstCaseDistance;
        }
        return 0d;
    }
}
