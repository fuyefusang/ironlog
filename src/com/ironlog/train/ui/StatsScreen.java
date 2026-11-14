package com.ironlog.train.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ironlog.train.data.Exercise;
import com.ironlog.train.data.Routine;
import com.ironlog.train.data.RoutineExercise;
import com.ironlog.train.data.SessionExercise;
import com.ironlog.train.data.SetEntry;
import com.ironlog.train.data.Store;
import com.ironlog.train.data.WorkoutSession;
import com.ironlog.train.logic.Fmt;
import com.ironlog.train.logic.Stats;
import com.ironlog.train.ui.widget.BarChartView;
import com.ironlog.train.ui.widget.CalendarView;
import com.ironlog.train.ui.widget.LineChartView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Combined "data" tab: plan calendar, routines, strength trends and PRs. */
public class StatsScreen extends Screen {

    private String selectedExercise;
    private YearMonth month = YearMonth.now();
    private LocalDate selectedDay = LocalDate.now();

    public StatsScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected void build() {
        final Store store = Store.get(ctx);
        final List<WorkoutSession> finished = store.finishedSessions();

        LinearLayout body = Ui.col(ctx);
        body.setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 20), Ui.dp(ctx, 18), Ui.dp(ctx, 28));

        LinearLayout head = Ui.row(ctx);
        LinearLayout titles = Ui.col(ctx);
        titles.addView(Ui.label(ctx, "训练数据"));
        titles.addView(Ui.title(ctx, "数据"));
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView hist = Icons.action(ctx, com.ironlog.train.R.drawable.ic_calendar, "历史",
                Ui.TEXT2, Ui.SURFACE2);
        hist.setClickable(true);
        hist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.push(new HistoryScreen(act));
            }
        });
        head.addView(hist);
        body.addView(head);

        body.addView(Ui.spacer(ctx, 16));

        // ------------------------------------------------------- overview
        LinearLayout overview = Ui.card(ctx);
        LinearLayout r1 = Ui.row(ctx);
        r1.addView(stat("总训练", String.valueOf(finished.size()), "次"), Ui.weight(ctx, 1));
        r1.addView(stat("总容量", Fmt.vol(Stats.totalVolume(finished)), store.settings.unit), Ui.weight(ctx, 1));
        overview.addView(r1);
        overview.addView(Ui.spacer(ctx, 14));
        LinearLayout r2 = Ui.row(ctx);
        r2.addView(stat("总组数", String.valueOf(Stats.totalSets(finished)), "组"), Ui.weight(ctx, 1));
        r2.addView(stat("连续", String.valueOf(Stats.weekStreak(finished)), "周"), Ui.weight(ctx, 1));
        overview.addView(r2);
        body.addView(overview);

        body.addView(Ui.spacer(ctx, 14));

        body.addView(calendarCard(store, finished));
        body.addView(Ui.spacer(ctx, 14));
        body.addView(plansCard(store));

        body.addView(Ui.spacer(ctx, 14));

        // --------------------------------------------------------- trend
        Map<String, Double> prs = Stats.prMap(finished);
        if (selectedExercise == null || store.exercise(selectedExercise) == null) {
            selectedExercise = null;
            for (String id : prs.keySet()) {
                selectedExercise = id;
                break;
            }
        }

        LinearLayout trendCard = Ui.card(ctx);
        LinearLayout trendHead = Ui.row(ctx);
        trendHead.addView(Ui.sectionTitle(ctx, "力量趋势"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView change = Ui.t(ctx, "切换动作", 12f, Ui.TEXT2);
        Icons.end(change, ctx, com.ironlog.train.R.drawable.ic_chevron_right, Ui.TEXT2, 11, 1);
        change.setClickable(true);
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickExercise();
            }
        });
        trendHead.addView(change);
        trendCard.addView(trendHead);

        String exName = selectedExercise == null ? "暂无动作" : store.exerciseName(selectedExercise);
        trendCard.addView(Ui.spacer(ctx, 2));
        trendCard.addView(Ui.t(ctx, exName + " · 估算 1RM", 12f, Ui.TEXT3));
        trendCard.addView(Ui.spacer(ctx, 8));

        LineChartView line = new LineChartView(ctx);
        line.setAccent(Ui.BLUE);
        if (selectedExercise != null) {
            line.setPoints(Stats.exerciseTrend(finished, selectedExercise));
        }
        trendCard.addView(line, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(trendCard);

        body.addView(Ui.spacer(ctx, 14));

        // ------------------------------------------------ weekly volume
        LinearLayout weekCard = Ui.card(ctx);
        weekCard.addView(Ui.sectionTitle(ctx, "每周容量"));
        weekCard.addView(Ui.t(ctx, "最近 8 周", 11.5f, Ui.TEXT3));
        weekCard.addView(Ui.spacer(ctx, 10));

        int weeks = 8;
        double[] vols = Stats.weeklyVolume(finished, weeks);
        String[] labels = new String[weeks];
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        for (int i = 0; i < weeks; i++) {
            LocalDate ws = monday.minusWeeks(weeks - 1 - i);
            labels[i] = i == weeks - 1 ? "本周" : Fmt.shortDate(Fmt.millis(ws));
        }
        BarChartView bars = new BarChartView(ctx);
        bars.setData(vols, labels);
        weekCard.addView(bars, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(weekCard);

        body.addView(Ui.spacer(ctx, 14));

        // --------------------------------------------------- muscle split
        LinearLayout muscleCard = Ui.card(ctx);
        muscleCard.addView(Ui.sectionTitle(ctx, "肌群分布"));
        muscleCard.addView(Ui.t(ctx, "按容量占比", 11.5f, Ui.TEXT3));
        muscleCard.addView(Ui.spacer(ctx, 12));

        Map<String, String> muscleOf = new LinkedHashMap<>();
        for (Exercise e : store.allExercises()) {
            muscleOf.put(e.id, e.muscle);
        }
        Map<String, Double> dist = Stats.muscleVolume(finished, muscleOf);
        if (dist.isEmpty()) {
            muscleCard.addView(Ui.t(ctx, "还没有数据", 12f, Ui.TEXT3));
        } else {
            double max = 0;
            for (double v : dist.values()) {
                max = Math.max(max, v);
            }
            for (Map.Entry<String, Double> en : dist.entrySet()) {
                muscleCard.addView(muscleBar(en.getKey(), en.getValue(), max));
                muscleCard.addView(Ui.spacer(ctx, 8));
            }
        }
        body.addView(muscleCard);

        body.addView(Ui.spacer(ctx, 14));

        // ------------------------------------------------------------ PRs
        LinearLayout prCard = Ui.card(ctx);
        prCard.addView(Ui.sectionTitle(ctx, "个人记录"));
        prCard.addView(Ui.t(ctx, "按估算 1RM 排序", 11.5f, Ui.TEXT3));
        prCard.addView(Ui.spacer(ctx, 10));
        if (prs.isEmpty()) {
            prCard.addView(Ui.t(ctx, "完成一次训练后这里会出现记录", 12f, Ui.TEXT3));
        } else {
            List<Map.Entry<String, Double>> list = new ArrayList<>(prs.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
                @Override
                public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b) {
                    return Double.compare(b.getValue(), a.getValue());
                }
            });
            int n = Math.min(10, list.size());
            for (int i = 0; i < n; i++) {
                final Map.Entry<String, Double> en = list.get(i);
                LinearLayout row = Ui.row(ctx);
                row.setPadding(0, Ui.dp(ctx, 7), 0, Ui.dp(ctx, 7));
                row.setClickable(true);
                Ui.pressable(row);

                TextView idxView = Ui.num(ctx, String.valueOf(i + 1), 11f,
                        i == 0 ? Ui.ACCENT : Ui.TEXT3);
                idxView.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(ctx, 20),
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                row.addView(idxView);

                TextView name = Ui.t(ctx, store.exerciseName(en.getKey()), 13.5f, Ui.TEXT);
                name.setMaxLines(1);
                name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                row.addView(Ui.num(ctx, Fmt.num(en.getValue()) + " " + store.settings.unit, 13f, Ui.ACCENT));

                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedExercise = en.getKey();
                        rebuild();
                    }
                });
                prCard.addView(row);
                if (i < n - 1) {
                    prCard.addView(Ui.divider(ctx));
                }
            }
        }
        body.addView(prCard);

        addView(Ui.scroll(ctx, body), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // ------------------------------------------------------------- calendar

    private View calendarCard(final Store store, final List<WorkoutSession> finished) {
        LinearLayout card = Ui.card(ctx);

        LinearLayout head = Ui.row(ctx);
        head.addView(Ui.sectionTitle(ctx, "计划日历"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView prev = Icons.button(ctx, com.ironlog.train.R.drawable.ic_back, 15, Ui.TEXT2, 4);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeMonth(-1);
            }
        });
        head.addView(prev);

        TextView monthLabel = Ui.num(ctx, month.getMonthValue() + "月", 13f, Ui.TEXT);
        monthLabel.setPadding(Ui.dp(ctx, 6), 0, Ui.dp(ctx, 6), 0);
        head.addView(monthLabel);

        ImageView next = Icons.button(ctx, com.ironlog.train.R.drawable.ic_chevron_right,
                15, Ui.TEXT2, 4);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeMonth(1);
            }
        });
        head.addView(next);
        card.addView(head);

        card.addView(Ui.spacer(ctx, 8));

        CalendarView cal = new CalendarView(ctx);
        cal.setMonth(month);
        cal.setSelected(selectedDay);

        Map<Integer, Integer> done = new HashMap<>();
        Map<Integer, Integer> planned = new HashMap<>();
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            LocalDate day = month.atDay(d);
            int c = Stats.countOn(finished, day);
            if (c > 0) {
                done.put(d, c);
            }
            int p = 0;
            for (Routine r : store.routines) {
                if (r.onDay(day.getDayOfWeek().getValue())) {
                    p++;
                }
            }
            if (p > 0) {
                planned.put(d, p);
            }
        }
        cal.setCounts(done);
        cal.setPlanned(planned);
        cal.setListener(new CalendarView.Listener() {
            @Override
            public void onDaySelected(LocalDate day) {
                selectedDay = day;
                rebuild();
            }
        });
        card.addView(cal, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // legend
        LinearLayout legend = Ui.row(ctx);
        legend.setPadding(0, Ui.dp(ctx, 8), 0, Ui.dp(ctx, 12));

        View dot = new View(ctx);
        dot.setBackground(Ui.circle(Ui.ACCENT, ctx));
        legend.addView(dot, new LinearLayout.LayoutParams(Ui.dp(ctx, 6), Ui.dp(ctx, 6)));
        TextView l1 = Ui.t(ctx, " 已完成", 10.5f, Ui.TEXT3);
        l1.setPadding(Ui.dp(ctx, 3), 0, Ui.dp(ctx, 14), 0);
        legend.addView(l1);

        View ring = new View(ctx);
        ring.setBackground(Ui.strokeBg(0, Ui.BLUE, 20, 1.4f, ctx));
        legend.addView(ring, new LinearLayout.LayoutParams(Ui.dp(ctx, 7), Ui.dp(ctx, 7)));
        TextView l2 = Ui.t(ctx, " 计划日", 10.5f, Ui.TEXT3);
        l2.setPadding(Ui.dp(ctx, 3), 0, 0, 0);
        legend.addView(l2);

        card.addView(legend);
        card.addView(Ui.divider(ctx));
        card.addView(Ui.spacer(ctx, 10));

        // selected day detail
        int dow = selectedDay.getDayOfWeek().getValue();
        card.addView(Ui.bold(ctx, selectedDay.getMonthValue() + "月"
                + selectedDay.getDayOfMonth() + "日 · " + Fmt.weekday(Fmt.millis(selectedDay)),
                13.5f, Ui.TEXT));
        card.addView(Ui.spacer(ctx, 8));

        boolean any = false;
        for (final Routine r : store.routines) {
            if (!r.onDay(dow)) {
                continue;
            }
            any = true;
            boolean completed = isCompleted(finished, r, selectedDay);
            LinearLayout row = Ui.row(ctx);
            row.setBackground(Ui.bg(Ui.SURFACE2, 10, ctx));
            Ui.pad(row, 12, 9, 10, 9);

            LinearLayout col = Ui.col(ctx);
            col.addView(Ui.t(ctx, r.name, 13f, Ui.TEXT));
            col.addView(Ui.t(ctx, completed ? "今天已完成 ✓" : "计划 " + r.items.size() + " 个动作",
                    10.5f, completed ? Ui.GREEN : Ui.TEXT3));
            row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            if (!completed) {
                TextView go = Icons.action(ctx, com.ironlog.train.R.drawable.ic_play, "开始",
                        Ui.ON_ACCENT, Ui.ACCENT);
                go.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startRoutine(r);
                    }
                });
                row.addView(go);
            }
            card.addView(row);
            card.addView(Ui.spacer(ctx, 7));
        }

        List<WorkoutSession> daySessions = Stats.onDay(finished, selectedDay);
        for (WorkoutSession s : daySessions) {
            any = true;
            LinearLayout row = Ui.row(ctx);
            row.setPadding(0, Ui.dp(ctx, 4), 0, Ui.dp(ctx, 4));
            row.addView(Ui.t(ctx, "· " + Ui.nz(s.name, "训练"), 12.5f, Ui.TEXT2),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(Ui.num(ctx, Fmt.vol(s.volume()) + " " + store.settings.unit, 11.5f, Ui.ACCENT));
            card.addView(row);
        }

        if (!any) {
            card.addView(Ui.t(ctx, "这天没有计划，也没有训练记录", 11.5f, Ui.TEXT3));
        }

        return card;
    }

    /** Moving to another month must not leave the day panel pointing at the old month. */
    private void changeMonth(int delta) {
        month = month.plusMonths(delta);
        if (selectedDay.getYear() != month.getYear()
                || selectedDay.getMonthValue() != month.getMonthValue()) {
            selectedDay = month.atDay(1);
        }
        rebuild();
    }

    private boolean isCompleted(List<WorkoutSession> finished, Routine r, LocalDate day) {        for (WorkoutSession s : finished) {
            if (!day.equals(Fmt.date(s.startedAt))) {
                continue;
            }
            if (r.id.equals(s.routineId) || (s.name != null && s.name.equals(r.name))) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------------------- plans

    private View plansCard(final Store store) {
        LinearLayout card = Ui.card(ctx);
        LinearLayout head = Ui.row(ctx);
        head.addView(Ui.sectionTitle(ctx, "训练计划"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView add = Icons.action(ctx, com.ironlog.train.R.drawable.ic_add, "新建",
                Ui.ACCENT, Ui.ACCENT_SOFT);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRoutine();
            }
        });
        head.addView(add);
        card.addView(head);
        card.addView(Ui.spacer(ctx, 10));

        if (store.routines.isEmpty()) {
            card.addView(Ui.t(ctx, "把常练的动作存成模板，并在上周几，日历里就会出现计划日。",
                    11.5f, Ui.TEXT3));
            return card;
        }

        for (final Routine r : store.routines) {
            LinearLayout row = Ui.row(ctx);
            row.setBackground(Ui.bg(Ui.SURFACE2, 12, ctx));
            Ui.pad(row, 12, 11, 10, 11);
            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    routineMenu(r);
                }
            });

            LinearLayout col = Ui.col(ctx);
            col.addView(Ui.bold(ctx, r.name, 13.5f, Ui.TEXT));
            col.addView(Ui.t(ctx, r.daysLabel() + " · " + r.items.size() + " 个动作 · "
                    + r.plannedSets() + " 组", 10.5f, Ui.TEXT3));
            row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView go = Icons.action(ctx, com.ironlog.train.R.drawable.ic_play, "开始",
                    Ui.ON_ACCENT, Ui.ACCENT);
            go.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startRoutine(r);
                }
            });
            row.addView(go);

            card.addView(row);
            card.addView(Ui.spacer(ctx, 8));
        }
        return card;
    }

    private void routineMenu(final Routine r) {
        String[] items = {"开始训练", "编辑计划", "删除计划"};
        Dialogs.list(ctx, r.name, null, items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            startRoutine(r);
                        } else if (which == 1) {
                            act.push(new RoutineEditScreen(act, r.id));
                        } else {
                            Dialogs.confirm(ctx, "删除计划", "「" + r.name + "」会被删除，历史记录不受影响。",
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            Store.get(ctx).routines.remove(r);
                                            Store.get(ctx).save();
                                            rebuild();
                                        }
                                    });
                        }
                    }
                });
    }

    private void createRoutine() {
        String[] options = {"手动创建", "从模板生成（按一周几练）"};
        Dialogs.list(ctx, "新建计划", null, options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            manualCreate();
                        } else {
                            templateDialog();
                        }
                    }
                });
    }

    private void manualCreate() {
        Dialogs.prompt(ctx, "新建计划", "例如：推日 A / 腿日", "", new Dialogs.TextCallback() {
            @Override
            public void onText(String text) {
                if (text.isEmpty()) {
                    act.toast("名称不能为空");
                    return;
                }
                Store store = Store.get(ctx);
                Routine r = new Routine();
                r.id = "r_" + Store.newId();
                r.name = text;
                r.updatedAt = System.currentTimeMillis();
                store.routines.add(r);
                store.save();
                act.push(new RoutineEditScreen(act, r.id));
                rebuild();
            }
        });
    }

    private void templateDialog() {
        String[] items = new String[com.ironlog.train.data.Templates.NAMES.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = com.ironlog.train.data.Templates.NAMES[i];
        }
        // setItems + setMessage would drop the list on this platform, so the hint
        // travels in the custom title instead.
        Dialogs.list(ctx, "选择训练分化",
                "按每周训练频率生成对应计划，动作和组数已经填好，之后可以自己改。",
                items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        int n = com.ironlog.train.data.Templates.apply(Store.get(ctx), which);
                        act.toast("已生成 " + n + " 个计划：" + com.ironlog.train.data.Templates.DESCS[which]);
                        rebuild();
                    }
                });
    }

    private void startRoutine(final Routine r) {
        Store store = Store.get(ctx);
        if (store.activeSession() != null) {
            act.toast("已有进行中的训练");
            return;
        }
        if (r.items.isEmpty()) {
            act.toast("这个计划还没有动作");
            return;
        }
        WorkoutSession s = new WorkoutSession();
        s.id = Store.newId();
        s.name = r.name;
        s.routineId = r.id;
        s.startedAt = System.currentTimeMillis();
        for (RoutineExercise ri : r.items) {
            SessionExercise se = new SessionExercise(ri.exerciseId);
            se.restSec = ri.restSec;
            for (int i = 0; i < ri.sets; i++) {
                se.sets.add(new SetEntry(ri.weight, ri.reps));
            }
            s.exercises.add(se);
        }
        store.addSession(s);
        act.push(new WorkoutScreen(act, s.id));
    }

    // --------------------------------------------------------------- pieces

    private View stat(String label, String value, String unit) {
        LinearLayout c = Ui.col(ctx);
        c.addView(Ui.label(ctx, label));
        LinearLayout line = Ui.row(ctx);
        line.addView(Ui.num(ctx, value, 20f, Ui.TEXT));
        line.addView(Ui.t(ctx, " " + unit, 10f, Ui.TEXT3));
        c.addView(line);
        return c;
    }

    private View muscleBar(String name, double value, double max) {
        LinearLayout row = Ui.row(ctx);
        TextView label = Ui.t(ctx, name, 12f, Ui.TEXT2);
        label.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(ctx, 44),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(label);

        LinearLayout track = Ui.row(ctx);
        track.setBackground(Ui.bg(Ui.SURFACE2, 20, ctx));
        View fill = new View(ctx);
        fill.setBackground(Ui.bg(Ui.ACCENT, 20, ctx));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, (float) Math.max(0.001, value / max));
        track.addView(fill, flp);
        View rest = new View(ctx);
        track.addView(rest, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, (float) Math.max(0.001, 1 - value / max)));

        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, Ui.dp(ctx, 8), 1f);
        tlp.rightMargin = Ui.dp(ctx, 8);
        tlp.gravity = Gravity.CENTER_VERTICAL;
        row.addView(track, tlp);

        row.addView(Ui.t(ctx, Fmt.vol(value), 11f, Ui.TEXT3));
        return row;
    }

    private void pickExercise() {
        final Store store = Store.get(ctx);
        Map<String, Double> prs = Stats.prMap(store.finishedSessions());
        if (prs.isEmpty()) {
            act.toast("还没有可统计的动作");
            return;
        }
        final List<String> ids = new ArrayList<>(prs.keySet());
        String[] names = new String[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            names[i] = store.exerciseName(ids.get(i));
        }
        Dialogs.list(ctx, "选择动作", null, names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        selectedExercise = ids.get(which);
                        rebuild();
                    }
                });
    }

    @Override
    public void refresh() {
        rebuild();
    }
}
