package com.gathin.tvhome;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * wallpaper_moods: which group of a rotating wallpaper collection fits the time of day. Morning from sunrise to noon,
 * evening until half an hour after sunset, night the rest. The sun is the one at config.xml's latitude and longitude,
 * worked out offline by {@link Sun} as the sky does, or 07:00 and 18:30 without a place. A group that is missing, or
 * has none of the collection's pictures, falls back to the whole collection.
 * <p>
 * my-tv adds the weather: rain or fog in the forecast's current hour comes before the time of day, and the forecast's
 * sunrise and sunset come before config.xml's place. A rain or fog group without pictures falls back to the time of
 * day's.
 */
final class WallpaperMood {
    static final String MORNING = "morning", EVENING = "evening", NIGHT = "night", RAIN = "rain", FOG = "fog";
    static final String[] ALL = {MORNING, EVENING, NIGHT, RAIN, FOG};
    /** Sunrise and sunset in minutes of the day, without a place (or where the sun does not rise or set that day). */
    static final int DEFAULT_SUNRISE = 7 * 60, DEFAULT_SUNSET = 18 * 60 + 30;
    private static final int NOON = 12 * 60;
    /** Evening lasts this long after sunset, while the sky still glows. */
    private static final int DUSK_MINUTES = 30;

    private WallpaperMood() {
    }

    /** The mood at this minute of the day, for a sunrise and sunset in minutes of the day. */
    static String timeOfDay(int minute, int sunrise, int sunset) {
        if (minute >= sunrise && minute < NOON) return MORNING;
        if (minute >= NOON && minute < sunset + DUSK_MINUTES) return EVENING;
        return NIGHT;
    }

    /** That day's {sunrise, sunset} at config.xml's latitude and longitude; the defaults when either is unusable. */
    static int[] sun(LocalDate day, String latitude, String longitude, ZoneId zone) {
        int[] times = null;
        try {
            if (!latitude.trim().isEmpty() && !longitude.trim().isEmpty()) {
                times = Sun.riseAndSet(day, Double.parseDouble(latitude.trim()), Double.parseDouble(longitude.trim()), zone);
            }
        } catch (NumberFormatException ignored) {
            // not a number: the defaults
        }
        return times != null ? times : new int[]{DEFAULT_SUNRISE, DEFAULT_SUNSET};
    }

    /** my-tv: RAIN for drizzle, rain, showers and thunderstorms, FOG for fog (WMO codes), null for any other sky. */
    static String sky(int code) {
        if (code == 45 || code == 48) return FOG;
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82) || code >= 95) return RAIN;
        return null;
    }

    /**
     * my-tv: the moods for this moment, the most specific first: the sky's (or null), then the time of day's. With a
     * forecast, the time is its place's and the sun that day's from the forecast; without one, the TV's own time in
     * {@code zone} and {@code sun}. A forecast too old to show still gives the day's sun, but not the sky.
     */
    static String[] at(Weather weather, long nowMs, ZoneId zone, int[] sun) {
        int sunrise = sun[0], sunset = sun[1];
        String sky = null;
        LocalDateTime now;
        if (weather != null) {
            now = weather.localNow(nowMs);
            LocalDateTime[] times = weather.sunOn(now.toLocalDate());
            if (times != null) {
                sunrise = minuteOfDay(times[0]);
                sunset = minuteOfDay(times[1]);
            }
            if (weather.freshAt(nowMs)) sky = sky(weather.codeAt(now));
        } else {
            now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMs), zone);
        }
        return new String[]{sky, timeOfDay(minuteOfDay(now), sunrise, sunset)};
    }

    private static int minuteOfDay(LocalDateTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    /**
     * The pictures of the first mood whose group has any of them, in the collection's order; all of them when none
     * has. A null mood is skipped.
     */
    static List<File> select(List<File> all, Map<String, Set<String>> groups, String... moods) {
        for (String mood : moods) {
            Set<String> names = mood == null ? null : groups.get(mood);
            if (names == null) continue;
            List<File> group = new ArrayList<>();
            for (File file : all) if (names.contains(file.getName())) group.add(file);
            if (!group.isEmpty()) return group;
        }
        return all;
    }
}
