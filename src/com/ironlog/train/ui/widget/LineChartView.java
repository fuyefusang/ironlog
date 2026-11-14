package com.ironlog.train.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

import com.ironlog.train.logic.Fmt;
import com.ironlog.train.logic.Stats;
import com.ironlog.train.ui.Ui;

import java.util.List;

/** Line chart with soft gradient fill, used for strength / bodyweight trends. */
public class LineChartView extends View {

    private long[] dates = new long[0];
    private double[] values = new double[0];
    private String emptyText = "还没有数据";
    private int accent = Ui.BLUE;
    private boolean showDates = true;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path fill = new Path();
    private final Path line = new Path();

    public LineChartView(Context c) {
        super(c);
        p.setTypeface(Ui.MEDIUM);
    }

    public void setPoints(List<Stats.Point> pts) {
        if (pts == null || pts.isEmpty()) {
            setSeries(new long[0], new double[0]);
            return;
        }
        long[] d = new long[pts.size()];
        double[] v = new double[pts.size()];
        for (int i = 0; i < pts.size(); i++) {
            d[i] = pts.get(i).date;
            v[i] = pts.get(i).e1rm;
        }
        setSeries(d, v);
    }

    public void setSeries(long[] dates, double[] values) {
        this.dates = dates == null ? new long[0] : dates;
        this.values = values == null ? new double[0] : values;
        invalidate();
    }

    public void setAccent(int color) {
        this.accent = color;
        invalidate();
    }

    public void setShowDates(boolean show) {
        this.showDates = show;
        invalidate();
    }

    public void setEmptyText(String text) {
        this.emptyText = text;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), Ui.dp(getContext(), 170));
    }

    @Override
    protected void onDraw(Canvas cv) {
        int padL = Ui.dp(getContext(), 10);
        int padR = Ui.dp(getContext(), 10);
        int padT = Ui.dp(getContext(), 18);
        int padB = showDates ? Ui.dp(getContext(), 22) : Ui.dp(getContext(), 10);
        int w = getWidth() - padL - padR;
        int h = getHeight() - padT - padB;
        int n = values.length;

        if (n == 0 || w <= 0 || h <= 0) {
            p.setColor(Ui.TEXT3);
            p.setTextSize(Ui.dp(getContext(), 12));
            p.setTextAlign(Paint.Align.CENTER);
            cv.drawText(emptyText, getWidth() / 2f, getHeight() / 2f, p);
            return;
        }

        double max = values[0];
        double min = values[0];
        for (double v : values) {
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        if (max - min < 0.001) {
            max = max + Math.max(1, max * 0.1);
            min = Math.max(0, min - Math.max(1, min * 0.1));
        }
        double range = max - min;

        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = padL + (n == 1 ? w / 2f : (float) w * i / (n - 1));
            ys[i] = padT + h - (float) ((values[i] - min) / range * h);
        }

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        for (int g = 0; g <= 2; g++) {
            float y = padT + h * g / 2f;
            p.setColor(Ui.GRID);
            cv.drawLine(padL, y, padL + w, y, p);
        }

        line.reset();
        line.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2f;
            line.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i]);
        }

        fill.reset();
        fill.addPath(line);
        fill.lineTo(xs[n - 1], padT + h);
        fill.lineTo(xs[0], padT + h);
        fill.close();

        p.setStyle(Paint.Style.FILL);
        int accentFaded = (accent & 0x00FFFFFF) | 0x55000000;
        p.setShader(new LinearGradient(0, padT, 0, padT + h,
                new int[]{accentFaded, 0x00000000}, null, Shader.TileMode.CLAMP));
        cv.drawPath(fill, p);
        p.setShader(null);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(getContext(), 2.2f));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(accent);
        cv.drawPath(line, p);

        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < n; i++) {
            p.setColor(accent);
            cv.drawCircle(xs[i], ys[i], Ui.dp(getContext(), 3f), p);
            p.setColor(Ui.BG);
            cv.drawCircle(xs[i], ys[i], Ui.dp(getContext(), 1.4f), p);
        }

        p.setTextSize(Ui.dp(getContext(), 9.5f));
        p.setColor(Ui.TEXT3);
        p.setTextAlign(Paint.Align.LEFT);
        cv.drawText(Fmt.num(max), padL, padT - 5, p);
        cv.drawText(Fmt.num(min), padL, padT + h + Ui.dp(getContext(), 11), p);

        if (showDates) {
            if (n == 1) {
                // One point: label it under the dot itself, otherwise the axis shows
                // the same date twice (left and right) and neither lines up.
                p.setTextAlign(Paint.Align.CENTER);
                cv.drawText(Fmt.shortDate(dates[0]), xs[0], getHeight() - 4, p);
                p.setColor(accent);
                p.setTextSize(Ui.dp(getContext(), 10.5f));
                cv.drawText(Fmt.num(values[0]), xs[0],
                        ys[0] - Ui.dp(getContext(), 9), p);
            } else {
                p.setTextAlign(Paint.Align.LEFT);
                cv.drawText(Fmt.shortDate(dates[0]), padL, getHeight() - 4, p);
                p.setTextAlign(Paint.Align.RIGHT);
                cv.drawText(Fmt.shortDate(dates[n - 1]), padL + w, getHeight() - 4, p);
            }
        }
    }
}
