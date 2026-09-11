package org.evomaster.client.java.controller.mongo;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.RegexDistanceUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.buildScaledTruthness;

public abstract class MongoHeuristicsCalculatorHelper {

    // TODO these constants should be replaced by DistanceHelper constants
    public static final double C = 0.1;
    public static final Truthness C_FALSE = new Truthness(C, 1.0);
    // TODO These constants should be refactored by TruthnessUtils constants
    public static final Truthness TRUE_C = new Truthness(1.0, C);

    static Truthness buildSafeScaledTruthness(double maxOfTrue) {
        if (maxOfTrue == 1.0) {
            return TRUE_C;
        } else {
            return buildScaledTruthness(C, maxOfTrue);
        }
    }

    /**
     * Evaluates whether the given input string matches the provided regular expression pattern.
     * The result is represented as a {@link Truthness} object, which captures both
     * the definitive match result and the closeness to a potential match.
     * Optionally, a {@link TaintHandler} can be used to handle tainting information
     * related to the regular expression processing.
     *
     * @param inputValue the input string to be matched against the regular expression
     * @param pattern the compiled {@link Pattern} representing the regular expression to match
     * @param taintHandler an optional implementation of {@link TaintHandler} to handle taint-related processing;
     *                     may be null if taint handling is not required
     * @return a {@link Truthness} object representing the result of the evaluation,
     *         where one component (true or false) is 1 to reflect the match status, and the other
     *         captures the approximation in cases of non-exact matches
     */
    static Truthness evaluateRegularExpression(String inputValue, Pattern pattern, TaintHandler taintHandler) {
        final String patternString = pattern.pattern();

        if (taintHandler != null) {
            final int patternFlags = pattern.flags();
            // TODO: tainting should take into account the pattern flags, which can change the matching behavior
            // TODO: regex could be a partial word match (MongoDB $regex) instead of a whole word match (Matcher.matches())
            taintHandler.handleTaintForRegex(inputValue, patternString);
        }


        Matcher matcher = pattern.matcher(inputValue);
        boolean matches = matcher.find();

        if (matches) {
            return TRUE_C;
        } else {
            // TODO this does not take into account pattern flags, which can change the matching behavior
            final int distance = RegexDistanceUtils.getStandardDistance(inputValue, patternString);
            // The distance approximation can be zero even when Java's matcher rejects the input
            // (for example, when flags affect line terminators). Keep non-matches strictly false.
            double ofTrue = 1d / (1.1d + distance);
            return buildSafeScaledTruthness(ofTrue);
        }
    }

    static int toIntValue(Boolean actualValue) {
        return actualValue ? 1 : 0;
    }

    static Truthness buildSafeScaledTruthness(Truthness truthness) {
        Objects.requireNonNull(truthness);

        return buildSafeScaledTruthness(truthness.getOfTrue());
    }
}
