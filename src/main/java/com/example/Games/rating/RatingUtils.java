package com.example.Games.rating;

public class RatingUtils {

    public static RatingLabel computeRatingLabel(long total, long recommendedCount) {
        if (total == 0) return RatingLabel.UNRATED;

        double ratio = (double) recommendedCount / total;

        if (ratio >= 0.95) return RatingLabel.OVERWHELMINGLY_POSITIVE;
        if (ratio >= 0.80) return RatingLabel.VERY_POSITIVE;
        if (ratio >= 0.60) return RatingLabel.MOSTLY_POSITIVE;
        if (ratio >= 0.40) return RatingLabel.MIXED;

        return RatingLabel.NEGATIVE;
    }
}
