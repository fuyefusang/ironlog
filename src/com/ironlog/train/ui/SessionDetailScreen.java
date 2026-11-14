package com.ironlog.train.ui;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ironlog.train.data.SessionExercise;
import com.ironlog.train.data.SetEntry;
import com.ironlog.train.data.Store;
import com.ironlog.train.data.WorkoutSession;
import com.ironlog.train.logic.Fmt;
import com.ironlog.train.logic.Stats;

import java.time.LocalDate;

public class SessionDetailScreen extends Screen {

    private final String sessionId;
    private WorkoutSession session;

    public SessionDetailScreen(MainActivity a, String sessionId) {
        super(a);
        this.sessionId = sessionId;
    }

    public String sessionId() {
        return sessionId;
    }

    @Override
    protected void build() {
        Store store = Store.get(ctx);
        session = null;
        for (WorkoutSession s : store.sessions) {
            if (sessionId.equals(s.id)) {
                session = s;
            }
        }
        if (session == null) {
            TextView tv = Ui.t(ctx, "记录不存在", 14f, Ui.TEXT2);
            tv.setGravity(Gravity.CENTER);
            addView(tv);
            return;
        }

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

        LocalDate d = Fmt.date(session.startedAt);
        LinearLayout titles = Ui.col(ctx);
        titles.addView(Ui.label(ctx, d.getYear() + "年" + Fmt.weekday(session.startedAt)));
        titles.addView(Ui.title(ctx, d.getMonthValue() + "月" + d.getDayOfMonth() + "日"));
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView menu = Icons.button(ctx, com.ironlog.train.R.drawable.ic_more, 20, Ui.TEXT2, 6);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuDialog();
            }
        });
        head.addView(menu);
        body.addView(head);

        body.addView(Ui.spacer(ctx, 16));

        // -------------------------------------------------------- summary
        LinearLayout card = Ui.card(ctx);
        card.addView(Ui.bold(ctx, Ui.nz(session.name, "训练"), 15.5f, Ui.TEXT));
        card.addView(Ui.spacer(ctx, 12));
        LinearLayout stats = Ui.row(ctx);
        stats.addView(stat("时长", Fmt.duration(session.durationSec()), ""), Ui.weight(ctx, 1));
        stats.addView(stat("容量", Fmt.vol(session.volume()), store.settings.unit), Ui.weight(ctx, 1));
        stats.addView(stat("组数", String.valueOf(session.totalSets()), ""), Ui.weight(ctx, 1));
        stats.addView(stat("动作", String.valueOf(session.exercises.size()), ""), Ui.weight(ctx, 1));
        card.addView(stats);

        if (session.note != null && !session.note.isEmpty()) {
            card.addView(Ui.spacer(ctx, 12));
            card.addView(Ui.divider(ctx));
            card.addView(Ui.spacer(ctx, 10));
            card.addView(Ui.t(ctx, "备注：" + session.note, 12f, Ui.TEXT2));
        }
        body.addView(card);

        body.addView(Ui.spacer(ctx, 16));

        // ------------------------------------------------------- exercises
        for (SessionExercise se : session.exercises) {
            body.addView(exerciseBlock(se));
            body.addView(Ui.spacer(ctx, 8));
        }

        if (session.exercises.isEmpty()) {
            LinearLayout empty = Ui.card(ctx);
            TextView e = Ui.t(ctx, "这次训练没有记录动作", 12.5f, Ui.TEXT3);
            e.setGravity(Gravity.CENTER);
            empty.addView(e);
            body.addView(empty);
        }

        body.addView(Ui.spacer(ctx, 18));

        if (session.isActive()) {
            TextView resume = Ui.primaryButton(ctx, "继续这次训练");
            resume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    act.push(new WorkoutScreen(act, session.id));
                }
            });
            body.addView(resume, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            body.addView(Ui.spacer(ctx, 10));
        }

        TextView del = Icons.button(ctx, com.ironlog.train.R.drawable.ic_trash,
                "删除这条记录", Ui.RED, 0x1FE2605F);
        del.setTextSize(15f);
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialogs.confirm(ctx, "删除记录", "删除后无法恢复，确定吗？", new Runnable() {
                    @Override
                    public void run() {
                        Store.get(ctx).removeSession(session);
                        act.pop();
                    }
                });
            }
        });
        body.addView(del, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addView(Ui.scroll(ctx, body), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private View stat(String label, String value, String unit) {
        LinearLayout c = Ui.col(ctx);
        c.addView(Ui.label(ctx, label));
        LinearLayout line = Ui.row(ctx);
        line.addView(Ui.num(ctx, value, 16f, Ui.TEXT));
        if (!unit.isEmpty()) {
            line.addView(Ui.t(ctx, " " + unit, 9.5f, Ui.TEXT3));
        }
        c.addView(line);
        return c;
    }

    private View exerciseBlock(SessionExercise se) {
        Store store = Store.get(ctx);
        LinearLayout card = Ui.col(ctx);
        card.setBackground(Ui.bg(Ui.SURFACE, 14, ctx));
        Ui.pad(card, 14, 13, 14, 13);

        LinearLayout top = Ui.row(ctx);
        top.addView(Ui.bold(ctx, store.exerciseName(se.exerciseId), 14.5f, Ui.TEXT),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        double e1 = Stats.bestE1rm(se);
        if (e1 > 0) {
            top.addView(Ui.pill(ctx, "e1RM " + Fmt.num(e1), Ui.TEXT2, Ui.SURFACE2));
        }
        card.addView(top);

        if (se.note != null && !se.note.isEmpty()) {
            card.addView(Ui.t(ctx, se.note, 11.5f, Ui.TEXT3));
        }

        card.addView(Ui.spacer(ctx, 8));

        int idx = 1;
        for (SetEntry s : se.sets) {
            LinearLayout row = Ui.row(ctx);
            String tag = s.type == SetEntry.WARMUP ? "热身" : (s.type == SetEntry.DROP ? "递减" : String.valueOf(idx));
            TextView no = Ui.num(ctx, tag, 11.5f, s.type == SetEntry.WARMUP ? Ui.BLUE : Ui.TEXT3);
            no.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(ctx, 34),
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(no);

            String line = s.weight > 0
                    ? Fmt.num(s.weight) + " " + store.settings.unit + " × " + s.reps
                    : s.reps + " 次";
            row.addView(Ui.num(ctx, line, 13f, s.done ? Ui.TEXT : Ui.TEXT3),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            if (s.rpe > 0) {
                row.addView(Ui.t(ctx, "RPE " + Fmt.num(s.rpe), 10.5f, Ui.TEXT3));
            }
            card.addView(row);
            if (s.type == SetEntry.WARMUP || s.type == SetEntry.DROP) {
                continue;
            }
            idx++;
        }

        if (se.sets.isEmpty()) {
            card.addView(Ui.t(ctx, "没有记录", 11.5f, Ui.TEXT3));
        }
        return card;
    }

    private void menuDialog() {
        String[] items = {"编辑备注", "删除这条记录"};
        new android.app.AlertDialog.Builder(ctx)
                .setItems(items, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        if (which == 0) {
                            Dialogs.prompt(ctx, "训练备注", "今天状态如何",
                                    session.note, new Dialogs.TextCallback() {
                                        @Override
                                        public void onText(String text) {
                                            session.note = text;
                                            Store.get(ctx).save();
                                            rebuild();
                                        }
                                    });
                        } else {
                            Dialogs.confirm(ctx, "删除记录", "删除后无法恢复，确定吗？", new Runnable() {
                                @Override
                                public void run() {
                                    Store.get(ctx).removeSession(session);
                                    act.pop();
                                }
                            });
                        }
                    }
                })
                .show();
    }
}
