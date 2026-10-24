package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The remote buttons the owner assigned on the TV ("Remote buttons" in Launcher settings). They live in their own
 * preferences file, apart from the launcher's other on-TV choices, so pushing a theme does not wipe them. The key
 * services add them after the theme's app_keys, so a choice made here wins over the theme for the same button.
 *
 * A button is stored by its scancode when it has one ("scan:398"), otherwise by its keycode ("183"); that is the
 * same id the app_keys list uses. One choice per line: "id|label|package".
 */
final class RemoteKeyChoices {
    static final String PREFS = "remote_keys";
    private static final String ENTRIES = "entries";
    private static final int MAX_LABEL = 40;
    private static final AtomicInteger REVISION = new AtomicInteger();

    private RemoteKeyChoices() { }

    /** One assigned button: its id, a name for the screen, and the app it opens. */
    static final class Choice {
        final String id;
        final String label;
        final String pkg;

        Choice(String id, String label, String pkg) {
            this.id = id;
            this.label = label;
            this.pkg = pkg;
        }
    }

    /** Moves whenever a choice is saved or removed, so the key services know to read their lists again. */
    static int revision() {
        return REVISION.get();
    }

    /** The id of a button: its scancode when it has one, else its keycode. */
    static String id(int keyCode, int scanCode) {
        return scanCode > 0 ? "scan:" + scanCode : String.valueOf(keyCode);
    }

    /** True for "scan:<n>" or a keycode that is not protected. */
    static boolean validId(String id) {
        if (id == null) return false;
        if (id.startsWith("scan:")) return id.substring(5).matches("[1-9][0-9]{0,5}");
        return id.matches("[1-9][0-9]{0,5}") && !RemoteKeyPolicy.isProtected(Integer.parseInt(id));
    }

    static String cleanLabel(String label) {
        String text = label == null ? "" : label.replaceAll("[^A-Za-z0-9_ ()]", "").trim();
        return text.length() > MAX_LABEL ? text.substring(0, MAX_LABEL).trim() : text;
    }

    /** The saved text as choices by id; lines with a bad id, a protected keycode or a bad package are skipped. */
    static Map<String, Choice> parse(String text) {
        Map<String, Choice> choices = new LinkedHashMap<>();
        if (text == null) return choices;
        for (String line : text.split("\n")) {
            String[] parts = line.split("\\|", 3);
            if (parts.length != 3) continue;
            String id = parts[0].trim();
            String pkg = parts[2].trim();
            if (!validId(id) || !LibraryApps.validPackage(pkg)) continue;
            choices.put(id, new Choice(id, cleanLabel(parts[1]), pkg));
        }
        return choices;
    }

    static String encode(Collection<Choice> choices) {
        StringBuilder text = new StringBuilder();
        for (Choice c : choices) {
            if (text.length() > 0) text.append('\n');
            text.append(c.id).append('|').append(cleanLabel(c.label)).append('|').append(c.pkg);
        }
        return text.toString();
    }

    /** The theme's app_keys entries, then the owner's choices as entries of the same list: the last one wins. */
    static List<String> merge(List<String> base, Collection<Choice> choices) {
        List<String> merged = new ArrayList<>(base);
        for (Choice c : choices) merged.add(c.id + ":" + c.pkg);
        return merged;
    }

    static Map<String, Choice> load(Context ctx) {
        return parse(prefs(ctx).getString(ENTRIES, ""));
    }

    /** Saves the app for a button, replacing its earlier choice. False when the id or the package is not valid. */
    static boolean put(Context ctx, String id, String label, String pkg) {
        if (!validId(id) || !LibraryApps.validPackage(pkg)) return false;
        Map<String, Choice> choices = load(ctx);
        choices.put(id, new Choice(id, cleanLabel(label), pkg));
        return save(ctx, choices);
    }

    static boolean remove(Context ctx, String id) {
        Map<String, Choice> choices = load(ctx);
        if (choices.remove(id) == null) return false;
        return save(ctx, choices);
    }

    private static boolean save(Context ctx, Map<String, Choice> choices) {
        boolean saved = prefs(ctx).edit().putString(ENTRIES, encode(choices.values())).commit();
        if (saved) REVISION.incrementAndGet();
        return saved;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
