package com.darkyen.minecraft.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 */
public final class Util {

    private Util() {}


    private static final Pattern TIME_SANITIZER = Pattern.compile("[^a-zA-Z0-9]");
    public static long parseTimeMs(@Nullable String time, long defaultMs, Logger log) {
        if (time == null) {
            return defaultMs;
        }
        final String sanitized = TIME_SANITIZER.matcher(time).replaceAll("");
        if ("never".equalsIgnoreCase(sanitized)) {
            return Long.MAX_VALUE;
        }
        int firstLetterIndex = 0;
        while (firstLetterIndex < sanitized.length() && Character.isDigit(sanitized.charAt(firstLetterIndex))) {
            firstLetterIndex++;
        }
        if (firstLetterIndex >= sanitized.length()) {
            log.log(Level.WARNING, "Time \""+time+"\" is missing an unit");
            return defaultMs;
        }
        if (firstLetterIndex == 0) {
            log.log(Level.WARNING, "Time \""+time+"\" is missing an amount");
            return defaultMs;
        }
        final long amount;
        try {
            amount = Long.parseLong(sanitized.substring(0, firstLetterIndex));
        } catch (NumberFormatException e) {
            log.log(Level.WARNING, "Time \""+time+"\" is invalid");
            return defaultMs;
        }

        final TimeUnit unit;
        switch (sanitized.charAt(firstLetterIndex)) {
            case 's':
                unit = TimeUnit.SECONDS;
                break;
            case 'm':
                unit = TimeUnit.MINUTES;
                break;
            case 'h':
                unit = TimeUnit.HOURS;
                break;
            case 'd':
                unit = TimeUnit.DAYS;
                break;
            default:
                log.log(Level.WARNING, "Time \""+time+"\" has invalid unit");
                return defaultMs;
        }

        return unit.toMillis(amount);
    }

    @Nullable
    public static UUID parseUUID(@NotNull String from) {
        String[] components = from.split("-");
        if (components.length != 5)
            return null;

        long mostSigBits = Long.parseLong(components[0], 16);
        mostSigBits <<= 16;
        mostSigBits |= Long.parseLong(components[1], 16);
        mostSigBits <<= 16;
        mostSigBits |= Long.parseLong(components[2], 16);

        long leastSigBits = Long.parseLong(components[3], 16);
        leastSigBits <<= 48;
        leastSigBits |= Long.parseLong(components[4], 16);

        return new UUID(mostSigBits, leastSigBits);
    }

}
