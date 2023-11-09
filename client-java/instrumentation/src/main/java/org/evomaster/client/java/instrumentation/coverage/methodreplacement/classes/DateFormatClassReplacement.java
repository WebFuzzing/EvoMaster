package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class DateFormatClassReplacement implements MethodReplacementClass {

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";

    @Override
    public Class<?> getTargetClass() {
        return DateFormat.class;
    }

    private static Date parseSimpleDateFormat(SimpleDateFormat caller, String input, String idTemplate) throws ParseException {

        final String pattern = caller.toPattern();
        if (ExecutionTracer.isTaintInput(input)) {
            final StringSpecializationInfo specializationInfo;
            switch (pattern) {
                case YYYY_MM_DD:
                    specializationInfo = new StringSpecializationInfo(StringSpecialization.DATE_YYYY_MM_DD, pattern);
                    break;
                case YYYY_MM_DD_HH_MM:
                    specializationInfo = new StringSpecializationInfo(StringSpecialization.DATE_YYYY_MM_DD_HH_MM, pattern);
                    break;
                default:
                    specializationInfo = new StringSpecializationInfo(StringSpecialization.DATE_FORMAT_PATTERN, pattern);
            }
            ExecutionTracer.addStringSpecialization(input,
                    specializationInfo);
        }

        if (idTemplate == null) {
            return caller.parse(input);
        }

        try {
            Date res = caller.parse(input);
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                    new Truthness(1, DistanceHelper.H_NOT_NULL));
            return res;
        } catch (ParseException e) {
            final double h;
            switch (pattern) {
                case YYYY_MM_DD:
                    h = DateTimeParsingUtils.getHeuristicToISOLocalDateParsing(input);
                    break;
                case YYYY_MM_DD_HH_MM:
                    h = DateTimeParsingUtils.getHeuristicToDateTimeParsing(input);
                    break;
                default:
                    h = DateTimeParsingUtils.getHeuristicToDateTimePatternParsing(input, pattern);
            }
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                    new Truthness(h, 1));
            throw e;
        }

    }

    @Replacement(type = ReplacementType.EXCEPTION, category = ReplacementCategory.BASE)
    public static Date parse(DateFormat caller, String input, String idTemplate) throws ParseException {
        Objects.requireNonNull(caller);

        if (caller instanceof SimpleDateFormat) {
            SimpleDateFormat sdf = (SimpleDateFormat) caller;
            return parseSimpleDateFormat(sdf, input, idTemplate);
        } else {

            if (ExecutionTracer.isTaintInput(input)) {
                ExecutionTracer.addStringSpecialization(input,
                        new StringSpecializationInfo(StringSpecialization.DATE_FORMAT_UNKNOWN_PATTERN, null));
            }

            if (idTemplate == null) {
                return caller.parse(input);
            }

            try {
                Date res = caller.parse(input);
                ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                        new Truthness(1, DistanceHelper.H_NOT_NULL));
                return res;
            } catch (ParseException e) {
                // we do not have much guidance since we cannot access any pattern
                double h = input == null ? DistanceHelper.H_REACHED_BUT_NULL : DistanceHelper.H_NOT_NULL;
                ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION,
                        new Truthness(h, 1));
                throw e;
            }
        }
    }


}
