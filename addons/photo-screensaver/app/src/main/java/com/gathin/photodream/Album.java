package com.gathin.photodream;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional: the pictures of a Google Photos shared album. When album.txt holds the album's "anyone with the link"
 * address, each time the screensaver starts (at most once an hour) the page is read, new pictures are downloaded at
 * screen size and pictures taken out of the album are removed. Only files named gp-*.jpg are touched, so pictures
 * put on the TV any other way stay.
 *
 * This reads the public share page, not an API: Google closed its Photos API to other people's albums in 2025.
 * shortcut: the page is Google's, not a contract, and it carries the first pictures only (at most MAX_FILES are
 * used anyway); when the page changes shape nothing is deleted and the pictures already here keep showing.
 */
final class Album {
    static final String PREFIX = "gp-";
    private static final String SYNCED = "album_synced_";
    private static final long EVERY_MS = 60 * 60 * 1000L;
    private static final int TIMEOUT_MS = 20000;
    private static final long PAGE_MAX = 4L * 1024 * 1024;
    /** Fits inside the screen, keeps the shape: a portrait comes down 1080 high. */
    private static final String SIZE = "=w1920-h1080";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36";
    private static final Pattern LINK =
            Pattern.compile("https://(photos\\.google\\.com/share/|photos\\.app\\.goo\\.gl/)[\\w./?=&%~-]+");
    private static final Pattern ITEM = Pattern.compile(
            "\\[\"(AF1Qip[\\w-]+)\",\\[\"(https://lh3\\.googleusercontent\\.com/pw/[\\w-]+)\"");
    private static final AtomicBoolean RUNNING = new AtomicBoolean();

    private Album() { }

    /** Only the two Google Photos share addresses are fetched, whatever album.txt says. */
    static boolean validLink(String link) {
        return link != null && link.length() < 400 && LINK.matcher(link).matches();
    }

    /** {id, url} of each picture on the page, in page order, each id once, at most Media.MAX_FILES. */
    static List<String[]> parse(String page) {
        List<String[]> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Matcher m = ITEM.matcher(page);
        while (m.find() && items.size() < Media.MAX_FILES) {
            if (seen.add(m.group(1))) items.add(new String[]{m.group(1), m.group(2)});
        }
        return items;
    }

    /** A name the same id always gets, that Media accepts: gp-<12 hex>.jpg. */
    static String fileName(String id) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(id.getBytes(StandardCharsets.UTF_8));
            StringBuilder name = new StringBuilder(PREFIX);
            for (int i = 0; i < 6; i++) name.append(String.format("%02x", digest[i]));
            return name.append(".jpg").toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Syncs in the background when album.txt holds a valid link and the last good sync of that link is over an hour
     * old. {@code done} runs on the main thread afterwards, also after a failed try, so the slideshow lists the
     * folder again.
     */
    static void start(Context context, Runnable done) {
        File dir = context.getExternalFilesDir(null);
        String link = dir == null ? null : read(new File(dir, Media.ALBUM));
        SharedPreferences prefs = context.getSharedPreferences("photo_dream", Context.MODE_PRIVATE);
        String key = SYNCED + (link == null ? 0 : link.hashCode());   // a new link syncs at once
        if (!validLink(link) || System.currentTimeMillis() - prefs.getLong(key, 0) < EVERY_MS
                || !RUNNING.compareAndSet(false, true)) return;
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                sync(new File(dir, Media.PICTURES), link);
                prefs.edit().putLong(key, System.currentTimeMillis()).apply();
            } catch (IOException offlineOrChanged) {
                // keep what is on the TV; the next start tries again
            } finally {
                RUNNING.set(false);
                main.post(done);
            }
        }, "album-sync").start();
    }

    private static void sync(File pictures, String link) throws IOException {
        ByteArrayOutputStream page = new ByteArrayOutputStream();
        fetch(link, page, PAGE_MAX, "text/html");
        List<String[]> items = parse(new String(page.toByteArray(), StandardCharsets.UTF_8));
        // A page that lists nothing is a changed page, not an emptied album: do not clear the screensaver for it.
        if (items.isEmpty()) throw new IOException("no pictures on the page");
        if (!pictures.isDirectory() && !pictures.mkdirs()) throw new IOException("no folder");
        Set<String> keep = new HashSet<>();
        for (String[] item : items) {
            String name = fileName(item[0]);
            keep.add(name);
            File file = new File(pictures, name);
            // shortcut: an edited picture keeps its id and is not fetched again; remove the file to get it again.
            if (file.length() == 0) download(item[1] + SIZE, file);
        }
        File[] gone = pictures.listFiles((d, n) -> n.startsWith(PREFIX) && !keep.contains(n));
        if (gone != null) for (File f : gone) f.delete();
    }

    private static void download(String url, File file) throws IOException {
        File part = new File(file.getParentFile(), "." + file.getName() + ".part");   // a dot name, which Media skips
        try (FileOutputStream out = new FileOutputStream(part)) {
            fetch(url, out, Media.IMAGE_MAX, "image/");
        } catch (IOException e) {
            part.delete();
            throw e;
        }
        if (!part.renameTo(file)) {
            part.delete();
            throw new IOException("rename failed");
        }
    }

    private static void fetch(String url, OutputStream out, long max, String type) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try {
            String reply = connection.getContentType();
            if (connection.getResponseCode() != 200 || reply == null || !reply.startsWith(type)) {
                throw new IOException("unexpected reply");
            }
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[16 * 1024];
                long total = 0;
                for (int n; (n = in.read(buffer)) != -1; ) {
                    total += n;
                    if (total > max) throw new IOException("reply too large");
                    out.write(buffer, 0, n);
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String read(File file) {
        try {
            return file.isFile() && file.length() < 512
                    ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.US_ASCII).trim() : null;
        } catch (IOException e) {
            return null;
        }
    }
}
