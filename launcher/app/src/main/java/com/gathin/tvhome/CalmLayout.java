package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Locale;

/**
 * my-tv home_layout "calm": look up to glance, look down to act.
 *
 * The top half is a header that never takes focus: the time on the left, the weather mirrored on the right, in
 * the same type sizes. Everything the remote reaches sits just above the dock: the focused app's name as a small
 * caption with "Last opened" beside it, and the system buttons on the same line at the right. Sizes follow the
 * mockup at 1920x1080 on a density-2 TV: 128 px margins (64dp), 132/34/28/22 px type (66/17/14/11sp).
 *
 * Applied once at the end of MainActivity.onCreate over the standard layout, by moving and restyling its views;
 * MainActivity asks it where the caption and the buttons go each time the shelf moves.
 */
final class CalmLayout {
    static final int INK = 0xFFFFFFFF, INK_2 = 0xBDFFFFFF, INK_3 = 0x8AFFFFFF;
    /** The mockup's night colour, rgb(3,5,10), for the scrim. */
    static final int NIGHT = 0x03050A;

    private final Activity activity;
    private final FrameLayout root;
    private final float density;
    private final LinearLayout hero;
    private final View topButtons;

    CalmLayout(Activity activity, FrameLayout root, float density) {
        this.activity = activity;
        this.root = root;
        this.density = density;
        hero = activity.findViewById(R.id.hero);
        topButtons = activity.findViewById(R.id.topButtons);
    }

    void apply() {
        header();
        caption();
        moveButtons();
        buttons = new QuietButtons(activity, (LinearLayout) topButtons, density);
        buttons.style();
        weather();
        scrim();
        tiles();
    }

    // ---- the tiles: a white ring, a lift and a shadow; the next row fades into the bottom edge -----

    private void tiles() {
        AppAdapter.calmScale = 1.08f;
        ((GlassView) activity.findViewById(R.id.glass)).setRing(true);
        View grid = activity.findViewById(R.id.grid);
        grid.setVerticalFadingEdgeEnabled(true);
        grid.setFadingEdgeLength(dp(42));
    }

    /** Left/Right on a system button: the next shown button that way; the row's ends keep the focus. */
    boolean moveAcross(View from, boolean right) {
        LinearLayout row = (LinearLayout) topButtons;
        int at = row.indexOfChild(from);
        for (int i = at + (right ? 1 : -1); i >= 0 && i < row.getChildCount(); i += right ? 1 : -1) {
            View b = row.getChildAt(i);
            if (b.getVisibility() == View.VISIBLE && b.isFocusable()) {
                b.requestFocus();
                return true;
            }
        }
        return true;
    }

    /** Up from the dock: the first system button that is shown (Inputs on a TV with inputs). */
    boolean focusButtons() {
        LinearLayout row = (LinearLayout) topButtons;
        for (int i = 0; i < row.getChildCount(); i++) {
            View b = row.getChildAt(i);
            if (b.getVisibility() == View.VISIBLE && b.isFocusable()) return b.requestFocus();
        }
        return false;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    private static void size(TextView t, float sp) {
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    }

    private void font(TextView t, int font) {
        t.setTypeface(activity.getResources().getFont(font));
    }

    // ---- the glance header ------------------------------------------------------------------------

    /** Greeting as an eyebrow, the time as the display line, the date under it instead of beside it. */
    private void header() {
        LinearLayout left = activity.findViewById(R.id.leftBlock);
        // The standard layout's 56dp sides plus the grid's 10dp padding put the tiles at 66dp: line the
        // header and the tiles up on the mockup's 64dp.
        LinearLayout main = (LinearLayout) ((View) left.getParent()).getParent();
        main.setPaddingRelative(dp(54), dp(36), dp(54), main.getPaddingBottom());
        ((ViewGroup.MarginLayoutParams) left.getLayoutParams()).setMarginStart(dp(10));

        TextView greeting = activity.findViewById(R.id.greeting);
        size(greeting, 11);
        greeting.setTextColor(INK_3);
        greeting.setLetterSpacing(0.26f);
        ((ViewGroup.MarginLayoutParams) greeting.getLayoutParams()).setMarginStart(0);

        TextClock clock = activity.findViewById(R.id.clock);
        size(clock, 66);
        font(clock, R.font.outfit_extralight);
        clock.setLetterSpacing(-0.02f);
        LinearLayout row = (LinearLayout) clock.getParent();
        ((ViewGroup.MarginLayoutParams) row.getLayoutParams()).topMargin = dp(5);

        TextClock ampm = activity.findViewById(R.id.ampm);
        size(ampm, 14);
        font(ampm, R.font.outfit_regular);
        ampm.setTextColor(INK_2);
        ampm.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams ampmLp = (LinearLayout.LayoutParams) ampm.getLayoutParams();
        ampmLp.gravity = -1;          // back to the row's baseline alignment
        ampmLp.bottomMargin = 0;
        ampmLp.setMarginStart(dp(8));

        // The row holds clock, am/pm, the divider and the date column: drop the divider, move the date under.
        View divider = row.getChildAt(2);
        divider.setVisibility(View.GONE);
        View dateColumn = row.getChildAt(3);
        row.removeView(dateColumn);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateLp.topMargin = dp(8);
        left.addView(dateColumn, left.indexOfChild(row) + 1, dateLp);
        TextClock date = activity.findViewById(R.id.date);
        size(date, 14);
        font(date, R.font.outfit_regular);
        date.setTextColor(INK_2);
        date.setAllCaps(false);
        date.setLetterSpacing(0f);
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM");
        date.setFormat12Hour(pattern);
        date.setFormat24Hour(pattern);
        TextView shamsi = activity.findViewById(R.id.shamsiDate);
        size(shamsi, 14);
        shamsi.setTextColor(INK_2);
    }

    // ---- the act band: caption and buttons above the dock ------------------------------------------

    /** The shelf's name as a one-line caption, "Last opened" beside it on the same baseline. */
    private void caption() {
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setBaselineAligned(true);
        ((ViewGroup.MarginLayoutParams) hero.getLayoutParams()).setMarginStart(dp(64));
        TextView name = activity.findViewById(R.id.heroName);
        size(name, 17);
        font(name, R.font.outfit_medium);
        name.setLetterSpacing(0.005f);
        TextView meta = activity.findViewById(R.id.heroMeta);
        size(meta, 14);
        meta.setTextColor(INK_3);
        LinearLayout.LayoutParams metaLp = (LinearLayout.LayoutParams) meta.getLayoutParams();
        metaLp.topMargin = 0;
        metaLp.setMarginStart(dp(9));
        activity.findViewById(R.id.heroDots).setVisibility(View.GONE);
    }

    /** The system buttons leave the top bar for the act band, at its right end. */
    private void moveButtons() {
        View left = activity.findViewById(R.id.leftBlock);
        RelativeLayout.LayoutParams leftLp = (RelativeLayout.LayoutParams) left.getLayoutParams();
        leftLp.removeRule(RelativeLayout.START_OF);
        leftLp.removeRule(RelativeLayout.LEFT_OF);
        ((ViewGroup) topButtons.getParent()).removeView(topButtons);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT);
        // The root clips each child to its bounds (the top bar's containers did not): room around the row for a
        // focused button's 1.06x lift, taken back out of the margin so the row's edge stays on the 64dp line.
        int room = dp(6);
        topButtons.setPadding(room, room, room, room);
        lp.rightMargin = dp(64) - room;
        root.addView(topButtons, lp);
        // Until the dock is laid out, keep them out of sight rather than in the top corner.
        topButtons.setAlpha(0f);
    }

    // ---- the system buttons: quiet round icons, a label only on focus --------------------------------

    private QuietButtons buttons;

    /** The calm layout's system buttons (QuietButtons), for MainActivity.updateWifiLabel. */
    QuietButtons buttons() {
        return buttons;
    }

    /** The mockup's motion: 240 ms, cubic-bezier(.2,.8,.2,1). */
    static final int MOTION_MS = 240;
    static final android.view.animation.Interpolator EASE = new android.view.animation.PathInterpolator(0.2f, 0.8f, 0.2f, 1f);

    /** The quick settings panel rises from the Settings button, lined up with the button row's right edge. */
    void anchorPanel(android.content.Intent panel, View settings) {
        int[] at = new int[2];
        settings.getLocationOnScreen(at);
        panel.putExtra(QuickSettingsActivity.EXTRA_ABOVE_Y, at[1]);
        panel.putExtra(QuickSettingsActivity.EXTRA_END_MARGIN, dp(64));
    }

    /** The middle of the caption line: the mockup's 64 px row, 26 px above the tiles. */
    private float headCenter(float dockTopInRoot) {
        return dockTopInRoot - dp(13) - dp(16);
    }

    /** Where the caption goes for a dock whose tiles start at dockTopInRoot. */
    float captionY(float dockTopInRoot) {
        return headCenter(dockTopInRoot) - hero.getHeight() / 2f;
    }

    /**
     * While the grid scrolls: back on the caption line above where the dock's tiles are now, or, with the dock
     * scrolled out of view, moved by the scroll like the standard focus highlight.
     */
    void followDock(androidx.recyclerview.widget.RecyclerView grid, int dy) {
        androidx.recyclerview.widget.RecyclerView.ViewHolder dock = grid.findViewHolderForAdapterPosition(0);
        View card = dock == null ? null : dock.itemView.findViewById(R.id.card);
        if (card == null || card.getHeight() == 0) {
            topButtons.setTranslationY(topButtons.getTranslationY() - dy);
            return;
        }
        android.graphics.Rect r = new android.graphics.Rect(0, 0, card.getWidth(), card.getHeight());
        root.offsetDescendantRectToMyCoords(card, r);
        topButtons.setY(headCenter(r.top) - topButtons.getHeight() / 2f);
    }

    private boolean buttonsShown;

    /** Puts the system buttons on the caption's line and shows them with it. */
    void placeButtons(float dockTopInRoot) {
        topButtons.setY(headCenter(dockTopInRoot) - topButtons.getHeight() / 2f);
        if (buttonsShown) return;
        buttonsShown = true;
        topButtons.animate().cancel();
        topButtons.animate().alpha(1f).setDuration(200).start();
    }

    /** With the caption: browsing the rows below the dock hides the act band, as the standard shelf hides. */
    void hideButtons() {
        if (!buttonsShown) return;
        buttonsShown = false;
        topButtons.animate().cancel();
        topButtons.animate().alpha(0f).setDuration(160).start();
    }

    // ---- the weather, mirrored on the right --------------------------------------------------------

    /** Fixed at the top right, always shown: the place as its eyebrow, the hours under a hairline. */
    private void weather() {
        LinearLayout panel = activity.findViewById(R.id.weather);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) panel.getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.RIGHT;
        lp.topMargin = dp(36);
        lp.rightMargin = dp(64);
        lp.setMarginEnd(dp(64));
        panel.setAlpha(1f);

        TextView place = activity.findViewById(R.id.weatherPlace);
        panel.removeView(place);
        panel.addView(place, 0);
        LinearLayout.LayoutParams placeLp = (LinearLayout.LayoutParams) place.getLayoutParams();
        placeLp.topMargin = 0;
        size(place, 11);
        place.setTextColor(INK_3);
        place.setLetterSpacing(0.26f);
        place.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(INK_3));

        TextView temp = activity.findViewById(R.id.weatherTemp);
        View nowRow = (View) temp.getParent();
        ((LinearLayout.LayoutParams) nowRow.getLayoutParams()).topMargin = dp(5);
        ImageView icon = activity.findViewById(R.id.weatherIcon);
        LinearLayout.LayoutParams iconLp = (LinearLayout.LayoutParams) icon.getLayoutParams();
        iconLp.width = iconLp.height = dp(36);
        iconLp.setMarginEnd(dp(13));
        size(temp, 66);
        temp.setLetterSpacing(-0.02f);

        TextView condition = activity.findViewById(R.id.weatherCondition);
        size(condition, 14);
        condition.setTextColor(INK_2);
        ((LinearLayout.LayoutParams) condition.getLayoutParams()).topMargin = dp(8);
        TextView details = activity.findViewById(R.id.weatherDetails);
        size(details, 14);
        details.setTextColor(INK_3);
        details.setLetterSpacing(0f);
        ((LinearLayout.LayoutParams) details.getLayoutParams()).topMargin = dp(3);

        LinearLayout hours = activity.findViewById(R.id.weatherHours);
        hours.setBackground(new HairlineTop(0x24FFFFFF, Math.max(1, Math.round(density / 2))));
        hours.setPadding(0, dp(9), 0, 0);
        ((LinearLayout.LayoutParams) hours.getLayoutParams()).topMargin = dp(13);

        // After the whole pass: the weather is laid out before the header's block, which has no height yet then.
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> fitHeaderToWeather(panel));
    }

    /**
     * The weather floats over the screen, so the header (the clock's block) grows to reach its bottom: the grid
     * then starts below it, and rows scrolled up slide under the header's edge as in the standard layout instead
     * of over the forecast. The dock stays put, since its parking padding is measured from the grid's bottom.
     */
    private void fitHeaderToWeather(View panel) {
        View left = activity.findViewById(R.id.leftBlock);
        int need = 0;
        if (panel.getVisibility() == View.VISIBLE && panel.getHeight() > 0 && left.getHeight() > 0) {
            android.graphics.Rect p = new android.graphics.Rect(0, 0, panel.getWidth(), panel.getHeight());
            root.offsetDescendantRectToMyCoords(panel, p);
            android.graphics.Rect h = new android.graphics.Rect(0, 0, left.getWidth(), left.getHeight());
            root.offsetDescendantRectToMyCoords(left, h);
            need = p.bottom - h.top;
        }
        if (left.getMinimumHeight() != need) {
            left.setMinimumHeight(need);
            left.requestLayout();
        }
    }

    /** A 1 px line along the top edge, the mockup's hairline above the hours. */
    private static final class HairlineTop extends android.graphics.drawable.Drawable {
        private final Paint paint = new Paint();
        private final int height;

        HairlineTop(int color, int height) {
            paint.setColor(color);
            this.height = height;
        }

        @Override public void draw(Canvas canvas) {
            canvas.drawRect(getBounds().left, getBounds().top, getBounds().right, getBounds().top + height, paint);
        }

        @Override public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override public void setColorFilter(android.graphics.ColorFilter filter) {
            paint.setColorFilter(filter);
        }

        @Override public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    // ---- the scrim -------------------------------------------------------------------------------

    /**
     * The mockup's two gradients in night blue: from 50% at the top to clear by 35% of the height, clear again
     * from 46% to 70% (the band), then 94% at the bottom behind the dock. One small bitmap stretched over the
     * screen, like the standard shelf's scrim.
     */
    private void scrim() {
        View scrim = activity.findViewById(R.id.heroScrim);
        int h = 270;
        Bitmap bmp = Bitmap.createBitmap(4, h, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{alpha(0.50f), alpha(0f), alpha(0f), alpha(0.70f), alpha(0.94f)},
                new float[]{0f, 0.352f, 0.463f, 0.704f, 1f}, Shader.TileMode.CLAMP));
        new Canvas(bmp).drawRect(0, 0, 4, h, paint);
        BitmapDrawable drawable = new BitmapDrawable(activity.getResources(), bmp);
        drawable.setGravity(Gravity.FILL);
        scrim.setBackground(drawable);
        scrim.setAlpha(1f);
        scrim.setVisibility(View.VISIBLE);
    }

    private static int alpha(float a) {
        return (Math.round(a * 255) << 24) | NIGHT;
    }
}
