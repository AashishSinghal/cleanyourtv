package com.gathin.photodream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class AlbumTest {
    private static final String PAGE = "junk[[\"AF1QipAAA\",[\"https://lh3.googleusercontent.com/pw/AP1Gc-one\",676,1247],1],"
            + "[\"AF1QipBBB\",[\"https://lh3.googleusercontent.com/pw/AP1Gc_two\",1358,644]],"
            + "[\"AF1QipAAA\",[\"https://lh3.googleusercontent.com/pw/AP1Gc-one\",1,1]],"       // the same id again
            + "[\"AF1QipCCC\",[\"https://example.com/pw/evil\",1,1]],"                          // not Google's picture host
            + "[\"notanid\",[\"https://lh3.googleusercontent.com/pw/AP1Gc-three\",1,1]]]";      // not a photo id

    @Test public void parseKeepsGooglePicturesOncePerIdInPageOrder() {
        List<String[]> items = Album.parse(PAGE);
        assertEquals(2, items.size());
        assertEquals("AF1QipAAA", items.get(0)[0]);
        assertEquals("https://lh3.googleusercontent.com/pw/AP1Gc-one", items.get(0)[1]);
        assertEquals("AF1QipBBB", items.get(1)[0]);
    }

    @Test public void parseOfAChangedPageFindsNothing() {
        assertTrue(Album.parse("<html>nothing here</html>").isEmpty());
        assertTrue(Album.parse("").isEmpty());
    }

    @Test public void parseStopsAtTheFileLimit() {
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < Media.MAX_FILES + 20; i++) {
            many.append("[\"AF1Qip").append(i).append("\",[\"https://lh3.googleusercontent.com/pw/x").append(i).append("\",1,1]],");
        }
        assertEquals(Media.MAX_FILES, Album.parse(many.toString()).size());
    }

    @Test public void fileNamesAreStableDistinctAndAcceptedAsPictures() {
        String a = Album.fileName("AF1QipAAA");
        assertEquals(a, Album.fileName("AF1QipAAA"));
        assertNotEquals(a, Album.fileName("AF1QipBBB"));
        assertTrue(a.startsWith(Album.PREFIX));
        assertTrue(a, Media.image(a));
        assertEquals(Album.PREFIX.length() + 12 + 4, a.length());
    }

    @Test public void onlyGooglePhotosShareLinksAreFetched() {
        assertTrue(Album.validLink("https://photos.app.goo.gl/ZfostHCcEHAkcL6Q7"));
        assertTrue(Album.validLink("https://photos.google.com/share/AF1QipN_x-y?key=Vzly"));
        assertFalse(Album.validLink("http://photos.app.goo.gl/ZfostHCcEHAkcL6Q7"));
        assertFalse(Album.validLink("https://example.com/share/x"));
        assertFalse(Album.validLink("https://photos.google.com.evil.com/share/x"));
        assertFalse(Album.validLink("https://photos.google.com/share/x y"));
        assertFalse(Album.validLink("https://photos.google.com/share/x\nhttps://evil.com"));
        assertFalse(Album.validLink(""));
        assertFalse(Album.validLink(null));
    }

    @Test public void theProviderAcceptsTheAlbumFile() {
        assertEquals("album.txt", Media.path(java.util.Collections.singletonList("album.txt")));
    }
}
