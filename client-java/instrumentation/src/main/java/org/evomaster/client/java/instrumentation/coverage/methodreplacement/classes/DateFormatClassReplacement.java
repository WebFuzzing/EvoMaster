package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DateTimeParsingUtils;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
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

    public static final String YYYY_MM_DD = "YYYY-MM-DD";
    public static final String YYYY_MM_DD_HH_SS = "YYYY-MM-DD HH:SS";

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
                case YYYY_MM_DD_HH_SS:
                    specializationInfo = new StringSpecializationInfo(StringSpecialization.DATE_YYYY_MM_DD_HH_SS, pattern);
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
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
            return res;
        } catch (ParseException e) {
            final double h;
            switch (pattern) {
                case YYYY_MM_DD:
                    h = DateTimeParsingUtils.getDistanceToISOLocalDate(input);
                    break;
                case YYYY_MM_DD_HH_SS:
                    h = DateTimeParsingUtils.getDistanceToDateTime(input);
                    break;
                default:
                    h = DateTimeParsingUtils.getDistanceToDateTimePattern(input, pattern);
            }
            ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(h, 1));
            throw e;
        }

    }

    @Replacement(type = ReplacementType.EXCEPTION)
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
                ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(1, 0));
                return res;
            } catch (ParseException e) {
                // we do not have much guidance since we cannot access any pattern
                ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.EXCEPTION, new Truthness(0, 1));
                throw e;
            }
        }
    }


}
