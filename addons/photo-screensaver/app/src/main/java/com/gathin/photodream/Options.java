package com.gathin.photodream;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/** The choices in SettingsActivity. All off by default: then it is the plain full-screen slideshow. */
final class Options {
    static final String CLOCK = "clock";
    static final String DATES = "photo_dates";
    static final String SKY = "sky";
    static final String PRINTS = "prints";
    static final String WEATHER = "weather";
    static final String SECONDS = "seconds";
    static final String SHUFFLE = "shuffle";
    static final String STILL = "still";
    static final String CROP = "crop_portraits";

    final boolean clock;
    final boolean dates;
    final boolean sky;
    final boolean prints;
    final boolean weather;
    /** Random order instead of by file name. */
    final boolean shuffle;
    /** No zoom or drift. */
    final boolean still;
    /** A portrait fills the screen (cropped) instead of being shown whole over a blurred backdrop. */
    final boolean cropPortraits;

    private Options(SharedPreferences p) {
        clock = p.getBoolean(CLOCK, false);
        dates = p.getBoolean(DATES, false);
        sky = p.getBoolean(SKY, false);
        prints = p.getBoolean(PRINTS, false);
        weather = p.getBoolean(WEATHER, false);
        shuffle = p.getBoolean(SHUFFLE, false);
        still = p.getBoolean(STILL, false);
        cropPortraits = p.getBoolean(CROP, false);
    }

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("options", Context.MODE_PRIVATE);
    }

    static Options load(Context context) {
        return new Options(preferences(context));
    }

    /** Seconds per picture: the choice made in settings, else seconds.txt (sync.sh), else 20. */
    static int seconds(Context context) {
        int chosen = preferences(context).getInt(SECONDS, 0);
        if (chosen >= 5 && chosen <= 3600) return chosen;
        File dir = context.getExternalFilesDir(null);
        return Media.seconds(Photos.read(dir == null ? null : new File(dir, Media.SECONDS)));
    }
}
