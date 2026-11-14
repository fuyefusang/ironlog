package com.ironlog.train.ui;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ironlog.train.data.Routine;
import com.ironlog.train.data.RoutineExercise;
import com.ironlog.train.data.SessionExercise;
import com.ironlog.train.data.SetEntry;
import com.ironlog.train.data.Store;
import com.ironlog.train.data.WorkoutSession;
import com.ironlog.train.logic.Fmt;
import com.ironlog.train.logic.Stats;

import java.time.LocalDate;
import java.util.List;

public class HomeScreen extends Screen {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView activeTimer;
    private WorkoutSession activeRef;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (activeTimer != null && activeRef != null) {
                activeTimer.setText(activeRef.startedAt == 0L
                        ? "未开始" : Fmt.duration(activeRef.durationSec()));
            }
            handler.postDelayed(this, 1000);
        }
    };

    public HomeScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected void build() {
        final Store store = Store.get(ctx);
        List<WorkoutSession> finished = store.finishedSessions();
        WorkoutSession active = store.activeSession();

        LinearLayout body = Ui.col(ctx);
        body.setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 20), Ui.dp(ctx, 18), Ui.dp(ctx, 28));

        // ---------------------------------------------------------- header
        LinearLayout head = Ui.row(ctx);
        LinearLayout headText = Ui.col(ctx);
        LocalDate today = LocalDate.now();
        headText.addView(Ui.label(ctx, Fmt.weekday(System.currentTimeMillis()) + " · " + today.getYear()));
        TextView date = Ui.title(ctx, (today.getMonthValue()) + "月" + today.getDayOfMonth() + "日");
        headText.addView(date);
        head.addView(headText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int streak = Stats.weekStreak(finished);
        if (streak > 0) {
            TextView badge = Ui.pill(ctx, "连续 " + streak + " 周", Ui.ACCENT, Ui.ACCENT_SOFT);
            head.addView(Icons.start(badge, ctx, com.ironlog.train.R.drawable.ic_flame,
                    Ui.ACCENT, 12, 4));
        }
        body.addView(head);

        body.addView(Ui.spacer(ctx, 18));

        // ------------------------------------------------------ this week
        double[] weekVol = Stats.weeklyVolume(finished, 1);
        int[] weekCnt = Stats.weeklyCount(finished, 1);
        LinearLayout card = Ui.card(ctx);
        LinearLayout stats = Ui.row(ctx);
        stats.addView(statBlock("本周训练", String.valueOf(weekCnt[0]), "次"), Ui.weight(ctx, 1));
        stats.addView(statBlock("本周容量", Fmt.vol(weekVol[0]), store.settings.unit), Ui.weight(ctx, 1));
        stats.addView(statBlock("累计训练", String.valueOf(finished.size()), "次"), Ui.weight(ctx, 1));
        card.addView(stats);
        body.addView(card);

        body.addView(Ui.spacer(ctx, 14));

        // ---------------------------------------------------- active card
        if (active != null) {
            activeRef = active;
            LinearLayout ac = Ui.col(ctx);
            ac.setBackground(Ui.strokeBg(Ui.ACCENT_SOFT, Ui.ACCENT, 16, 1.2f, ctx));
            Ui.pad(ac, 16, 15, 16, 15);

            LinearLayout line = Ui.row(ctx);
            LinearLayout left = Ui.col(ctx);
            left.addView(Ui.label(ctx, "训练进行中"));
            TextView name = Ui.bold(ctx, Ui.nz(active.name, "训练"), 17f, Ui.TEXT);
            left.addView(name);
            line.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            activeTimer = Ui.num(ctx, active.startedAt == 0L
                    ? "未开始" : Fmt.duration(active.durationSec()), 20f, Ui.ACCENT);
            line.addView(activeTimer);
            ac.addView(line);

            ac.addView(Ui.spacer(ctx, 6));
            int doneSets = active.totalSets();
            String state = active.isPaused() ? "已暂停 · " : "";
            TextView sub = Ui.t(ctx, state + active.exercises.size() + " 个动作 · "
                    + doneSets + " 组已完成", 12.5f,
                    active.isPaused() ? Ui.TEXT3 : Ui.TEXT2);
            ac.addView(sub);
            ac.addView(Ui.spacer(ctx, 12));

            TextView resume = Icons.button(ctx, com.ironlog.train.R.drawable.ic_play,
                    "继续训练", Ui.ON_ACCENT, Ui.ACCENT);
            resume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    act.push(new WorkoutScreen(act, activeRef.id));
                }
            });
            ac.addView(resume, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            body.addView(ac);
            body.addView(Ui.spacer(ctx, 14));
            handler.removeCallbacks(tick);
            handler.post(tick);
        } else {
            activeTimer = null;
            activeRef = null;
        }

        // ---------------------------------------------------- start button
        if (active == null) {
            LinearLayout startRow = Ui.row(ctx);

            TextView start = Icons.button(ctx, com.ironlog.train.R.drawable.ic_play,
                    "开始训练", Ui.ON_ACCENT, Ui.ACCENT);
            start.setTextSize(16f);
            start.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 17), Ui.dp(ctx, 16), Ui.dp(ctx, 17));
            start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Store.get(ctx).activeSession() != null) {
                        act.toast("先结束当前训练");
                        return;
                    }
                    startBlank();
                }
            });
            startRow.addView(start, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView plan = Icons.ghost(ctx, com.ironlog.train.R.drawable.ic_calendar, "计划");
            plan.setTextSize(13f);
            plan.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 17), Ui.dp(ctx, 12), Ui.dp(ctx, 17));
            plan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    act.switchTab(1);
                }
            });
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            plp.leftMargin = Ui.dp(ctx, 8);
            startRow.addView(plan, plp);

            body.addView(startRow, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // ----------------------------------------------------- quick start
        List<Routine> routines = store.routines;
        if (!routines.isEmpty()) {
            body.addView(Ui.spacer(ctx, 22));
            LinearLayout sectionHead = Ui.row(ctx);
            sectionHead.addView(Ui.sectionTitle(ctx, "快速开始"),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView all = Ui.t(ctx, "全部计划", 12.5f, Ui.TEXT2);
            Icons.end(all, ctx, com.ironlog.train.R.drawable.ic_chevron_right, Ui.TEXT2, 12, 2);
            all.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    act.switchTab(1);
                }
            });
            sectionHead.addView(all);
            body.addView(sectionHead);
            body.addView(Ui.spacer(ctx, 10));

            HorizontalScrollView hs = new HorizontalScrollView(ctx);
            hs.setHorizontalScrollBarEnabled(false);
            hs.setClipToPadding(false);
            LinearLayout rail = Ui.row(ctx);
            rail.setPadding(0, 0, Ui.dp(ctx, 18), 0);
            for (final Routine r : routines) {
                rail.addView(routineChip(r));
            }
            hs.addView(rail, new android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            body.addView(hs, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        // -------------------------------------------------------- history
        body.addView(Ui.spacer(ctx, 22));
        LinearLayout hHead = Ui.row(ctx);
        hHead.addView(Ui.sectionTitle(ctx, "最近训练"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView cal = Ui.t(ctx, "日历", 12.5f, Ui.TEXT2);
        Icons.start(cal, ctx, com.ironlog.train.R.drawable.ic_calendar, Ui.TEXT2, 13, 4);
        Icons.end(cal, ctx, com.ironlog.train.R.drawable.ic_chevron_right, Ui.TEXT2, 11, 1);
        cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.push(new HistoryScreen(act));
            }
        });
        hHead.addView(cal);
        body.addView(hHead);
        body.addView(Ui.spacer(ctx, 10));

        if (finished.isEmpty()) {
            LinearLayout empty = Ui.card(ctx);
            empty.setGravity(Gravity.CENTER);
            TextView e1 = Ui.t(ctx, "还没有训练记录", 13.5f, Ui.TEXT2);
            e1.setGravity(Gravity.CENTER);
            empty.addView(e1);
            body.addView(empty);
        } else {
            int n = Math.min(6, finished.size());
            for (int i = 0; i < n; i++) {
                body.addView(sessionRow(finished.get(i)));
                if (i < n - 1) {
                    body.addView(Ui.spacer(ctx, 8));
                }
            }
        }

        addView(Ui.scroll(ctx, body), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private View statBlock(String label, String value, String unit) {
        LinearLayout c = Ui.col(ctx);
        c.addView(Ui.label(ctx, label));
        LinearLayout line = Ui.row(ctx);
        line.addView(Ui.num(ctx, value, 20f, Ui.TEXT));
        TextView u = Ui.t(ctx, " " + unit, 10.5f, Ui.TEXT3);
        line.addView(u);
        c.addView(line);
        return c;
    }

    private View routineChip(final Routine r) {
        LinearLayout c = Ui.col(ctx);
        c.setBackground(Ui.bg(Ui.SURFACE, 14, ctx));
        Ui.pad(c, 14, 12, 14, 12);
        c.setClickable(true);
        Ui.ripple(c, 0x22FFFFFF);

        TextView name = Ui.bold(ctx, r.name, 14f, Ui.TEXT);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setMaxWidth(Ui.dp(ctx, 170));
        c.addView(name);
        c.addView(Ui.spacer(ctx, 4));
        c.addView(Ui.t(ctx, r.items.size() + " 个动作 · " + r.plannedSets() + " 组", 11.5f, Ui.TEXT2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(ctx, 10);
        c.setLayoutParams(lp);

        c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Store.get(ctx).activeSession() != null) {
                    act.toast("先结束当前训练");
                    return;
                }
                startFromRoutine(r);
            }
        });
        return c;
    }

    private View sessionRow(final WorkoutSession s) {
        LinearLayout row = Ui.row(ctx);
        row.setBackground(Ui.bg(Ui.SURFACE, 14, ctx));
        Ui.pad(row, 14, 13, 14, 13);
        row.setClickable(true);
        Ui.ripple(row, 0x1FFFFFFF);

        LinearLayout left = Ui.col(ctx);
        left.addView(Ui.bold(ctx, Ui.nz(s.name, "训练"), 14.5f, Ui.TEXT));
        String sub = Fmt.shortDate(s.startedAt) + " · " + Fmt.duration(s.durationSec())
                + " · " + s.totalSets() + " 组";
        left.addView(Ui.t(ctx, sub, 11.5f, Ui.TEXT3));

        String exercises = exerciseSummary(s);
        if (!exercises.isEmpty()) {
            TextView exLine = Ui.t(ctx, exercises, 11f, Ui.TEXT2);
            exLine.setMaxLines(1);
            exLine.setEllipsize(android.text.TextUtils.TruncateAt.END);
            exLine.setPadding(0, Ui.dp(ctx, 3), 0, 0);
            left.addView(exLine);
        }
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = Ui.col(ctx);
        right.setGravity(Gravity.END);
        TextView vol = Ui.num(ctx, Fmt.vol(s.volume()), 14f, Ui.ACCENT);
        vol.setGravity(Gravity.END);
        right.addView(vol);
        TextView unit = Ui.t(ctx, Store.get(ctx).settings.unit + " 容量", 10f, Ui.TEXT3);
        unit.setGravity(Gravity.END);
        right.addView(unit);
        // Must not be MATCH_PARENT here: in a horizontal row that would squeeze `left` to 0.
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.leftMargin = Ui.dp(ctx, 10);
        row.addView(right, rlp);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.push(new SessionDetailScreen(act, s.id));
            }
        });
        return row;
    }

    /** "卧推 4 组 · 划船 3 组 …" so the row is not just a summary line. */
    private String exerciseSummary(WorkoutSession s) {
        if (s.exercises == null || s.exercises.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (com.ironlog.train.data.SessionExercise se : s.exercises) {
            if (se.doneSets() == 0 && se.sets.isEmpty()) {
                continue;
            }
            if (shown >= 3) {
                sb.append(" 等 ").append(s.exercises.size()).append(" 个动作");
                break;
            }
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(Store.get(ctx).exerciseName(se.exerciseId))
                    .append(" ").append(se.doneSets()).append(" 组");
            shown++;
        }
        return sb.toString();
    }

    private void startBlank() {
        Store store = Store.get(ctx);
        WorkoutSession s = new WorkoutSession();
        s.id = Store.newId();
        s.name = "训练";
        // Timer starts only once the first exercise is added.
        s.startedAt = 0L;
        store.addSession(s);
        act.push(new WorkoutScreen(act, s.id));
    }

    private void startFromRoutine(Routine r) {
        Store store = Store.get(ctx);
        WorkoutSession s = new WorkoutSession();
        s.id = Store.newId();
        s.name = r.name;
        s.routineId = r.id;
        s.startedAt = System.currentTimeMillis();
        for (RoutineExercise ri : r.items) {
            SessionExercise se = new SessionExercise(ri.exerciseId);
            se.restSec = ri.restSec;
            se.note = ri.note;
            for (int i = 0; i < ri.sets; i++) {
                se.sets.add(new SetEntry(ri.weight, ri.reps));
            }
            s.exercises.add(se);
        }
        store.addSession(s);
        act.push(new WorkoutScreen(act, s.id));
    }

    @Override
    public void onShown() {
        rebuild();
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (activeRef != null) {
            handler.removeCallbacks(tick);
            handler.post(tick);
        }
    }
}
