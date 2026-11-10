package com.gathin.photodream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PortraitTest {
    @Test public void onlyAPictureTallerThanWideIsPortrait() {
        assertTrue(Media.portrait(3472, 4640));
        assertFalse(Media.portrait(4640, 3472));
        assertFalse(Media.portrait(1000, 1000));
    }

    @Test public void aPortraitFitsTheScreenHeightAndIsCentred() {
        // 3:4 on a 1920x1080 screen: 810x1080, 555 px in from the left.
        assertArrayEquals(new int[]{555, 0, 810, 1080}, Media.fit(1920, 2560, 1920, 1080));
    }

    @Test public void aWidePictureFitsTheScreenWidthAndIsCentredVertically() {
        assertArrayEquals(new int[]{0, 270, 1920, 540}, Media.fit(3840, 1080, 1920, 1080));
    }

    @Test public void aPictureTheSizeOfTheScreenFillsIt() {
        assertArrayEquals(new int[]{0, 0, 1920, 1080}, Media.fit(1920, 1080, 1920, 1080));
    }
}
