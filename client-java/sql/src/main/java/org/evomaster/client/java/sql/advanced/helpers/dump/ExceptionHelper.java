package org.evomaster.client.java.sql.advanced.helpers.dump;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExceptionHelper {

    public static String exceptionToString(Exception e) {
        return Optional.ofNullable(e.getMessage())
            .map(message -> message.concat("\n\n")).orElse("")
            .concat(Stream.of(e.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n")));
    }
}
