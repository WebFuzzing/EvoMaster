package org.evomaster.client.java.instrumentation.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPAddressValidator {
    private static final int MAX_BYTE = 128;

    private static final int IPV4_MAX_OCTET_VALUE = 255;

    private static final int MAX_UNSIGNED_SHORT = 0xffff;

    private static final int BASE_16 = 16;

    private static final String IPV4_REGEX =
            "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$";

    // Max number of hex groups (separated by :) in an IPV6 address
    private static final int IPV6_MAX_HEX_GROUPS = 8;

    // Max hex digits in each IPv6 group
    private static final int IPV6_MAX_HEX_DIGITS_PER_GROUP = 4;

    private static final IPAddressValidator VALIDATOR = new IPAddressValidator();

    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d{1,3}");
    private static final Pattern ID_CHECK_PATTERN = Pattern.compile("[^\\s/%]+");

    public static boolean isValidInet4Address(final String inet4Address) {
        // verify that address conforms to generic IPv4 format
        Pattern pattern = Pattern.compile(IPV4_REGEX);
        final Matcher matcher = pattern.matcher(inet4Address);

        if (matcher.matches()) {
            final int count = matcher.groupCount();

            if (count != 4) {
                return false;
            }

            for (int i = 0; i < count; i++) {
                String segment = matcher.group(i + 1);

                int ipSegment = 0;
                try {
                    ipSegment = Integer.parseInt(segment);
                } catch (final NumberFormatException e) {
                    return false;
                }

                if (ipSegment > IPV4_MAX_OCTET_VALUE) {
                    return false;
                }

                if (segment.length() > 1 && segment.startsWith("0")) {
                    return false;
                }
            }

        }

        return true;
    }

    public static boolean isValidInet6Address(String inet6Address) {
        String[] parts;
        // remove prefix size. This will appear after the zone id (if any)
        parts = inet6Address.split("/", -1);
        if (parts.length > 2) {
            return false; // can only have one prefix specifier
        }
        if (parts.length == 2) {
            if (!DIGITS_PATTERN.matcher(parts[1]).matches()) {
                return false; // not a valid number
            }
            final int bits = Integer.parseInt(parts[1]); // cannot fail because of RE check
            if (bits < 0 || bits > MAX_BYTE) {
                return false; // out of range
            }
        }
        // remove zone-id
        parts = parts[0].split("%", -1);
        if (parts.length > 2) {
            return false;
        }
        // The id syntax is implementation independent, but it presumably cannot allow:
        // whitespace, '/' or '%'
        if (parts.length == 2 && !ID_CHECK_PATTERN.matcher(parts[1]).matches()) {
            return false; // invalid id
        }
        inet6Address = parts[0];
        final boolean containsCompressedZeroes = inet6Address.contains("::");
        if (containsCompressedZeroes && inet6Address.indexOf("::") != inet6Address.lastIndexOf("::")) {
            return false;
        }
        if (inet6Address.startsWith(":") && !inet6Address.startsWith("::")
                || inet6Address.endsWith(":") && !inet6Address.endsWith("::")) {
            return false;
        }
        String[] octets = inet6Address.split(":");
        if (containsCompressedZeroes) {
            final List<String> octetList = new ArrayList<>(Arrays.asList(octets));
            if (inet6Address.endsWith("::")) {
                // String.split() drops ending empty segments
                octetList.add("");
            } else if (inet6Address.startsWith("::") && !octetList.isEmpty()) {
                octetList.remove(0);
            }
            octets = octetList.toArray(new String[0]);
        }
        if (octets.length > IPV6_MAX_HEX_GROUPS) {
            return false;
        }
        int validOctets = 0;
        int emptyOctets = 0; // consecutive empty chunks
        for (int index = 0; index < octets.length; index++) {
            final String octet = octets[index];
            if (octet.isEmpty()) {
                emptyOctets++;
                if (emptyOctets > 1) {
                    return false;
                }
            } else {
                emptyOctets = 0;
                // Is last chunk an IPv4 address?
                if (index == octets.length - 1 && octet.contains(".")) {
                    if (!isValidInet4Address(octet)) {
                        return false;
                    }
                    validOctets += 2;
                    continue;
                }
                if (octet.length() > IPV6_MAX_HEX_DIGITS_PER_GROUP) {
                    return false;
                }
                int octetInt = 0;
                try {
                    octetInt = Integer.parseInt(octet, BASE_16);
                } catch (final NumberFormatException e) {
                    return false;
                }
                if (octetInt < 0 || octetInt > MAX_UNSIGNED_SHORT) {
                    return false;
                }
            }
            validOctets++;
        }
        if (validOctets > IPV6_MAX_HEX_GROUPS || validOctets < IPV6_MAX_HEX_GROUPS && !containsCompressedZeroes) {
            return false;
        }
        return true;
    }

}
