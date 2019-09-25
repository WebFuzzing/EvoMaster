package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import java.time.LocalDate;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.*;
import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.MAX_CHAR_DISTANCE;

public class DateTimeParsingUtils {


    private static final String ISO_LOCAL_TIME_PATTERN = "HH:MM:SS";

    private static final String ISO_LOCAL_DATE_PATTERN = "YYYY-MM-DD";

    private static final String ISO_LOCAL_DATE_TIME_PATTERN = String.format("%sT%s", ISO_LOCAL_DATE_PATTERN, ISO_LOCAL_TIME_PATTERN);

    private static final int ISO_LOCAL_TIME_LENGTH = ISO_LOCAL_TIME_PATTERN.length();

    private static final int ISO_LOCAL_DATE_LENGTH = ISO_LOCAL_DATE_PATTERN.length();

    private static final int ISO_LOCAL_DATE_TIME_LENGTH = ISO_LOCAL_DATE_TIME_PATTERN.length();


    /**
     * returns how close the input was to HH:MM
     *
     * @param input
     * @return
     */
    private static double getDistanceToLocalTimeWithoutSeconds(CharSequence input) {
        return getDistanceToLocalTimeWithSeconds(input == null ? null : input + ":00");
    }

    /**
     * returns how close the input was to HH:MM:SS
     *
     * @param input
     * @return
     */
    private static double getDistanceToLocalTimeWithSeconds(CharSequence input) {

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }

        final double base = H_NOT_NULL;

        long distance = 0;

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i);

            //format HH:MM:SS
            //let's simplify and only allow 00:00:00 to 19:59:59

            if (i == 0) {
                distance += distanceToRange(c, '0', '1');
            } else if (i == 1 || i == 4 || i == 7) {
                distance += distanceToRange(c, '0', '9');
            } else if (i == 2 || i == 5) {
                distance += distanceToChar(c, ':');
            } else if (i == 3 || i == 6) {
                distance += distanceToRange(c, '0', '5');
            } else {
                distance += MAX_CHAR_DISTANCE;
            }
        }

        if (input.length() < ISO_LOCAL_TIME_LENGTH) {
            //too short
            distance += (MAX_CHAR_DISTANCE * (ISO_LOCAL_TIME_LENGTH - input.length()));
        }

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ((1d - base) / (distance + 1));
    }

    /**
     * returns a value that represents how close is the value to the format YYYY-MM-DD
     *
     * @param input
     * @return
     */
    public static double getDistanceToISOLocalDateTime(CharSequence input) {

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }

        final double base = H_NOT_NULL;

        long distance = 0;

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i);

            // TODO: The code below can be refactored with class DateFormatClassReplacement
            //format YYYY-MM-DDT
            if (i >= 0 && i <= 3) {
                //any Y value is ok
                distance += distanceToDigit(c);
            } else if (i == 4 || i == 7) {
                distance += distanceToChar(c, '-');
            } else if (i == 5) {
                //let's simplify and only allow 01 to 09 for MM
                distance += distanceToChar(c, '0');
            } else if (i == 6) {
                distance += distanceToRange(c, '1', '9');
            } else if (i == 8) {
                //let's simplify and only allow 01 to 28
                distance += distanceToRange(c, '0', '2');
            } else if (i == 9) {
                distance += distanceToRange(c, '1', '8');
            } else if (i == 10) {
                // The letter 'T'. Parsing is case insensitive.
                distance += Math.min(distanceToChar(c, 'T'), distanceToChar(c, 't'));
            } else if (i == 11) {
                distance += distanceToRange(c, '0', '1');
            } else if (i == 12 || i == 15 || i == 18) {
                distance += distanceToRange(c, '0', '9');
            } else if (i == 13 || i == 16) {
                distance += distanceToChar(c, ':');
            } else if (i == 14 || i == 17) {
                distance += distanceToRange(c, '0', '5');
            } else {
                distance += MAX_CHAR_DISTANCE;
            }
        }

        if (input.length() < ISO_LOCAL_DATE_TIME_LENGTH) {
            //too short
            distance += (MAX_CHAR_DISTANCE * (ISO_LOCAL_DATE_TIME_LENGTH - input.length()));
        }

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ((1d - base) / (distance + 1));
    }

    /**
     * returns how close the input was to HH:MM or HH:MM:SS
     *
     * @param input
     * @return
     */
    public static double getDistanceToISOLocalTime(CharSequence input) {
        return Math.min(
                getDistanceToLocalTimeWithoutSeconds(input),
                getDistanceToLocalTimeWithSeconds(input));
    }

    /**
     * returns a value that represents how close is the value to the format YYYY-MM-DD
     *
     * @param input
     * @return
     */
    public static double getDistanceToISOLocalDate(CharSequence input) {

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }

        try {
            LocalDate.parse(input);
            /*
                due to the simplification later on, still must make
                sure to get a 1 if no exception is thrown
             */
            return 1d;
        } catch (RuntimeException e) {
            //nothing to do
        }

        final double base = H_NOT_NULL;

        long distance = 0;

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i);

            // TODO: The code below can be refactored with class DateFormatClassReplacement
            //format YYYY-MM-DD
            if (i >= 0 && i <= 3) {
                //any Y value is ok
                distance += distanceToDigit(c);
            } else if (i == 4 || i == 7) {
                distance += distanceToChar(c, '-');
            } else if (i == 5) {
                //let's simplify and only allow 01 to 09 for MM
                distance += distanceToChar(c, '0');
            } else if (i == 6) {
                distance += distanceToRange(c, '1', '9');
            } else if (i == 8) {
                //let's simplify and only allow 01 to 28
                distance += distanceToRange(c, '0', '2');
            } else if (i == 9) {
                distance += distanceToRange(c, '1', '8');
            } else {
                distance += MAX_CHAR_DISTANCE;
            }
        }

        if (input.length() < ISO_LOCAL_DATE_LENGTH) {
            //too short
            distance += (MAX_CHAR_DISTANCE * (ISO_LOCAL_DATE_LENGTH - input.length()));
        }

        //recall h in [0,1] where the highest the distance the closer to 0
        return base + ((1d - base) / (distance + 1));
    }

}
