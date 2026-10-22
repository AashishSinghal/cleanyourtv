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
 * my-tv home_layout "calm" (design/v4): look up to glance, look down to act.
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
        styleButtons();
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

    /** The mockup's motion: 240 ms, cubic-bezier(.2,.8,.2,1). */
    static final int MOTION_MS = 240;
    static final android.view.animation.Interpolator EASE = new android.view.animation.PathInterpolator(0.2f, 0.8f, 0.2f, 1f);
    private static final int INK_FOCUSED = 0xFF0B0D14, INK_BUTTON = 0xE0FFFFFF;

    /** Each button's label, shown only while it has focus ("Wi-Fi · Connected"). */
    private final java.util.Map<TextView, CharSequence> labels = new java.util.HashMap<>();
    private DotIcon networkIcon;
    private int networkIconRes;

    /**
     * 32dp round icons on a faint fill. Focused: white, dark ink, 1.06x, and the label slides out to the left (the
     * row is anchored at its right end and each button animates its own width, see resize).
     */
    private void styleButtons() {
        LinearLayout row = (LinearLayout) topButtons;
        android.content.res.ColorStateList ink = new android.content.res.ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}}, new int[]{INK_FOCUSED, INK_BUTTON});
        boolean first = true;
        for (int i = 0; i < row.getChildCount(); i++) {
            if (!(row.getChildAt(i) instanceof TextView)) continue;
            TextView b = (TextView) row.getChildAt(i);
            CharSequence label = b.getText() != null && b.getText().length() > 0 ? b.getText() : b.getContentDescription();
            labels.put(b, label);
            b.setContentDescription(label);
            b.setText(null);
            b.setBackground(buttonBackground());
            b.setTextColor(ink);
            b.setCompoundDrawableTintList(ink);
            size(b, 14);
            font(b, R.font.outfit_medium);
            b.setSingleLine(true);
            // The standard Wi-Fi pill caps its width for a network name; the calm label needs its full length.
            b.setMaxWidth(Integer.MAX_VALUE);
            b.setEllipsize(null);
            android.graphics.drawable.Drawable icon = b.getCompoundDrawablesRelative()[0];
            if (icon != null) {
                icon = icon.mutate();
                icon.setBounds(0, 0, dp(15), dp(15));
                b.setCompoundDrawablesRelative(icon, null, null, null);
            }
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) b.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.height = dp(32);
            lp.setMarginStart(first ? 0 : dp(7));
            first = false;
            b.setMinWidth(dp(32));
            b.setMinimumWidth(dp(32));
            unfocused(b);
            View.OnFocusChangeListener before = b.getOnFocusChangeListener();
            b.setOnFocusChangeListener((v, has) -> {
                if (before != null) before.onFocusChange(v, has);
                if (has) focused(b);
                else unfocused(b);
            });
        }
    }

    private android.graphics.drawable.Drawable buttonBackground() {
        android.graphics.drawable.GradientDrawable on = new android.graphics.drawable.GradientDrawable();
        on.setColor(0xFFFFFFFF);
        on.setCornerRadius(dp(16));
        android.graphics.drawable.GradientDrawable off = new android.graphics.drawable.GradientDrawable();
        off.setColor(0x17FFFFFF);
        off.setCornerRadius(dp(16));
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, on);
        states.addState(new int[]{}, off);
        states.setEnterFadeDuration(MOTION_MS);
        states.setExitFadeDuration(MOTION_MS);
        return states;
    }

    private final java.util.Map<TextView, android.animation.ValueAnimator> resizing = new java.util.HashMap<>();

    private void focused(TextView b) {
        int from = b.getWidth();
        b.setText(labels.get(b));
        b.setCompoundDrawablePadding(dp(6));
        b.setPaddingRelative(dp(10), 0, dp(13), 0);
        b.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(dp(32), View.MeasureSpec.EXACTLY));
        resize(b, from, b.getMeasuredWidth(), null);
        b.animate().scaleX(1.06f).scaleY(1.06f).setDuration(MOTION_MS).setInterpolator(EASE).start();
    }

    private void unfocused(TextView b) {
        int pad = Math.round(8.5f * density);
        // The label stays while the pill closes over it (a TextView clips its text to its bounds), then goes.
        resize(b, b.getWidth(), dp(32), () -> {
            b.setText(null);
            b.setCompoundDrawablePadding(0);
            b.setPaddingRelative(pad, 0, pad, 0);
        });
        b.animate().scaleX(1f).scaleY(1f).setDuration(MOTION_MS).setInterpolator(EASE).start();
    }

    /**
     * Animates a button's width without ever passing through zero: Android drops focus from a focused view whose
     * size becomes zero (View.sizeChange), which is what a LayoutTransition did on the first frame of the slide and
     * sent the focus to a tile. Before the first layout (width 0) the change is applied at once.
     */
    private void resize(TextView b, int from, int to, Runnable end) {
        android.animation.ValueAnimator running = resizing.remove(b);
        if (running != null) running.cancel();
        ViewGroup.LayoutParams lp = b.getLayoutParams();
        if (from <= 0 || from == to) {
            if (end != null) end.run();
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            b.setLayoutParams(lp);
            return;
        }
        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofInt(from, to);
        anim.setDuration(MOTION_MS);
        anim.setInterpolator(EASE);
        anim.addUpdateListener(a -> {
            lp.width = (int) a.getAnimatedValue();
            b.setLayoutParams(lp);
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override public void onAnimationCancel(android.animation.Animator a) {
                cancelled = true;
            }

            @Override public void onAnimationEnd(android.animation.Animator a) {
                resizing.remove(b);
                if (cancelled) return;
                if (end != null) end.run();
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                b.setLayoutParams(lp);
            }
        });
        resizing.put(b, anim);
        anim.start();
    }

    /** The quick settings panel rises from the Settings button, lined up with the button row's right edge. */
    void anchorPanel(android.content.Intent panel, View settings) {
        int[] at = new int[2];
        settings.getLocationOnScreen(at);
        panel.putExtra(QuickSettingsActivity.EXTRA_ABOVE_Y, at[1]);
        panel.putExtra(QuickSettingsActivity.EXTRA_END_MARGIN, dp(64));
    }

    /** From MainActivity.updateWifiLabel: the network icon with its online dot, and the label for focus. */
    void updateNetwork(TextView button, int iconRes, CharSequence name, boolean online) {
        CharSequence label = name + " · " + activity.getString(online ? R.string.connected : R.string.offline);
        labels.put(button, label);
        button.setContentDescription(label);
        if (button.isFocused()) button.setText(label);
        if (networkIcon == null || iconRes != networkIconRes) {
            networkIconRes = iconRes;
            android.graphics.drawable.Drawable inner = activity.getDrawable(iconRes);
            if (inner == null) return;
            networkIcon = new DotIcon(inner.mutate(), density);
            networkIcon.setBounds(0, 0, dp(15), dp(15));
            button.setCompoundDrawablesRelative(networkIcon, null, null, null);
        }
        networkIcon.setOnline(online);
    }

    /**
     * An icon with the mockup's status dot at its top right: 5.5dp of green, cut out of the button by a 1.5dp ring
     * in the button's own colour (night, or white while focused). The pill's ink tints only the icon.
     */
    private static final class DotIcon extends android.graphics.drawable.Drawable {
        private static final int GREEN = 0xFF30D158;
        private final android.graphics.drawable.Drawable icon;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        private boolean online, focused;

        DotIcon(android.graphics.drawable.Drawable icon, float density) {
            this.icon = icon;
            this.density = density;
        }

        void setOnline(boolean online) {
            if (this.online == online) return;
            this.online = online;
            invalidateSelf();
        }

        @Override protected void onBoundsChange(android.graphics.Rect bounds) {
            icon.setBounds(bounds);
        }

        @Override public void draw(Canvas canvas) {
            icon.draw(canvas);
            if (!online) return;
            android.graphics.Rect b = getBounds();
            float cx = b.right - 0.25f * density, cy = b.top + 1.25f * density, r = 2.75f * density;
            paint.setColor(focused ? 0xFFFFFFFF : 0xFF000000 | NIGHT);
            canvas.drawCircle(cx, cy, r + 1.5f * density, paint);
            paint.setColor(GREEN);
            canvas.drawCircle(cx, cy, r, paint);
        }

        @Override public boolean isStateful() {
            return true;
        }

        @Override protected boolean onStateChange(int[] state) {
            boolean f = false;
            for (int s : state) if (s == android.R.attr.state_focused) f = true;
            boolean iconChanged = icon.setState(state);
            if (f == focused) return iconChanged;
            focused = f;
            invalidateSelf();
            return true;
        }

        @Override public void setTintList(android.content.res.ColorStateList tint) {
            icon.setTintList(tint);   // the pill's ink, never the dot
        }

        @Override public void setTintMode(android.graphics.PorterDuff.Mode mode) {
            icon.setTintMode(mode);
        }

        @Override public void setAlpha(int alpha) {
            icon.setAlpha(alpha);
            paint.setAlpha(alpha);
        }

        @Override public void setColorFilter(android.graphics.ColorFilter filter) {
            icon.setColorFilter(filter);
        }

        @Override public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }

        @Override public int getIntrinsicWidth() {
            return icon.getIntrinsicWidth();
        }

        @Override public int getIntrinsicHeight() {
            return icon.getIntrinsicHeight();
        }
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
