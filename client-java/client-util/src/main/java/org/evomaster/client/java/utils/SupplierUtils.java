package org.evomaster.client.java.utils;

import java.util.function.Supplier;

public class SupplierUtils {

    /**
     * What {@code supplier} gives, or null if it fails. For values that are worth having but
     * whose loss should not fail the caller.
     *
     * @param warning what to warn about on a failure. Warned only once, as the same call can be
     *                repeated often; the reason of each failure goes to the debug log
     */
    public static <T> T getOrNullOnFailure(Supplier<T> supplier, String warning) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            SimpleLogger.uniqueWarn(warning);
            if (SimpleLogger.isDebug()) {
                SimpleLogger.debug(warning + ": " + e.getMessage());
            }
            return null;
        }
    }
}
