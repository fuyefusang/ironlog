package com.ironlog.train.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ironlog.train.data.Exercise;
import com.ironlog.train.data.SessionExercise;
import com.ironlog.train.data.SetEntry;
import com.ironlog.train.data.Store;
import com.ironlog.train.data.WorkoutSession;
import com.ironlog.train.logic.Fmt;

import java.util.List;

/** The live training log: sets, weights, reps and the rest timer. */
public class WorkoutScreen extends Screen {

    private final String sessionId;
    private WorkoutSession session;

    private ScrollView scroll;
    private LinearLayout body;
    private TextView timerView;

    private LinearLayout restBar;
    private TextView restText;
    private long restEndAt;
    private boolean resting;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            if (timerView != null && session != null) {
                if (session.startedAt == 0L) {
                    timerView.setText("未开始 · 选好动作后点「开始训练」");
                    timerView.setTextColor(Ui.TEXT3);
                } else if (session.isPaused()) {
                    timerView.setText("已暂停 · " + Fmt.duration(session.durationSec()));
                    timerView.setTextColor(Ui.TEXT3);
                } else {
                    timerView.setText(Fmt.duration(session.durationSec()));
                    timerView.setTextColor(Ui.ACCENT);
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable restTick = new Runnable() {
        @Override
        public void run() {
            if (!resting) {
                return;
            }
            long remainMs = restEndAt - System.currentTimeMillis();
            if (remainMs <= 0) {
                stopRest(true);
                return;
            }
            long s = (remainMs + 999) / 1000;
            if (restText != null) {
                restText.setText("休息 " + Fmt.clock(s));
            }
            handler.postDelayed(this, 200);
        }
    };

    public WorkoutScreen(MainActivity a, String sessionId) {
        super(a);
        this.sessionId = sessionId;
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
            TextView tv = Ui.t(ctx, "训练记录不存在", 14f, Ui.TEXT2);
            tv.setGravity(Gravity.CENTER);
            addView(tv);
            return;
        }

        if (store.settings.keepAwake) {
            act.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            act.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // ------------------------------------------------------- fixed header
        LinearLayout header = Ui.row(ctx);
        header.setBackgroundColor(Ui.NAV);
        header.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12));

        ImageView back = Icons.button(ctx, com.ironlog.train.R.drawable.ic_back, 22, Ui.TEXT, 6);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.pop();
            }
        });
        header.addView(back);

        LinearLayout titleBox = Ui.col(ctx);
        titleBox.setClickable(true);
        TextView name = Ui.bold(ctx, Ui.nz(session.name, "训练"), 16f, Ui.TEXT);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleBox.addView(name);
        timerView = Ui.num(ctx, Fmt.duration(session.durationSec()), 12f, Ui.ACCENT);
        titleBox.addView(timerView);
        titleBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialogs.prompt(ctx, "训练名称", "例如：推日 A", session.name, new Dialogs.TextCallback() {
                    @Override
                    public void onText(String text) {
                        session.name = text.isEmpty() ? "训练" : text;
                        Store.get(ctx).save();
                        rebuild();
                    }
                });
            }
        });
        header.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final ImageView pause = Icons.button(ctx,
                session.isPaused() ? com.ironlog.train.R.drawable.ic_play
                        : com.ironlog.train.R.drawable.ic_pause,
                19, session.isPaused() ? Ui.ACCENT : Ui.TEXT2, 7);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePause();
            }
        });
        header.addView(pause);

        ImageView more = Icons.button(ctx, com.ironlog.train.R.drawable.ic_more, 20, Ui.TEXT2, 8);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sessionMenu();
            }
        });
        header.addView(more);

        TextView finish = Ui.button(ctx, "完成", Ui.ON_ACCENT, Ui.ACCENT);
        finish.setTextSize(13.5f);
        finish.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 8));
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishWorkout();
            }
        });
        header.addView(finish);

        android.widget.FrameLayout.LayoutParams hlp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 62));
        hlp.gravity = Gravity.TOP;
        addView(header, hlp);

        // ------------------------------------------------------------ content
        body = Ui.col(ctx);
        body.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 96));

        if (session.exercises.isEmpty()) {
            LinearLayout empty = Ui.card(ctx);
            empty.setGravity(Gravity.CENTER);
            TextView e = Ui.t(ctx, "点击下方「添加动作」开始记录", 13f, Ui.TEXT2);
            e.setGravity(Gravity.CENTER);
            empty.addView(e);
            body.addView(empty);
        }

        for (int i = 0; i < session.exercises.size(); i++) {
            body.addView(exerciseCard(session.exercises.get(i)));
            body.addView(Ui.spacer(ctx, 10));
        }

        scroll = Ui.scroll(ctx, body);
        android.widget.FrameLayout.LayoutParams slp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        slp.topMargin = Ui.dp(ctx, 62);
        slp.bottomMargin = Ui.dp(ctx, 76);
        addView(scroll, slp);

        // --------------------------------------------------------- bottom bar
        LinearLayout bottom = Ui.row(ctx);
        bottom.setBackgroundColor(Ui.NAV);
        bottom.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 10), Ui.dp(ctx, 16), Ui.dp(ctx, 10));

        TextView add = Icons.button(ctx, com.ironlog.train.R.drawable.ic_add,
                "添加动作", Ui.ON_ACCENT, Ui.ACCENT);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickExercises();
            }
        });

        if (session.startedAt == 0L) {
            TextView start = Icons.button(ctx, com.ironlog.train.R.drawable.ic_play,
                    "开始训练", Ui.ON_ACCENT, Ui.ACCENT);
            start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startTraining();
                }
            });
            LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(0, Ui.dp(ctx, 48), 1f);
            startLp.rightMargin = Ui.dp(ctx, 8);
            bottom.addView(start, startLp);
            bottom.addView(add, new LinearLayout.LayoutParams(0, Ui.dp(ctx, 48), 1f));
        } else {
            bottom.addView(add, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 48)));
        }

        android.widget.FrameLayout.LayoutParams blp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.gravity = Gravity.BOTTOM;
        addView(bottom, blp);

        // ---------------------------------------------------------- rest timer
        restBar = Ui.row(ctx);
        restBar.setBackground(Ui.strokeBg(Ui.ACCENT_SOFT, Ui.ACCENT, 14, 1, ctx));
        Ui.pad(restBar, 16, 9, 12, 9);
        restText = Ui.num(ctx, "休息", 14f, Ui.ACCENT);
        restBar.addView(restText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView skip = Ui.t(ctx, "跳过", 13f, Ui.TEXT2);
        skip.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 4), Ui.dp(ctx, 6), Ui.dp(ctx, 4));
        skip.setClickable(true);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRest(false);
            }
        });
        restBar.addView(skip);

        android.widget.FrameLayout.LayoutParams rlp = new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.gravity = Gravity.BOTTOM;
        rlp.bottomMargin = Ui.dp(ctx, 78);
        rlp.leftMargin = Ui.dp(ctx, 16);
        rlp.rightMargin = Ui.dp(ctx, 16);
        addView(restBar, rlp);
        restBar.setVisibility(resting ? View.VISIBLE : View.GONE);

        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
        if (resting) {
            handler.removeCallbacks(restTick);
            handler.post(restTick);
        }
    }

    // ------------------------------------------------------------- exercise

    private View exerciseCard(final SessionExercise se) {
        LinearLayout card = Ui.col(ctx);
        card.setBackground(Ui.bg(Ui.SURFACE, 16, ctx));
        Ui.pad(card, 14, 12, 14, 12);

        String exName = Store.get(ctx).exerciseName(se.exerciseId);
        Exercise meta = Store.get(ctx).exercise(se.exerciseId);

        LinearLayout top = Ui.row(ctx);
        LinearLayout titles = Ui.col(ctx);
        titles.addView(Ui.bold(ctx, exName, 15f, Ui.TEXT));
        if (meta != null) {
            titles.addView(Ui.t(ctx, meta.muscle + " · " + meta.equipment, 11f, Ui.TEXT3));
        }
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // previous performance hint
        SessionExercise last = Store.get(ctx).lastEntryFor(se.exerciseId);
        if (last != null) {
            SetEntry best = null;
            for (SetEntry s : last.sets) {
                if (s.done && s.type != SetEntry.WARMUP && (best == null || s.weight > best.weight)) {
                    best = s;
                }
            }
            if (best != null) {
                top.addView(Ui.pill(ctx, "上次 " + Fmt.num(best.weight) + "×" + best.reps, Ui.TEXT2, Ui.SURFACE2));
            }
        }

        ImageView menu = Icons.button(ctx, com.ironlog.train.R.drawable.ic_more, 17, Ui.TEXT3, 6);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exerciseMenu(se);
            }
        });
        top.addView(menu);
        card.addView(top);

        card.addView(Ui.spacer(ctx, 8));

        for (int i = 0; i < se.sets.size(); i++) {
            card.addView(setRow(se, se.sets.get(i), i));
            if (i < se.sets.size() - 1) {
                card.addView(Ui.spacer(ctx, 6));
            }
        }

        TextView addSet = Ui.t(ctx, "添加一组", 13f, Ui.ACCENT);
        Icons.start(addSet, ctx, com.ironlog.train.R.drawable.ic_add, Ui.ACCENT, 13, 4);
        addSet.setPadding(0, Ui.dp(ctx, 12), 0, Ui.dp(ctx, 2));
        addSet.setClickable(true);
        addSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetEntry lastSet = se.sets.isEmpty() ? new SetEntry() : se.sets.get(se.sets.size() - 1);
                SetEntry n = new SetEntry(lastSet.weight, lastSet.reps);
                n.type = SetEntry.NORMAL;
                se.sets.add(n);
                Store.get(ctx).save();
                softRebuild();
            }
        });
        card.addView(addSet);

        return card;
    }

    private View setRow(final SessionExercise se, final SetEntry set, final int index) {
        LinearLayout row = Ui.row(ctx);
        row.setPadding(0, Ui.dp(ctx, 2), 0, Ui.dp(ctx, 2));

        final TextView no = Ui.num(ctx, setLabel(set, index), 12f,
                set.type == SetEntry.WARMUP ? Ui.BLUE : Ui.TEXT3);
        no.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(Ui.dp(ctx, 26),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        no.setLayoutParams(nlp);
        no.setClickable(true);
        no.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setTypeMenu(se, set);
                return true;
            }
        });
        row.addView(no);

        final EditText weight = Ui.numberInput(ctx, "");
        weight.setText(set.weight > 0 ? Fmt.num(set.weight) : "");
        weight.setTextSize(14f);
        weight.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    weight.setSelection(weight.getText().length());
                }
            }
        });
        weight.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                set.weight = Dialogs.parse(s.toString());
                Store.get(ctx).save();
            }
        });
        row.addView(weight, new LinearLayout.LayoutParams(0, Ui.dp(ctx, 42), 1f));

        row.addView(unitLabel("kg"));
        row.addView(Ui.gap(ctx, 6));

        final EditText reps = Ui.numberInput(ctx, "");
        reps.setText(set.reps > 0 ? String.valueOf(set.reps) : "");
        reps.setTextSize(14f);
        reps.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    reps.setSelection(reps.getText().length());
                }
            }
        });
        reps.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                set.reps = (int) Dialogs.parse(s.toString());
                Store.get(ctx).save();
            }
        });
        row.addView(reps, new LinearLayout.LayoutParams(0, Ui.dp(ctx, 42), 1f));

        row.addView(unitLabel("次"));
        row.addView(Ui.gap(ctx, 8));

        final ImageView check = new ImageView(ctx);
        check.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        applyCheckStyle(check, set.done);
        check.setClickable(true);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set.done = !set.done;
                applyCheckStyle(check, set.done);
                weight.setTextColor(set.done ? Ui.TEXT : Ui.TEXT2);
                reps.setTextColor(set.done ? Ui.TEXT : Ui.TEXT2);
                Store.get(ctx).save();
                if (set.done && set.reps > 0) {
                    startRest(se.restSec);
                }
            }
        });
        row.addView(check, new LinearLayout.LayoutParams(Ui.dp(ctx, 32), Ui.dp(ctx, 32)));

        weight.setTextColor(set.done ? Ui.TEXT : Ui.TEXT2);
        reps.setTextColor(set.done ? Ui.TEXT : Ui.TEXT2);

        return row;
    }

    private TextView unitLabel(String s) {
        TextView v = Ui.t(ctx, s, 10.5f, Ui.TEXT3);
        v.setPadding(Ui.dp(ctx, 4), 0, Ui.dp(ctx, 2), 0);
        return v;
    }

    private String setLabel(SetEntry set, int index) {
        if (set.type == SetEntry.WARMUP) {
            return "W";
        }
        if (set.type == SetEntry.DROP) {
            return "D";
        }
        return String.valueOf(index + 1);
    }

    private void applyCheckStyle(ImageView check, boolean done) {
        check.setBackground(done ? Ui.circle(Ui.ACCENT, ctx)
                : Ui.strokeBg(0x00000000, Ui.STROKE, 40, 1.4f, ctx));
        check.setImageDrawable(Icons.d(ctx, com.ironlog.train.R.drawable.ic_check,
                done ? Ui.ON_ACCENT : Ui.TEXT3));
        int p = Ui.dp(ctx, done ? 6 : 7);
        check.setPadding(p, p, p, p);
    }

    // ---------------------------------------------------------------- menus

    private void setTypeMenu(final SessionExercise se, final SetEntry set) {
        String[] items = {"标记为正式组", "标记为热身组", "标记为递减组", "删除这一组"};
        new AlertDialog.Builder(ctx)
                .setTitle("第 " + (se.sets.indexOf(set) + 1) + " 组")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            set.type = SetEntry.NORMAL;
                        } else if (which == 1) {
                            set.type = SetEntry.WARMUP;
                            set.done = true;
                        } else if (which == 2) {
                            set.type = SetEntry.DROP;
                        } else {
                            se.sets.remove(set);
                        }
                        Store.get(ctx).save();
                        softRebuild();
                    }
                })
                .show();
    }

    private void exerciseMenu(final SessionExercise se) {
        String[] items = {"备注", "休息时长（当前 " + se.restSec + " 秒）", "替换动作", "移除动作"};
        new AlertDialog.Builder(ctx)
                .setTitle(Store.get(ctx).exerciseName(se.exerciseId))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            Dialogs.prompt(ctx, "动作备注", "例如：握距略宽", se.note, new Dialogs.TextCallback() {
                                @Override
                                public void onText(String text) {
                                    se.note = text;
                                    Store.get(ctx).save();
                                    softRebuild();
                                }
                            });
                        } else if (which == 1) {
                            Dialogs.number(ctx, "休息时长（秒）", "90", se.restSec, new Dialogs.ValueCallback() {
                                @Override
                                public void onValue(double v) {
                                    se.restSec = (int) Math.max(0, v);
                                    Store.get(ctx).save();
                                    softRebuild();
                                }
                            });
                        } else if (which == 2) {
                            act.push(new ExercisePickerScreen(act, false, new ExercisePickerScreen.Callback() {
                                @Override
                                public void onPicked(List<Exercise> picked) {
                                    if (!picked.isEmpty()) {
                                        se.exerciseId = picked.get(0).id;
                                        Store.get(ctx).save();
                                        softRebuild();
                                    }
                                }
                            }));
                        } else {
                            session.exercises.remove(se);
                            Store.get(ctx).save();
                            softRebuild();
                        }
                    }
                })
                .show();
    }

    private void sessionMenu() {
        String[] items = {"训练备注", "放弃这次训练"};
        new AlertDialog.Builder(ctx)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            Dialogs.prompt(ctx, "训练备注", "今天状态如何", session.note, new Dialogs.TextCallback() {
                                @Override
                                public void onText(String text) {
                                    session.note = text;
                                    Store.get(ctx).save();
                                }
                            });
                        } else {
                            Dialogs.confirm(ctx, "放弃训练", "这次训练的组数会全部丢弃，确定吗？", new Runnable() {
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

    // ------------------------------------------------------------ exercises

    private void pickExercises() {
        act.push(new ExercisePickerScreen(act, true, new ExercisePickerScreen.Callback() {
            @Override
            public void onPicked(List<Exercise> picked) {
                Store store = Store.get(ctx);
                for (Exercise e : picked) {
                    SessionExercise se = new SessionExercise(e.id);
                    SessionExercise last = store.lastEntryFor(e.id);
                    if (last != null && !last.sets.isEmpty()) {
                        SetEntry top = last.sets.get(0);
                        for (SetEntry s : last.sets) {
                            if (s.done && s.type != SetEntry.WARMUP && s.weight > top.weight) {
                                top = s;
                            }
                        }
                        se.restSec = last.restSec;
                        for (int i = 0; i < Math.max(1, last.doneSets()); i++) {
                            se.sets.add(new SetEntry(top.weight, top.reps));
                        }
                    } else {
                        for (int i = 0; i < 3; i++) {
                            se.sets.add(new SetEntry(0, 8));
                        }
                    }
                    session.exercises.add(se);
                }
                store.save();
                softRebuild();
            }
        }));
    }

    // ------------------------------------------------------------ rest timer

    private void startRest(int seconds) {
        if (seconds <= 0) {
            return;
        }
        restEndAt = System.currentTimeMillis() + seconds * 1000L;
        resting = true;
        if (restBar != null) {
            restBar.setVisibility(View.VISIBLE);
        }
        handler.removeCallbacks(restTick);
        handler.post(restTick);
    }

    private void stopRest(boolean vibrate) {
        resting = false;
        handler.removeCallbacks(restTick);
        if (restBar != null) {
            restBar.setVisibility(View.GONE);
        }
        if (vibrate && Store.get(ctx).settings.restSound) {
            try {
                Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    v.vibrate(VibrationEffect.createOneShot(240, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            } catch (Exception ignored) {
            }
        }
    }

    // ---------------------------------------------------------------- pause

    /** Timer starts only when the user explicitly says so. */
    private void startTraining() {
        if (session.startedAt == 0L) {
            session.startedAt = System.currentTimeMillis();
            Store.get(ctx).save();
            act.toast("训练开始");
            softRebuild();
        }
    }

    private void togglePause() {
        if (session.isPaused()) {
            session.pausedAccumMs += Math.max(0, System.currentTimeMillis() - session.pausedAt);
            session.pausedAt = 0;
            act.toast("继续训练");
        } else {
            session.pausedAt = System.currentTimeMillis();
            if (resting) {
                stopRest(false);
            }
            act.toast("已暂停，计时停止");
        }
        Store.get(ctx).save();
        softRebuild();
    }

    // ---------------------------------------------------------------- finish

    private void finishWorkout() {
        if (session.isPaused()) {
            session.pausedAccumMs += Math.max(0, System.currentTimeMillis() - session.pausedAt);
            session.pausedAt = 0;
        }

        // Anything with a rep count or a weight counts as recorded, even without the tick.
        int entered = 0;
        for (SessionExercise se : session.exercises) {
            for (SetEntry s : se.sets) {
                if (s.reps > 0 || s.weight > 0 || s.done) {
                    entered++;
                }
            }
        }

        if (entered == 0) {
            Dialogs.confirm(ctx, "还没有记录任何一组",
                    "这次训练没有任何内容，要放弃吗？", new Runnable() {
                        @Override
                        public void run() {
                            Store.get(ctx).removeSession(session);
                            act.pop();
                        }
                    });
            return;
        }

        for (SessionExercise se : session.exercises) {
            for (SetEntry s : se.sets) {
                if (s.reps > 0 || s.weight > 0) {
                    s.done = true;
                }
            }
            if (se.doneSets() == 0) {
                se.sets.clear();
            }
        }
        for (int i = session.exercises.size() - 1; i >= 0; i--) {
            if (session.exercises.get(i).sets.isEmpty()) {
                session.exercises.remove(i);
            }
        }

        if (session.startedAt == 0L) {
            session.startedAt = System.currentTimeMillis();
        }
        session.endedAt = System.currentTimeMillis();
        Store.get(ctx).save();
        act.toast("训练已保存 · 容量 " + Fmt.vol(session.volume()));
        act.pop();
    }

    private void softRebuild() {
        int y = scroll == null ? 0 : scroll.getScrollY();
        rebuild();
        if (scroll != null) {
            final ScrollView s = scroll;
            s.post(new Runnable() {
                @Override
                public void run() {
                    s.scrollTo(0, y);
                }
            });
        }
    }

    @Override
    public boolean onBack() {
        discardIfEmptyDraft();
        act.pop();
        return true;
    }

    /** A session that was started but never got an exercise is not worth keeping. */
    private void discardIfEmptyDraft() {
        if (session != null && session.startedAt == 0L && session.exercises.isEmpty()) {
            Store.get(ctx).removeSession(session);
        }
    }

    @Override
    public void refresh() {
        // Content is rebuilt by the picker callback; rebuilding again would reset the scroll.
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Re-arm keep-screen-on after returning from the exercise picker.
        if (session != null && Store.get(ctx).settings.keepAwake) {
            try {
                act.getWindow().addFlags(
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(clockTick);
        handler.removeCallbacks(restTick);
        try {
            act.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception ignored) {
        }
        super.onDetachedFromWindow();
    }
}
