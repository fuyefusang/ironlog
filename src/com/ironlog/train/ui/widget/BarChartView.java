package com.ironlog.train.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import com.ironlog.train.logic.Fmt;
import com.ironlog.train.ui.Ui;

/** Rounded bar chart for weekly tonnage. */
public class BarChartView extends View {

    private double[] values = new double[0];
    private String[] labels = new String[0];
    private int accent = Ui.ACCENT;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context c) {
        super(c);
        p.setTypeface(Ui.MEDIUM);
    }

    public void setData(double[] values, String[] labels) {
        this.values = values == null ? new double[0] : values;
        this.labels = labels == null ? new String[0] : labels;
        invalidate();
    }

    public void setAccent(int color) {
        this.accent = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int h = Ui.dp(getContext(), 150);
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas cv) {
        int padL = Ui.dp(getContext(), 4);
        int padR = Ui.dp(getContext(), 4);
        int labelH = Ui.dp(getContext(), 22);
        int valueH = Ui.dp(getContext(), 18);
        int w = getWidth() - padL - padR;
        int usableH = getHeight() - labelH - valueH;

        if (values.length == 0) {
            return;
        }

        double max = 0;
        for (double v : values) {
            max = Math.max(max, v);
        }
        if (max <= 0) {
            max = 1;
        }

        float slot = (float) w / values.length;
        float barW = Math.min(Ui.dp(getContext(), 34), slot * 0.52f);

        for (int i = 0; i < values.length; i++) {
            float cx = padL + slot * i + slot / 2f;
            float barH = (float) (values[i] / max * usableH);
            float bottom = valueH + usableH;
            float top = bottom - barH;

            // track
            p.setColor(Ui.GRID);
            cv.drawRoundRect(new RectF(cx - barW / 2, top, cx + barW / 2, bottom),
                    barW / 2, barW / 2, p);

            if (barH > 1) {
                p.setColor(accent);
                RectF r = new RectF(cx - barW / 2, top, cx + barW / 2, bottom);
                cv.drawRoundRect(r, barW / 2, barW / 2, p);
                // brighter cap
                p.setColor(0xFFFFFFFF);
                p.setAlpha(40);
                cv.drawRoundRect(new RectF(cx - barW / 2, top, cx + barW / 2,
                        top + Math.min(barH, Ui.dp(getContext(), 6))), barW / 2, barW / 2, p);
            }

            // value on top
            if (values[i] > 0) {
                p.setColor(Ui.TEXT2);
                p.setTextSize(Ui.dp(getContext(), 10));
                p.setTextAlign(Paint.Align.CENTER);
                cv.drawText(Fmt.vol(values[i]), cx, Math.max(valueH - 2, top - 6), p);
            }

            // label
            p.setColor(Ui.TEXT3);
            p.setTextSize(Ui.dp(getContext(), 10));
            p.setTextAlign(Paint.Align.CENTER);
            String lb = i < labels.length ? labels[i] : "";
            cv.drawText(lb, cx, getHeight() - 4, p);
        }
    }
}
