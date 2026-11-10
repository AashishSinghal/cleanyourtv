package com.gathin.photodream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PlaybackOptionsTest {
    private static List<File> files(String... names) {
        List<File> list = new ArrayList<>();
        for (String name : names) list.add(new File(name));
        return list;
    }

    @Test public void secondsStepUpAndWrapAround() {
        assertEquals(10, Media.nextSeconds(5));
        assertEquals(30, Media.nextSeconds(20));
        assertEquals(300, Media.nextSeconds(120));
        assertEquals(5, Media.nextSeconds(300));     // after the last, back to the first
        assertEquals(30, Media.nextSeconds(25));     // a value from seconds.txt that is not a step
        assertEquals(10, Media.nextSeconds(7));
        assertEquals(5, Media.nextSeconds(3600));
    }

    @Test public void everyStepIsAcceptedAsTheSecondsPerPicture() {
        int seconds = 5;
        for (int i = 0; i < 20; i++) {
            assertEquals(seconds, Media.seconds(String.valueOf(seconds)));
            seconds = Media.nextSeconds(seconds);
        }
    }

    @Test public void randomOtherNeverRepeatsThePreviousPicture() {
        List<File> list = files("a.jpg", "b.jpg", "c.jpg");
        Random random = new Random(7);
        String previous = "a.jpg";
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            File pick = Media.randomOther(list, previous, random);
            assertNotEquals(previous, pick.getName());
            seen.add(pick.getName());
            previous = pick.getName();
        }
        assertEquals(3, seen.size());                // all of them come up
    }

    @Test public void randomOtherOfOnePictureIsThatPictureAndOfNoneIsNull() {
        assertEquals("only.jpg", Media.randomOther(files("only.jpg"), "only.jpg", new Random(1)).getName());
        assertNull(Media.randomOther(Collections.<File>emptyList(), "", new Random(1)));
    }

    @Test public void randomOtherWithAnUnknownPreviousStillPicksOne() {
        assertTrue(Media.randomOther(files("a.jpg", "b.jpg"), "gone.jpg", new Random(3)) != null);
    }
}
