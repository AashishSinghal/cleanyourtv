package com.gathin.tvhome;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RemoteKeyChoicesTest {

    private static RemoteKeyChoices.Choice choice(String id, String label, String pkg) {
        return new RemoteKeyChoices.Choice(id, label, pkg);
    }

    @Test public void aButtonIsStoredByItsScancodeWhenItHasOne() {
        assertEquals("scan:398", RemoteKeyChoices.id(183, 398));
        assertEquals("scan:398", RemoteKeyChoices.id(0, 398));
        assertEquals("183", RemoteKeyChoices.id(183, 0));
    }

    @Test public void idsAreScancodesOrUnprotectedKeycodes() {
        assertTrue(RemoteKeyChoices.validId("scan:398"));
        assertTrue(RemoteKeyChoices.validId("183"));
        assertFalse(RemoteKeyChoices.validId("scan:0"));
        assertFalse(RemoteKeyChoices.validId("scan:abc"));
        assertFalse(RemoteKeyChoices.validId("0"));
        assertFalse(RemoteKeyChoices.validId("-5"));
        assertFalse(RemoteKeyChoices.validId("KEYCODE_PROG_RED"));
        assertFalse(RemoteKeyChoices.validId(null));
    }

    @Test public void aProtectedKeycodeIsNotAValidId() {
        assertFalse(RemoteKeyChoices.validId("4"));    // BACK
        assertFalse(RemoteKeyChoices.validId("23"));   // DPAD_CENTER
        assertFalse(RemoteKeyChoices.validId("24"));   // VOLUME_UP
        assertFalse(RemoteKeyChoices.validId("26"));   // POWER
        assertFalse(RemoteKeyChoices.validId("3"));    // HOME
    }

    @Test public void saveAndReadRoundTrip() {
        Map<String, RemoteKeyChoices.Choice> in = new LinkedHashMap<>();
        in.put("scan:398", choice("scan:398", "PROG_RED (scancode 398)", "org.smarttube.stable"));
        in.put("190", choice("190", "BUTTON_3", "com.spotify.tv.android"));
        Map<String, RemoteKeyChoices.Choice> out = RemoteKeyChoices.parse(RemoteKeyChoices.encode(in.values()));
        assertEquals(2, out.size());
        assertEquals("org.smarttube.stable", out.get("scan:398").pkg);
        assertEquals("PROG_RED (scancode 398)", out.get("scan:398").label);
        assertEquals("com.spotify.tv.android", out.get("190").pkg);
    }

    @Test public void badLinesAreSkippedOnRead() {
        String text = "scan:398|Red|org.smarttube.stable\n"
                + "4|Back|com.evil.app\n"                  // a protected keycode
                + "scan:399|Green|not a package\n"         // not a package name
                + "nonsense\n"
                + "|x|a.b\n"
                + "scan:400|Yellow|com.crunchyroll.crunchyroid";
        Map<String, RemoteKeyChoices.Choice> out = RemoteKeyChoices.parse(text);
        assertEquals(2, out.size());
        assertTrue(out.containsKey("scan:398"));
        assertTrue(out.containsKey("scan:400"));
        assertNull(out.get("4"));
    }

    @Test public void emptyOrMissingTextReadsAsNothing() {
        assertTrue(RemoteKeyChoices.parse("").isEmpty());
        assertTrue(RemoteKeyChoices.parse(null).isEmpty());
    }

    @Test public void labelsAreCleanedAndShortened() {
        assertEquals("PROG_RED (scancode 398)", RemoteKeyChoices.cleanLabel("PROG_RED (scancode 398)"));
        assertEquals("abc", RemoteKeyChoices.cleanLabel("a|b\nc"));
        assertEquals(40, RemoteKeyChoices.cleanLabel(new String(new char[100]).replace((char) 0, (char) 120)).length());
        assertEquals("", RemoteKeyChoices.cleanLabel(null));
    }

    @Test public void aLabelCannotBreakTheLineFormat() {
        RemoteKeyChoices.Choice c = choice("scan:398", "a|b\nscan:399|x|a.b", "org.smarttube.stable");
        Map<String, RemoteKeyChoices.Choice> out = RemoteKeyChoices.parse(RemoteKeyChoices.encode(Collections.singletonList(c)));
        assertEquals(1, out.size());
        assertEquals("org.smarttube.stable", out.get("scan:398").pkg);
    }

    @Test public void theOwnersChoicesComeAfterTheThemeSoTheyWin() {
        List<String> theme = Arrays.asList("scan:398:org.smarttube.stable", "scan:399:com.spotify.tv.android");
        List<String> merged = RemoteKeyChoices.merge(theme,
                Collections.singletonList(choice("scan:398", "Red", "com.crunchyroll.crunchyroid")));
        assertEquals(3, merged.size());
        assertEquals("scan:398:com.crunchyroll.crunchyroid", merged.get(2));
        Map<Integer, String> scancodes = RemoteKeys.parseAppScancodes(merged);
        assertEquals("com.crunchyroll.crunchyroid", scancodes.get(398));     // the choice replaced the theme's app
        assertEquals("com.spotify.tv.android", scancodes.get(399));           // an untouched theme button stays
    }

    @Test public void keycodeChoicesMergeIntoTheKeycodeList() {
        List<String> merged = RemoteKeyChoices.merge(Collections.singletonList("190:a.b"),
                Collections.singletonList(choice("183", "PROG_RED", "c.d")));
        Map<Integer, String> keys = RemoteKeys.parseAppKeys(merged, name -> 0);
        assertEquals("a.b", keys.get(190));
        assertEquals("c.d", keys.get(183));
    }

    @Test public void mergingNothingLeavesTheThemeAlone() {
        List<String> theme = Arrays.asList("scan:398:org.smarttube.stable");
        assertEquals(theme, RemoteKeyChoices.merge(theme, Collections.<RemoteKeyChoices.Choice>emptyList()));
    }
}
