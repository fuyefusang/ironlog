package com.ironlog.train.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Small styling toolkit so the UI code stays readable without any support library. */
public final class Ui {

    // Palette is mutable: the user can switch between dark and light at runtime.
    public static boolean DARK = true;

    public static int BG = 0xFF0E1116;
    public static int SURFACE = 0xFF171D25;
    public static int SURFACE2 = 0xFF202832;
    public static int STROKE = 0xFF2B3541;
    public static int TEXT = 0xFFF3F6F9;
    public static int TEXT2 = 0xFF9BA8B6;
    public static int TEXT3 = 0xFF5F6C7B;
    public static int ACCENT = 0xFFFFB454;
    public static int ACCENT_SOFT = 0xFF3B2A12;
    public static int GREEN = 0xFF57CE7E;
    public static int RED = 0xFFE2605F;
    public static int BLUE = 0xFF5B93FF;
    public static int PURPLE = 0xFFA98BFF;
    /** Bottom nav / fixed header background. */
    public static int NAV = 0xFF12171E;
    /** Chart grid lines. */
    public static int GRID = 0xFF232B35;

    /** On-accent foreground (text drawn on top of ACCENT). */
    public static int ON_ACCENT = 0xFF151007;

    public static void applyTheme(boolean dark) {
        DARK = dark;
        if (dark) {
            BG = 0xFF0E1116;
            SURFACE = 0xFF171D25;
            SURFACE2 = 0xFF202832;
            STROKE = 0xFF2B3541;
            TEXT = 0xFFF3F6F9;
            TEXT2 = 0xFF9BA8B6;
            TEXT3 = 0xFF5F6C7B;
            ACCENT = 0xFFFFB454;
            ACCENT_SOFT = 0xFF3B2A12;
            GREEN = 0xFF57CE7E;
            RED = 0xFFE2605F;
            BLUE = 0xFF5B93FF;
            PURPLE = 0xFFA98BFF;
            ON_ACCENT = 0xFF151007;
            NAV = 0xFF12171E;
            GRID = 0xFF232B35;
        } else {
            BG = 0xFFF4F6F8;
            SURFACE = 0xFFFFFFFF;
            SURFACE2 = 0xFFEBEEF3;
            STROKE = 0xFFDCE2EA;
            TEXT = 0xFF11161C;
            TEXT2 = 0xFF56616E;
            TEXT3 = 0xFF8A95A3;
            ACCENT = 0xFFD9820B;
            ACCENT_SOFT = 0xFFFBEEDA;
            GREEN = 0xFF2E9E5B;
            RED = 0xFFCF4A4B;
            BLUE = 0xFF2F6FE0;
            PURPLE = 0xFF7A5AE0;
            ON_ACCENT = 0xFFFFFFFF;
            NAV = 0xFFFFFFFF;
            GRID = 0xFFE3E8EF;
        }
    }

    private static int rippleColor() {
        return DARK ? 0x33FFFFFF : 0x22000000;
    }

    public static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    public static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);

    private Ui() {
    }

    // --------------------------------------------------------------- sizes

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int screenWidth(Context c) {
        return c.getResources().getDisplayMetrics().widthPixels;
    }

    // ------------------------------------------------------------- drawing

    public static GradientDrawable bg(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable strokeBg(int fill, int stroke, float radiusDp, float widthDp, Context c) {
        GradientDrawable d = bg(fill, radiusDp, c);
        d.setStroke(Math.max(1, dp(c, widthDp)), stroke);
        return d;
    }

    public static GradientDrawable circle(int color, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    /** Adds a ripple on top of an existing background. */
    public static void ripple(View v, int rippleColor) {
        android.graphics.drawable.Drawable bgd = v.getBackground();
        if (bgd == null) {
            return;
        }
        RippleDrawable r = new RippleDrawable(ColorStateList.valueOf(rippleColor), bgd, null);
        v.setBackground(r);
    }

    // ---------------------------------------------------------------- text

    public static TextView t(Context c, CharSequence s, float sp, int color) {
        TextView v = new TextView(c);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        v.setLineSpacing(dp(c, 3), 1f);
        return v;
    }

    public static TextView num(Context c, CharSequence s, float sp, int color) {
        TextView v = t(c, s, sp, color);
        v.setTypeface(MEDIUM);
        return v;
    }

    public static TextView bold(Context c, CharSequence s, float sp, int color) {
        TextView v = t(c, s, sp, color);
        v.setTypeface(BOLD);
        return v;
    }

    public static TextView label(Context c, CharSequence s) {
        TextView v = t(c, s, 12f, TEXT2);
        v.setLetterSpacing(0.06f);
        return v;
    }

    public static TextView title(Context c, CharSequence s) {
        TextView v = bold(c, s, 25f, TEXT);
        return v;
    }

    public static TextView sectionTitle(Context c, CharSequence s) {
        TextView v = bold(c, s, 16f, TEXT);
        return v;
    }

    // ------------------------------------------------------------- layout

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        // Full width by default: containers are almost always stacked vertically.
        l.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    public static ScrollView scroll(Context c, View content) {
        ScrollView s = new ScrollView(c);
        s.setFillViewport(true);
        s.setClipToPadding(false);
        s.setVerticalScrollBarEnabled(false);
        s.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return s;
    }

    public static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    public static View gap(Context c, int widthDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, widthDp), 1));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(STROKE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 0.7f)));
        v.setLayoutParams(lp);
        return v;
    }

    public static void pad(View v, int l, int t, int r, int b) {
        v.setPadding(dp(v.getContext(), l), dp(v.getContext(), t),
                dp(v.getContext(), r), dp(v.getContext(), b));
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lpm(Context c, int wDp, int hDp) {
        return new LinearLayout.LayoutParams(dp(c, wDp), dp(c, hDp));
    }

    public static LinearLayout.LayoutParams weight(Context c, float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    public static void marginBottom(View v, Context c, int dp) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).bottomMargin = dp(c, dp);
        }
    }

    public static void marginTop(View v, Context c, int dp) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).topMargin = dp(c, dp);
        }
    }

    // --------------------------------------------------------------- input

    public static EditText input(Context c, String hint, int inputType) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(TEXT3);
        e.setTextColor(TEXT);
        e.setTextSize(15f);
        e.setTypeface(MEDIUM);
        e.setInputType(inputType);
        e.setBackground(bg(SURFACE2, 10, c));
        e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        e.setSingleLine(true);
        e.setImeOptions(EditorInfo.IME_ACTION_DONE);
        // Tapping a filled field should let the user retype it instead of appending
        // to the old value. select-all-on-focus loses to the cursor placement the
        // movement method does on finger-up, so select again after the tap.
        e.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, MotionEvent ev) {
                if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
                    v.post(new Runnable() {
                        @Override
                        public void run() {
                            EditText et = (EditText) v;
                            if (et.hasFocus()) {
                                et.selectAll();
                            }
                        }
                    });
                }
                return false;
            }
        });
        return e;
    }

    public static EditText numberInput(Context c, String hint) {
        EditText e = input(c, hint, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setGravity(Gravity.CENTER);
        e.setTextSize(15f);
        return e;
    }

    public static EditText textInput(Context c, String hint) {
        return input(c, hint, InputType.TYPE_CLASS_TEXT);
    }

    public static EditText multiline(Context c, String hint) {
        EditText e = input(c, hint, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        e.setSingleLine(false);
        e.setMinLines(2);
        e.setGravity(Gravity.TOP | Gravity.START);
        return e;
    }

    // --------------------------------------------------------------- cards

    public static LinearLayout card(Context c) {
        LinearLayout l = col(c);
        l.setBackground(bg(SURFACE, 16, c));
        pad(l, 16, 16, 16, 16);
        return l;
    }

    public static LinearLayout section(Context c) {
        LinearLayout l = col(c);
        return l;
    }

    public static TextView pill(Context c, CharSequence text, int fg, int bgColor) {
        TextView v = t(c, text, 11.5f, fg);
        v.setTypeface(MEDIUM);
        v.setBackground(bg(bgColor, 40, c));
        v.setPadding(dp(c, 9), dp(c, 4), dp(c, 9), dp(c, 4));
        return v;
    }

    public static TextView button(Context c, CharSequence text, int fg, int bgColor) {
        TextView v = t(c, text, 15f, fg);
        v.setTypeface(MEDIUM);
        v.setGravity(Gravity.CENTER);
        v.setBackground(bg(bgColor, 12, c));
        v.setPadding(dp(c, 16), dp(c, 13), dp(c, 16), dp(c, 13));
        v.setClickable(true);
        v.setFocusable(true);
        ripple(v, rippleColor());
        return v;
    }

    public static TextView ghostButton(Context c, CharSequence text) {
        TextView v = t(c, text, 14f, TEXT2);
        v.setTypeface(MEDIUM);
        v.setGravity(Gravity.CENTER);
        v.setBackground(strokeBg(0x00000000, STROKE, 12, 1, c));
        v.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        v.setClickable(true);
        ripple(v, rippleColor());
        return v;
    }

    public static TextView primaryButton(Context c, CharSequence text) {
        return button(c, text, ON_ACCENT, ACCENT);
    }

    public static TextView dangerButton(Context c, CharSequence text) {
        return button(c, text, RED, 0x1FE2605F);
    }

    /** Standard screen header: big title with optional trailing action. */
    public static LinearLayout header(Context c, String title, View trailing) {
        LinearLayout h = row(c);
        h.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = title(c, title);
        h.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (trailing != null) {
            h.addView(trailing);
        }
        return h;
    }

    public static void gone(View v) {
        v.setVisibility(View.GONE);
    }

    public static void visible(View v) {
        v.setVisibility(View.VISIBLE);
    }

    // ------------------------------------------------------- touch helpers

    /** Simple press feedback for views that use plain backgrounds. */
    public static void pressable(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    view.setAlpha(0.65f);
                } else if (e.getAction() == MotionEvent.ACTION_UP
                        || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    view.setAlpha(1f);
                }
                return false;
            }
        });
    }

    public static String nz(String s, String fallback) {
        return TextUtils.isEmpty(s) ? fallback : s;
    }
}
