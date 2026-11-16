package com.example.timeservlet;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts the user's timezone into a strict Java time zone. */
public final class TimezoneResolver {
    // A plus in the query string may be decoded as a space by the servlet container.
    private static final Pattern UTC_OFFSET = Pattern.compile(
            "^(UTC|GMT)([+\\- ])(\\d{1,2})(?::(\\d{2}))?$",
            Pattern.CASE_INSENSITIVE
    );

    private TimezoneResolver() {
    }

    public static ResolvedTimezone resolve(String input) {
        if (input == null) {
            return new ResolvedTimezone(ZoneOffset.UTC, "UTC");
        }

        String text = input.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Timezone is empty");
        }

        if (text.equalsIgnoreCase("UTC")) {
            return new ResolvedTimezone(ZoneOffset.UTC, "UTC");
        }
        if (text.equalsIgnoreCase("GMT")) {
            return new ResolvedTimezone(ZoneOffset.UTC, "GMT");
        }

        Matcher matcher = UTC_OFFSET.matcher(text);
        if (matcher.matches()) {
            String prefix = matcher.group(1).toUpperCase(Locale.ROOT);
            String signText = matcher.group(2);
            int hours = Integer.parseInt(matcher.group(3));
            int minutes = matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4));

            if (hours > 18 || minutes > 59 || (hours == 18 && minutes != 0)) {
                throw new IllegalArgumentException("Timezone offset is out of range");
            }

            int sign = signText.equals("-") ? -1 : 1;
            ZoneOffset offset = ZoneOffset.ofTotalSeconds(sign * (hours * 3600 + minutes * 60));
            String label = prefix + (sign == -1 ? "-" : "+") + matcher.group(3);
            if (matcher.group(4) != null) {
                label += ":" + matcher.group(4);
            }
            return new ResolvedTimezone(offset, label);
        }

        try {
            // This also supports names like Europe/Kyiv, Asia/Tokyo, America/New_York.
            return new ResolvedTimezone(ZoneId.of(text), text);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Unknown timezone: " + text, e);
        }
    }

    public record ResolvedTimezone(ZoneId zoneId, String label) {
    }
}
