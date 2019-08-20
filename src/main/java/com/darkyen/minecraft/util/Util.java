package com.darkyen.minecraft.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.NumberConversions;
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

    @Nullable
    public static World getWorld(@Nullable Location loc) {
        if (loc == null) {
            return null;
        }
        try {
            if (!loc.isWorldLoaded()) {
                return null;
            }
        } catch (Throwable ignored) {
            // isWorldLoaded is not available on servers < 1.14
        }
        try {
            return loc.getWorld();
        } catch (Throwable ignored) {
            // (>= 1.14) If the world gets unloaded between check above and now, this could throw, but it is unlikely.
            return null;
        }
    }

    public static double distance2(double aX, double aY, double aZ, double bX, double bY, double bZ) {
        return NumberConversions.square(aX - bX) + NumberConversions.square(aY - bY) + NumberConversions.square(aZ - bZ);
    }

    @FunctionalInterface
    public interface SearchLocationConsumer {
        /** @return true to stop, false to continue. */
        boolean acceptable(int x, int y, int z);
    }

    private static boolean searchLocationPlate(int originX, int originY, int originZ, int radius, SearchLocationConsumer consumer) {
        for (int x = originX - radius; x <= originX + radius; x++) {
            for (int z = originZ - radius; z <= originZ + radius; z++) {
                if (consumer.acceptable(x, originY, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean searchLocationRing(int originX, int originY, int originZ, int radius, SearchLocationConsumer consumer) {
        for (int i = -radius; i < radius; i++) {
            if (consumer.acceptable(originX + i, originY + radius, originZ)
                    || consumer.acceptable(originX + radius, originY - i, originZ)
                    || consumer.acceptable(originX - i, originY - radius, originZ)
                    || consumer.acceptable(originX - radius, originY + i, originZ)) {
                return true;
            }
        }
        return false;
    }

    /** Search for a location which is acceptable in boxes around given coordinates.
     * @return true if found, false if ran out of radius
     */
    public static boolean searchLocation(World world, int x, int y, int z, int radius, SearchLocationConsumer consumer) {
        // Degenerate case
        if (consumer.acceptable(x, y, z)) {
            return true;
        }
        final int maxHeight = world.getMaxHeight();
        for (int r = 1; r <= radius; r++) {
            int highY = y + r;
            if (highY <= maxHeight) {
                if (searchLocationPlate(x, highY, z, r, consumer)) {
                    return true;
                }
                highY--;
            } else {
                highY = maxHeight;
            }

            int lowY = y - r;
            if (lowY >= 0) {
                if (searchLocationPlate(x, lowY, z, r, consumer)) {
                    return true;
                }
                lowY--;
            } else {
                lowY = 0;
            }

            for (int ringY = highY; ringY >= lowY ; ringY--) {
                if (searchLocationRing(x, ringY, z, r, consumer)) {
                    return true;
                }
            }
        }
        return false;
    }
}
