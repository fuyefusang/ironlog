package com.ironlog.train.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Vector icon helpers: every glyph in the UI comes from res/drawable. */
public final class Icons {

    private Icons() {
    }

    /** Loads a vector drawable tinted to the given color. */
    public static Drawable d(Context c, int res, int color) {
        Drawable dr = c.getDrawable(res);
        if (dr == null) {
            return null;
        }
        dr = dr.mutate();
        dr.setTint(color);
        return dr;
    }

    public static ImageView iv(Context c, int res, float sizeDp, int color) {
        ImageView v = new ImageView(c);
        v.setImageDrawable(d(c, res, color));
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int s = Ui.dp(c, sizeDp);
        v.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return v;
    }

    /** Icon-only tappable control with a comfortable hit area. */
    public static ImageView button(Context c, int res, float sizeDp, int color, float padDp) {
        ImageView v = iv(c, res, sizeDp, color);
        int p = Ui.dp(c, padDp);
        v.setPadding(p, p, p, p);
        v.setClickable(true);
        v.setFocusable(true);
        Ui.pressable(v);
        return v;
    }

    private static void apply(TextView v, Context c, int res, int color, float sizeDp, float gapDp,
                              boolean atStart) {
        Drawable dr = d(c, res, color);
        if (dr == null) {
            return;
        }
        int s = Ui.dp(c, sizeDp);
        dr.setBounds(0, 0, s, s);
        if (atStart) {
            v.setCompoundDrawables(dr, null, null, null);
        } else {
            v.setCompoundDrawables(null, null, dr, null);
        }
        v.setCompoundDrawablePadding(Ui.dp(c, gapDp));
    }

    /** Prepends an icon to a text view. */
    public static TextView start(TextView v, Context c, int res, int color, float sizeDp, float gapDp) {
        apply(v, c, res, color, sizeDp, gapDp, true);
        return v;
    }

    /** Appends a trailing icon (e.g. a chevron) to a text view. */
    public static TextView end(TextView v, Context c, int res, int color, float sizeDp, float gapDp) {
        apply(v, c, res, color, sizeDp, gapDp, false);
        return v;
    }

    /** Small text+icon tile used for secondary actions. */
    public static TextView action(Context c, int res, String text, int fg, int bgColor) {
        TextView v = Ui.t(c, text, 12.5f, fg);
        v.setTypeface(Ui.MEDIUM);
        v.setGravity(android.view.Gravity.CENTER);
        v.setBackground(Ui.bg(bgColor, 40, c));
        v.setPadding(Ui.dp(c, 11), Ui.dp(c, 6), Ui.dp(c, 11), Ui.dp(c, 6));
        v.setClickable(true);
        Ui.ripple(v, Ui.DARK ? 0x22FFFFFF : 0x1A000000);
        return start(v, c, res, fg, 13, 5);
    }

    /** Primary-shaped button with a leading icon. */
    public static TextView button(Context c, int res, String text, int fg, int bgColor) {
        TextView v = Ui.button(c, text, fg, bgColor);
        return start(v, c, res, fg, 17, 7);
    }

    /** Ghost (outlined) button with a leading icon. */
    public static TextView ghost(Context c, int res, String text) {
        TextView v = Ui.ghostButton(c, text);
        return start(v, c, res, Ui.TEXT2, 16, 7);
    }
}
