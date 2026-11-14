package com.ironlog.train.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.ironlog.train.ui.Ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/** Compact month grid; days with workouts get a dot, the selected day gets a filled pill. */
public class CalendarView extends View {

    public interface Listener {
        void onDaySelected(LocalDate day);
    }

    private YearMonth month = YearMonth.now();
    private LocalDate selected = LocalDate.now();
    private LocalDate today = LocalDate.now();
    private Map<Integer, Integer> counts = new HashMap<>();
    private Map<Integer, Integer> planned = new HashMap<>();
    private Listener listener;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    private static final String[] WEEK = {"一", "二", "三", "四", "五", "六", "日"};

    public CalendarView(Context c) {
        super(c);
        p.setTypeface(Ui.MEDIUM);
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void setMonth(YearMonth m) {
        this.month = m;
        invalidate();
    }

    public YearMonth getMonth() {
        return month;
    }

    public void setSelected(LocalDate d) {
        this.selected = d;
        invalidate();
    }

    public LocalDate getSelected() {
        return selected;
    }

    /** day-of-month -> session count */
    public void setCounts(Map<Integer, Integer> counts) {
        this.counts = counts == null ? new HashMap<Integer, Integer>() : counts;
        invalidate();
    }

    /** day-of-month -> number of routines scheduled that day */
    public void setPlanned(Map<Integer, Integer> planned) {
        this.planned = planned == null ? new HashMap<Integer, Integer>() : planned;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int headerH = Ui.dp(getContext(), 26);
        int cell = (int) (w / 7f);
        int rows = 6;
        setMeasuredDimension(w, headerH + cell * rows + Ui.dp(getContext(), 4));
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth();
        int headerH = Ui.dp(getContext(), 26);
        float cell = w / 7f;
        float cellH = (getHeight() - headerH - Ui.dp(getContext(), 4)) / 6f;

        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);

        // weekday header
        p.setColor(Ui.TEXT3);
        p.setTextSize(Ui.dp(getContext(), 10.5f));
        for (int i = 0; i < 7; i++) {
            float cx = cell * i + cell / 2f;
            cv.drawText(WEEK[i], cx, headerH - Ui.dp(getContext(), 8), p);
        }

        LocalDate first = month.atDay(1);
        int offset = first.getDayOfWeek().getValue() - 1;
        int len = month.lengthOfMonth();

        for (int d = 1; d <= len; d++) {
            int idx = offset + d - 1;
            int row = idx / 7;
            int col = idx % 7;
            float cx = cell * col + cell / 2f;
            float cy = headerH + cellH * row + cellH / 2f;
            float rad = Math.min(cell, cellH) * 0.36f;

            LocalDate day = month.atDay(d);
            boolean isSelected = day.equals(selected);
            boolean isToday = day.equals(today);
            Integer cnt = counts.get(d);
            boolean hasWork = cnt != null && cnt > 0;

            if (isSelected) {
                p.setColor(Ui.ACCENT);
                cv.drawCircle(cx, cy, rad, p);
            } else if (isToday) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Ui.dp(getContext(), 1.2f));
                p.setColor(Ui.STROKE);
                cv.drawCircle(cx, cy, rad, p);
                p.setStyle(Paint.Style.FILL);
            }

            // planned day: hollow ring in the accent-neutral blue
            Integer planCnt = planned.get(d);
            if (planCnt != null && planCnt > 0 && !isSelected) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Ui.dp(getContext(), 1.4f));
                p.setColor((Ui.BLUE & 0x00FFFFFF) | 0x99000000);
                cv.drawCircle(cx, cy, rad - Ui.dp(getContext(), 1f), p);
                p.setStyle(Paint.Style.FILL);
            }

            p.setColor(isSelected ? Ui.ON_ACCENT : (isToday ? Ui.TEXT : Ui.TEXT2));
            p.setTextSize(Ui.dp(getContext(), 13));
            cv.drawText(String.valueOf(d), cx, cy + Ui.dp(getContext(), 4.5f), p);

            if (hasWork && !isSelected) {
                p.setColor(Ui.ACCENT);
                cv.drawCircle(cx, cy + rad + Ui.dp(getContext(), 3.2f), Ui.dp(getContext(), 1.8f), p);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }
        int w = getWidth();
        float cell = w / 7f;
        int headerH = Ui.dp(getContext(), 26);
        float cellH = (getHeight() - headerH - Ui.dp(getContext(), 4)) / 6f;
        float y = e.getY() - headerH;
        if (y < 0) {
            return true;
        }
        int col = (int) (e.getX() / cell);
        int row = (int) (y / cellH);
        if (col < 0 || col > 6) {
            return true;
        }
        int dayNum = row * 7 + col - (month.atDay(1).getDayOfWeek().getValue() - 1) + 1;
        if (dayNum < 1 || dayNum > month.lengthOfMonth()) {
            return true;
        }
        LocalDate d = month.atDay(dayNum);
        selected = d;
        invalidate();
        if (listener != null) {
            listener.onDaySelected(d);
        }
        return true;
    }

    /** Rebuild the counts map for a given month from a day -> count lookup. */
    public static Map<Integer, Integer> countsFor(YearMonth month, CountLookup lookup) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            int c = lookup.count(month.atDay(d));
            if (c > 0) {
                map.put(d, c);
            }
        }
        return map;
    }

    public interface CountLookup {
        int count(LocalDate day);
    }
}
