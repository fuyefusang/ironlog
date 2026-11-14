package com.ironlog.train.ui;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ironlog.train.data.Store;
import com.ironlog.train.data.WorkoutSession;
import com.ironlog.train.logic.Fmt;
import com.ironlog.train.logic.Stats;
import com.ironlog.train.ui.widget.CalendarView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class HistoryScreen extends Screen {

    private YearMonth month = YearMonth.now();
    private LocalDate selected = LocalDate.now();

    public HistoryScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected void build() {
        final Store store = Store.get(ctx);
        final List<WorkoutSession> finished = store.finishedSessions();

        LinearLayout body = Ui.col(ctx);
        body.setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 18), Ui.dp(ctx, 18), Ui.dp(ctx, 28));

        LinearLayout head = Ui.row(ctx);
        ImageView back = Icons.button(ctx, com.ironlog.train.R.drawable.ic_back, 22, Ui.TEXT, 6);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.pop();
            }
        });
        head.addView(back);

        LinearLayout titles = Ui.col(ctx);
        titles.addView(Ui.label(ctx, "全部记录"));
        titles.addView(Ui.title(ctx, "训练日历"));
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        body.addView(head);

        body.addView(Ui.spacer(ctx, 16));

        // ------------------------------------------------------- month nav
        LinearLayout navRow = Ui.row(ctx);
        ImageView prev = Icons.button(ctx, com.ironlog.train.R.drawable.ic_back, 18, Ui.TEXT2, 6);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                month = month.minusMonths(1);
                rebuild();
            }
        });
        navRow.addView(prev);

        TextView monthLabel = Ui.bold(ctx, Fmt.monthTitle(month), 15.5f, Ui.TEXT);
        navRow.addView(monthLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView next = Icons.button(ctx, com.ironlog.train.R.drawable.ic_chevron_right,
                18, Ui.TEXT2, 6);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                month = month.plusMonths(1);
                rebuild();
            }
        });
        navRow.addView(next);
        body.addView(navRow);

        // --------------------------------------------------------- calendar
        CalendarView cal = new CalendarView(ctx);
        final Map<Integer, Integer> counts = CalendarView.countsFor(month, new CalendarView.CountLookup() {
            @Override
            public int count(LocalDate day) {
                return Stats.countOn(finished, day);
            }
        });
        cal.setCounts(counts);
        cal.setMonth(month);
        if (selected.getYear() == month.getYear() && selected.getMonthValue() == month.getMonthValue()) {
            cal.setSelected(selected);
        } else {
            cal.setSelected(month.atDay(1));
            selected = month.atDay(1);
        }
        cal.setListener(new CalendarView.Listener() {
            @Override
            public void onDaySelected(LocalDate day) {
                selected = day;
                rebuild();
            }
        });
        body.addView(cal, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body.addView(Ui.spacer(ctx, 14));

        // ------------------------------------------------------ day detail
        List<WorkoutSession> dayList = Stats.onDay(finished, selected);
        LinearLayout dayHead = Ui.row(ctx);
        String title = selected.getMonthValue() + "月" + selected.getDayOfMonth() + "日 · "
                + Fmt.weekday(Fmt.millis(selected));
        dayHead.addView(Ui.sectionTitle(ctx, title),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        double dayVol = Stats.volumeOn(finished, selected);
        if (dayVol > 0) {
            dayHead.addView(Ui.num(ctx, Fmt.vol(dayVol) + " " + store.settings.unit, 12f, Ui.ACCENT));
        }
        body.addView(dayHead);
        body.addView(Ui.spacer(ctx, 10));

        if (dayList.isEmpty()) {
            LinearLayout empty = Ui.card(ctx);
            TextView e = Ui.t(ctx, "这天没有训练记录", 12.5f, Ui.TEXT3);
            e.setGravity(Gravity.CENTER);
            empty.addView(e);
            body.addView(empty);
        } else {
            for (WorkoutSession s : dayList) {
                body.addView(sessionCard(s));
                body.addView(Ui.spacer(ctx, 8));
            }
        }

        // ------------------------------------------------------ month list
        body.addView(Ui.spacer(ctx, 18));
        body.addView(Ui.sectionTitle(ctx, "本月全部"));
        body.addView(Ui.spacer(ctx, 10));

        int shown = 0;
        for (WorkoutSession s : finished) {
            LocalDate d = Fmt.date(s.startedAt);
            if (d.getYear() != month.getYear() || d.getMonthValue() != month.getMonthValue()) {
                continue;
            }
            body.addView(sessionCard(s));
            body.addView(Ui.spacer(ctx, 8));
            shown++;
        }
        if (shown == 0) {
            TextView e = Ui.t(ctx, "本月还没有训练", 12.5f, Ui.TEXT3);
            body.addView(e);
        }

        addView(Ui.scroll(ctx, body), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private View sessionCard(final WorkoutSession s) {
        LinearLayout row = Ui.row(ctx);
        row.setBackground(Ui.bg(Ui.SURFACE, 13, ctx));
        Ui.pad(row, 14, 12, 14, 12);
        row.setClickable(true);
        Ui.ripple(row, 0x1FFFFFFF);

        LinearLayout left = Ui.col(ctx);
        left.addView(Ui.bold(ctx, Ui.nz(s.name, "训练"), 14f, Ui.TEXT));
        String sub = Fmt.shortDate(s.startedAt) + " · " + Fmt.duration(s.durationSec())
                + " · " + s.totalSets() + " 组";
        left.addView(Ui.t(ctx, sub, 11f, Ui.TEXT3));
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(Ui.num(ctx, Fmt.vol(s.volume()), 13.5f, Ui.ACCENT));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.push(new SessionDetailScreen(act, s.id));
            }
        });
        return row;
    }
}
