package org.evomaster.client.java.instrumentation.shared;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPAddressValidator {
    private static final String IPV4_REGEX =
            "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$";

    private static final Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);

    private static final String IPV6_REGEX = "([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]){1,4}|"
            + "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)$|"
            + "^::(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$|"
            + "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$|"
            + "^((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?):(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$";
    private static final Pattern IPV6_PATTERN = Pattern.compile(IPV6_REGEX);

    public static boolean isValidInet4Address(final String ipv4Address) {
        final Matcher matcher = IPV4_PATTERN.matcher(ipv4Address);

        return matcher.matches();
    }

    public static boolean isValidInet6Address(String ipv6Address) {
        // https://www.ibm.com/docs/en/ts4500-tape-library?topic=functionality-ipv4-ipv6-address-formats
        final Matcher matcher = IPV6_PATTERN.matcher(ipv6Address);
        return matcher.matches();
    }

}
