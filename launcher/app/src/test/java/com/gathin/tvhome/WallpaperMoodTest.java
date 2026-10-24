package com.gathin.tvhome;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/** Plain JVM tests for wallpaper_moods: the time of day's group, my-tv's rain and fog, and the pictures it picks from. */
public class WallpaperMoodTest {
    private static final int RISE = 7 * 60 + 20, SET = 18 * 60 + 20;

    private static String at(int hour, int minute) {
        return WallpaperMood.timeOfDay(hour * 60 + minute, RISE, SET);
    }

    @Test
    public void timeOfDayFollowsSunriseNoonAndDusk() {
        assertEquals(WallpaperMood.NIGHT, at(0, 30));
        assertEquals(WallpaperMood.NIGHT, at(7, 19));
        assertEquals(WallpaperMood.MORNING, at(7, 20));
        assertEquals(WallpaperMood.MORNING, at(11, 59));
        assertEquals(WallpaperMood.EVENING, at(12, 0));
        assertEquals(WallpaperMood.EVENING, at(18, 49));   // half an hour after sunset is still evening
        assertEquals(WallpaperMood.NIGHT, at(18, 50));
        assertEquals(WallpaperMood.NIGHT, at(23, 59));
    }

    @Test
    public void sunComesFromTheConfiguredPlaceOrTheDefaults() {
        LocalDate day = LocalDate.of(2026, 10, 22);
        ZoneId zone = ZoneId.of("Europe/London");
        assertArrayEquals(Sun.riseAndSet(day, 51.51, -0.13, zone), WallpaperMood.sun(day, "51.51", " -0.13 ", zone));
        int[] defaults = {WallpaperMood.DEFAULT_SUNRISE, WallpaperMood.DEFAULT_SUNSET};
        assertArrayEquals(defaults, WallpaperMood.sun(day, "", "", zone));
        assertArrayEquals(defaults, WallpaperMood.sun(day, "51.51", "", zone));
        assertArrayEquals(defaults, WallpaperMood.sun(day, "north", "-0.13", zone));
        // the sun does not set at the pole in June: the defaults
        assertArrayEquals(defaults, WallpaperMood.sun(LocalDate.of(2026, 6, 21), "89.9", "0", ZoneId.of("UTC")));
    }

    private static List<File> files(String... names) {
        List<File> out = new ArrayList<>();
        for (String name : names) out.add(new File("/walls", name));
        return out;
    }

    private static List<String> names(List<File> files) {
        List<String> out = new ArrayList<>();
        for (File f : files) out.add(f.getName());
        return out;
    }

    @Test
    public void selectKeepsTheCollectionsOrderAndFallsBackToAll() {
        List<File> all = files("a.jpg", "b.jpg", "c.jpg", "d.jpg");
        Map<String, Set<String>> groups = new HashMap<>();
        groups.put(WallpaperMood.NIGHT, new HashSet<>(Arrays.asList("d.jpg", "a.jpg")));
        groups.put(WallpaperMood.MORNING, new HashSet<>(Collections.singletonList("gone.jpg")));
        assertEquals(Arrays.asList("a.jpg", "d.jpg"), names(WallpaperMood.select(all, groups, WallpaperMood.NIGHT)));
        assertEquals(all, WallpaperMood.select(all, groups, WallpaperMood.MORNING));   // none of its pictures here
        assertEquals(all, WallpaperMood.select(all, groups, WallpaperMood.EVENING));   // no group at all
        assertEquals(all, WallpaperMood.select(all, Collections.emptyMap(), WallpaperMood.NIGHT));
    }

    // my-tv: the weather

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);
    private static final int[] DEFAULTS = {WallpaperMood.DEFAULT_SUNRISE, WallpaperMood.DEFAULT_SUNSET};

    @Test
    public void skyIsRainOrFogOnly() {
        assertEquals(WallpaperMood.FOG, WallpaperMood.sky(45));
        assertEquals(WallpaperMood.FOG, WallpaperMood.sky(48));
        for (int code : new int[]{51, 55, 57, 61, 63, 67, 80, 82, 95, 99}) {
            assertEquals("code " + code, WallpaperMood.RAIN, WallpaperMood.sky(code));
        }
        for (int code : new int[]{0, 1, 2, 3, 71, 75, 85}) {
            assertNull("code " + code, WallpaperMood.sky(code));
        }
    }

    @Test
    public void selectTakesTheFirstMoodWithPictures() {
        List<File> all = files("a.jpg", "b.jpg", "c.jpg", "d.jpg");
        Map<String, Set<String>> groups = new HashMap<>();
        groups.put(WallpaperMood.RAIN, new HashSet<>(Collections.singletonList("c.jpg")));
        groups.put(WallpaperMood.NIGHT, new HashSet<>(Arrays.asList("d.jpg", "a.jpg")));
        groups.put(WallpaperMood.FOG, new HashSet<>(Collections.singletonList("gone.jpg")));
        assertEquals(Collections.singletonList("c.jpg"), names(WallpaperMood.select(all, groups, WallpaperMood.RAIN, WallpaperMood.NIGHT)));
        assertEquals(Arrays.asList("a.jpg", "d.jpg"), names(WallpaperMood.select(all, groups, null, WallpaperMood.NIGHT)));
        // a fog group whose pictures are not in the collection falls through to the time of day
        assertEquals(Arrays.asList("a.jpg", "d.jpg"), names(WallpaperMood.select(all, groups, WallpaperMood.FOG, WallpaperMood.NIGHT)));
        assertEquals(all, WallpaperMood.select(all, groups, null, WallpaperMood.MORNING));
    }

    /** Today and tomorrow, hourly, in a place at UTC-7; it drizzles from 14:00 to 16:59, fog at 06:00 tomorrow. */
    private static Weather weather(long fetchedAt) {
        List<Weather.Hour> hours = new ArrayList<>();
        for (int i = 0; i < 48; i++) {
            LocalDateTime t = TODAY.atStartOfDay().plusHours(i);
            int code = i >= 14 && i < 17 ? 53 : i == 30 ? 45 : 2;
            hours.add(new Weather.Hour(t, 15, code, t.getHour() >= 7 && t.getHour() < 18));
        }
        List<LocalDateTime[]> sun = Arrays.asList(
                new LocalDateTime[]{TODAY.atTime(7, 20), TODAY.atTime(18, 20)},
                new LocalDateTime[]{TODAY.plusDays(1).atTime(7, 21), TODAY.plusDays(1).atTime(18, 19)});
        return new Weather("", fetchedAt, -7 * 3600, 15, 14, 2, true, hours,
                Arrays.asList(TODAY, TODAY.plusDays(1)), Arrays.asList(new double[]{18, 12}, new double[]{19, 11}), sun);
    }

    /** That local time at the place, in epoch milliseconds. */
    private static long local(LocalDateTime t) {
        return t.toInstant(ZoneOffset.ofHours(-7)).toEpochMilli();
    }

    @Test
    public void theForecastsSkyAndSunComeFirst() {
        Weather w = weather(local(TODAY.atTime(13, 0)));
        assertArrayEquals(new String[]{WallpaperMood.RAIN, WallpaperMood.EVENING},
                WallpaperMood.at(w, local(TODAY.atTime(14, 30)), ZoneOffset.UTC, DEFAULTS));
        // its sunset (18:20), not the default 18:30
        assertArrayEquals(new String[]{null, WallpaperMood.NIGHT},
                WallpaperMood.at(w, local(TODAY.atTime(18, 55)), ZoneOffset.UTC, DEFAULTS));
        assertArrayEquals(new String[]{WallpaperMood.FOG, WallpaperMood.NIGHT},
                WallpaperMood.at(weather(local(TODAY.plusDays(1).atTime(5, 0))), local(TODAY.plusDays(1).atTime(6, 10)),
                        ZoneOffset.UTC, DEFAULTS));
    }

    @Test
    public void anOldForecastStillGivesTheSunButNotTheSky() {
        Weather w = weather(local(TODAY.atTime(6, 0)));
        assertArrayEquals(new String[]{null, WallpaperMood.EVENING},
                WallpaperMood.at(w, local(TODAY.atTime(14, 30)), ZoneOffset.UTC, DEFAULTS));
        assertArrayEquals(new String[]{null, WallpaperMood.MORNING},
                WallpaperMood.at(w, local(TODAY.plusDays(1).atTime(7, 21)), ZoneOffset.UTC, DEFAULTS));
    }

    @Test
    public void withoutAForecastTheTvClockAndTheGivenSunDecide() {
        long sixFiftyNine = TODAY.atTime(6, 59).toInstant(ZoneOffset.UTC).toEpochMilli();
        assertArrayEquals(new String[]{null, WallpaperMood.NIGHT}, WallpaperMood.at(null, sixFiftyNine, ZoneOffset.UTC, DEFAULTS));
        assertArrayEquals(new String[]{null, WallpaperMood.MORNING},
                WallpaperMood.at(null, sixFiftyNine + 60_000, ZoneOffset.UTC, DEFAULTS));
        // config.xml's place rises later: still night at 07:30
        assertArrayEquals(new String[]{null, WallpaperMood.NIGHT},
                WallpaperMood.at(null, sixFiftyNine + 31 * 60_000, ZoneOffset.UTC, new int[]{8 * 60, 17 * 60}));
    }
}
