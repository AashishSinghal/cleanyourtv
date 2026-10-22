package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * my-tv quiet system buttons: 32dp round icons on a faint fill; focused, one turns white with dark ink,
 * lifts 1.06x and slides its label out ("Wi-Fi · Connected") over 240 ms. The network icon carries a green dot
 * while online. Used by the calm layout, and on its own over the standard top bar (quiet_buttons).
 */
final class QuietButtons {
    private final Activity activity;
    private final LinearLayout row;
    private final float density;

    QuietButtons(Activity activity, LinearLayout row, float density) {
        this.activity = activity;
        this.row = row;
        this.density = density;
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

    private static final int INK_FOCUSED = 0xFF0B0D14, INK_BUTTON = 0xE0FFFFFF;

    /** Each button's label, shown only while it has focus ("Wi-Fi · Connected"). */
    private final java.util.Map<TextView, CharSequence> labels = new java.util.HashMap<>();
    private DotIcon networkIcon;
    private int networkIconRes;

    /**
     * 32dp round icons on a faint fill. Focused: white, dark ink, 1.06x, and the label slides out to the left (the
     * row is anchored at its right end and each button animates its own width, see resize).
     */
    void style() {
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
        states.setEnterFadeDuration(CalmLayout.MOTION_MS);
        states.setExitFadeDuration(CalmLayout.MOTION_MS);
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
        b.animate().scaleX(1.06f).scaleY(1.06f).setDuration(CalmLayout.MOTION_MS).setInterpolator(CalmLayout.EASE).start();
    }

    private void unfocused(TextView b) {
        int pad = Math.round(8.5f * density);
        // The label stays while the pill closes over it (a TextView clips its text to its bounds), then goes.
        resize(b, b.getWidth(), dp(32), () -> {
            b.setText(null);
            b.setCompoundDrawablePadding(0);
            b.setPaddingRelative(pad, 0, pad, 0);
        });
        b.animate().scaleX(1f).scaleY(1f).setDuration(CalmLayout.MOTION_MS).setInterpolator(CalmLayout.EASE).start();
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
        anim.setDuration(CalmLayout.MOTION_MS);
        anim.setInterpolator(CalmLayout.EASE);
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
            paint.setColor(focused ? 0xFFFFFFFF : 0xFF000000 | CalmLayout.NIGHT);
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

}
