package com.ironlog.train.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Thin wrappers around AlertDialog so every dialog matches the app palette. */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * The platform dialog surface (#424242 on dark, white on light) and its square
     * corners do not match the app's rounded cards, so repaint the window and push
     * the palette colours into the title/buttons after {@code show()}.
     */
    static AlertDialog show(AlertDialog.Builder b, Context c) {
        AlertDialog d = b.show();
        Window w = d.getWindow();
        if (w != null) {
            GradientDrawable bg = Ui.bg(Ui.SURFACE, 18, c);
            bg.setStroke(Ui.dp(c, 1), Ui.GRID);
            w.setBackgroundDrawable(bg);
        }
        recolor(d.getWindow() == null ? null : d.getWindow().getDecorView(), c);
        return d;
    }

    private static void recolor(View root, Context c) {
        if (root == null) {
            return;
        }
        if (root instanceof TextView) {
            TextView tv = (TextView) root;
            // android.R.id.alertTitle is not exposed by every compile SDK, so
            // resolve the platform id by name at runtime.
            int titleId = root.getResources().getIdentifier("alertTitle", "id", "android");
            int messageId = root.getResources().getIdentifier("message", "id", "android");
            if (tv.getId() == titleId) {
                tv.setTextColor(Ui.TEXT);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            } else if (tv.getId() == messageId) {
                tv.setTextColor(Ui.TEXT2);
            }
        }
        if (root instanceof Button) {
            Button btn = (Button) root;
            btn.setTextColor(Ui.ACCENT);
            btn.setAllCaps(false);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                recolor(g.getChildAt(i), c);
            }
        }
    }

    public static void confirm(Context c, String title, String message, final Runnable onYes) {
        AlertDialog.Builder b = new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        onYes.run();
                    }
                })
                .setNegativeButton("取消", null);
        show(b, c);
    }

    public static void options(Context c, String title, String[] items, DialogInterface.OnClickListener cb) {
        list(c, title, null, items, cb);
    }

    /** Title + optional hint + a tappable list. */
    public static void list(Context c, String title, String hint, String[] items,
                           DialogInterface.OnClickListener cb) {
        AlertDialog.Builder b = new AlertDialog.Builder(c)
                .setItems(items, cb)
                .setNegativeButton("取消", null);
        if (hint == null || hint.isEmpty()) {
            b.setTitle(title);
        } else {
            // A message would replace the item list on this platform, so the hint
            // rides along in a custom title instead.
            LinearLayout box = Ui.col(c);
            box.setPadding(Ui.dp(c, 24), Ui.dp(c, 20), Ui.dp(c, 24), Ui.dp(c, 4));
            box.addView(Ui.bold(c, title, 18f, Ui.TEXT));
            TextView sub = Ui.t(c, hint, 12f, Ui.TEXT3);
            sub.setPadding(0, Ui.dp(c, 6), 0, 0);
            sub.setLineSpacing(0, 1.2f);
            box.addView(sub);
            b.setCustomTitle(box);
        }
        show(b, c);
    }

    public interface TextCallback {
        void onText(String text);
    }

    public static void prompt(Context c, String title, String hint, String initial, final TextCallback cb) {
        final EditText e = Ui.textInput(c, hint);
        e.setText(initial == null ? "" : initial);
        e.setSelection(e.getText().length());
        LinearLayout wrap = Ui.col(c);
        wrap.setPadding(Ui.dp(c, 20), Ui.dp(c, 8), Ui.dp(c, 20), 0);
        wrap.addView(e);
        AlertDialog.Builder b = new AlertDialog.Builder(c)
                .setTitle(title)
                .setView(wrap)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        cb.onText(e.getText().toString().trim());
                    }
                })
                .setNegativeButton("取消", null);
        show(b, c);
    }

    public static void number(Context c, String title, String hint, double initial, final ValueCallback cb) {
        final EditText e = Ui.numberInput(c, hint);
        e.setText(initial > 0 ? com.ironlog.train.logic.Fmt.num(initial) : "");
        LinearLayout wrap = Ui.col(c);
        wrap.setPadding(Ui.dp(c, 20), Ui.dp(c, 8), Ui.dp(c, 20), 0);
        wrap.addView(e);
        AlertDialog.Builder b = new AlertDialog.Builder(c)
                .setTitle(title)
                .setView(wrap)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        cb.onValue(parse(e.getText().toString()));
                    }
                })
                .setNegativeButton("取消", null);
        show(b, c);
    }

    public interface ValueCallback {
        void onValue(double v);
    }

    public interface TimeCallback {
        void onTime(int hour, int minute);
    }

    /** Wheel-style hour/minute picker. */
    public static void time(Context c, int hour, int minute, final TimeCallback cb) {
        LinearLayout wrap = Ui.row(c);
        wrap.setGravity(android.view.Gravity.CENTER);
        wrap.setPadding(Ui.dp(c, 24), Ui.dp(c, 12), Ui.dp(c, 24), 0);

        final android.widget.NumberPicker hp = new android.widget.NumberPicker(c);
        hp.setMinValue(0);
        hp.setMaxValue(23);
        hp.setValue(Math.max(0, Math.min(23, hour)));
        hp.setFormatter(new android.widget.NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format(java.util.Locale.US, "%02d", value);
            }
        });
        hp.setWrapSelectorWheel(true);

        final android.widget.NumberPicker mp = new android.widget.NumberPicker(c);
        mp.setMinValue(0);
        mp.setMaxValue(59);
        mp.setValue(Math.max(0, Math.min(59, minute)));
        mp.setFormatter(new android.widget.NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format(java.util.Locale.US, "%02d", value);
            }
        });
        mp.setWrapSelectorWheel(true);

        wrap.addView(hp, new LinearLayout.LayoutParams(0, Ui.dp(c, 150), 1f));
        android.widget.TextView colon = Ui.bold(c, ":", 20f, Ui.TEXT);
        colon.setGravity(android.view.Gravity.CENTER);
        wrap.addView(colon, new LinearLayout.LayoutParams(Ui.dp(c, 24),
                Ui.dp(c, 150)));
        wrap.addView(mp, new LinearLayout.LayoutParams(0, Ui.dp(c, 150), 1f));

        AlertDialog.Builder b = new AlertDialog.Builder(c)
                .setTitle("提醒时间")
                .setView(wrap)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        cb.onTime(hp.getValue(), mp.getValue());
                    }
                })
                .setNegativeButton("取消", null);
        show(b, c);
    }

    public static double parse(String s) {
        if (s == null) {
            return 0;
        }
        s = s.trim().replace(",", ".").replace("，", ".");
        if (s.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
