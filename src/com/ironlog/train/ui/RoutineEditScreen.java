package com.ironlog.train.ui;

import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ironlog.train.data.Exercise;
import com.ironlog.train.data.Routine;
import com.ironlog.train.data.RoutineExercise;
import com.ironlog.train.data.Store;
import com.ironlog.train.logic.Fmt;

import java.util.List;

public class RoutineEditScreen extends Screen {

    private final String routineId;
    private Routine routine;

    public RoutineEditScreen(MainActivity a, String routineId) {
        super(a);
        this.routineId = routineId;
    }

    @Override
    protected void build() {
        Store store = Store.get(ctx);
        for (Routine r : store.routines) {
            if (routineId.equals(r.id)) {
                routine = r;
            }
        }
        if (routine == null) {
            TextView tv = Ui.t(ctx, "计划不存在", 14f, Ui.TEXT2);
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

        LinearLayout titles = Ui.col(ctx);
        titles.addView(Ui.label(ctx, "训练模板"));
        titles.addView(Ui.title(ctx, "编辑计划"));
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView done = Icons.action(ctx, com.ironlog.train.R.drawable.ic_check, "完成",
                Ui.ON_ACCENT, Ui.ACCENT);
        done.setClickable(true);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Store.get(ctx).save();
                act.pop();
            }
        });
        head.addView(done);
        body.addView(head);

        body.addView(Ui.spacer(ctx, 18));

        final EditText name = Ui.textInput(ctx, "计划名称");
        name.setText(routine.name);
        name.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                routine.name = s.toString();
                Store.get(ctx).save();
            }
        });
        body.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body.addView(Ui.spacer(ctx, 10));

        final EditText note = Ui.textInput(ctx, "备注（可选）");
        note.setText(routine.note == null ? "" : routine.note);
        note.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                routine.note = s.toString();
                Store.get(ctx).save();
            }
        });
        body.addView(note, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body.addView(Ui.spacer(ctx, 16));

        // ------------------------------------------------- weekly schedule
        LinearLayout scheduleBox = Ui.col(ctx);
        scheduleBox.setBackground(Ui.bg(Ui.SURFACE, 14, ctx));
        Ui.pad(scheduleBox, 14, 12, 14, 12);
        scheduleBox.addView(Ui.t(ctx, "每周安排", 12f, Ui.TEXT2));
        scheduleBox.addView(Ui.spacer(ctx, 8));
        LinearLayout dayRow = Ui.row(ctx);
        String[] names = {"一", "二", "三", "四", "五", "六", "日"};
        for (int i = 0; i < 7; i++) {
            final int iso = i + 1;
            final boolean on = routine.onDay(iso);
            TextView chip = Ui.t(ctx, names[i], 12.5f, on ? Ui.ON_ACCENT : Ui.TEXT2);
            chip.setTypeface(Ui.MEDIUM);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(Ui.bg(on ? Ui.ACCENT : Ui.SURFACE2, 30, ctx));
            chip.setClickable(true);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    routine.toggleDay(iso);
                    Store.get(ctx).save();
                    rebuild();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(ctx, 34), 1f);
            lp.rightMargin = Ui.dp(ctx, 6);
            dayRow.addView(chip, lp);
        }
        scheduleBox.addView(dayRow);
        scheduleBox.addView(Ui.spacer(ctx, 6));
        scheduleBox.addView(Ui.t(ctx, "标记后会在「数据」日历里显示为计划日", 10.5f, Ui.TEXT3));
        body.addView(scheduleBox);

        body.addView(Ui.spacer(ctx, 20));

        LinearLayout sectionHead = Ui.row(ctx);
        sectionHead.addView(Ui.sectionTitle(ctx, "动作"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sectionHead.addView(Ui.t(ctx, routine.items.size() + " 个", 12f, Ui.TEXT3));
        body.addView(sectionHead);
        body.addView(Ui.spacer(ctx, 10));

        if (routine.items.isEmpty()) {
            LinearLayout empty = Ui.card(ctx);
            TextView e = Ui.t(ctx, "添加动作，设定目标组数与次数", 12.5f, Ui.TEXT3);
            e.setGravity(Gravity.CENTER);
            empty.addView(e);
            body.addView(empty);
        }

        for (final RoutineExercise ri : routine.items) {
            body.addView(itemCard(ri));
            body.addView(Ui.spacer(ctx, 8));
        }

        body.addView(Ui.spacer(ctx, 6));

        TextView addEx = Icons.ghost(ctx, com.ironlog.train.R.drawable.ic_add, "添加动作");
        addEx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.push(new ExercisePickerScreen(act, true, new ExercisePickerScreen.Callback() {
                    @Override
                    public void onPicked(List<Exercise> picked) {
                        for (Exercise e : picked) {
                            routine.items.add(new RoutineExercise(e.id, 3, 8, 0));
                        }
                        Store.get(ctx).save();
                        rebuild();
                    }
                }));
            }
        });
        body.addView(addEx, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body.addView(Ui.spacer(ctx, 22));

        TextView startNow = Icons.button(ctx, com.ironlog.train.R.drawable.ic_play,
                "用这个计划开始训练", Ui.ON_ACCENT, Ui.ACCENT);
        startNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Store.get(ctx).save();
                startFromRoutine();
            }
        });
        body.addView(startNow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addView(Ui.scroll(ctx, body), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private View itemCard(final RoutineExercise ri) {
        LinearLayout card = Ui.col(ctx);
        card.setBackground(Ui.bg(Ui.SURFACE, 14, ctx));
        Ui.pad(card, 14, 12, 14, 12);

        LinearLayout top = Ui.row(ctx);
        LinearLayout titles = Ui.col(ctx);
        titles.addView(Ui.bold(ctx, Store.get(ctx).exerciseName(ri.exerciseId), 14.5f, Ui.TEXT));
        Exercise meta = Store.get(ctx).exercise(ri.exerciseId);
        if (meta != null) {
            titles.addView(Ui.t(ctx, meta.muscle + " · " + meta.equipment, 11f, Ui.TEXT3));
        }
        top.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView del = Icons.button(ctx, com.ironlog.train.R.drawable.ic_close, 15, Ui.TEXT3, 5);
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                routine.items.remove(ri);
                Store.get(ctx).save();
                rebuild();
            }
        });
        top.addView(del);
        card.addView(top);

        card.addView(Ui.spacer(ctx, 10));

        LinearLayout fields = Ui.row(ctx);
        fields.addView(numberField("组数", String.valueOf(ri.sets), new Dialogs.ValueCallback() {
            @Override
            public void onValue(double v) {
                ri.sets = (int) Math.max(1, v);
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        fields.addView(numberField("次数", String.valueOf(ri.reps), new Dialogs.ValueCallback() {
            @Override
            public void onValue(double v) {
                ri.reps = (int) Math.max(1, v);
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        fields.addView(numberField("重量", ri.weight > 0 ? Fmt.num(ri.weight) : "", new Dialogs.ValueCallback() {
            @Override
            public void onValue(double v) {
                ri.weight = v;
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        fields.addView(numberField("休息秒", String.valueOf(ri.restSec), new Dialogs.ValueCallback() {
            @Override
            public void onValue(double v) {
                ri.restSec = (int) Math.max(0, v);
            }
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(fields);

        return card;
    }

    private View numberField(String label, String value, final Dialogs.ValueCallback cb) {
        LinearLayout c = Ui.col(ctx);
        EditText e = Ui.numberInput(ctx, label);
        e.setText(value);
        e.setTextSize(14f);
        e.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                cb.onValue(Dialogs.parse(s.toString()));
                Store.get(ctx).save();
            }
        });
        c.addView(e, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 40)));
        TextView l = Ui.t(ctx, label, 9.5f, Ui.TEXT3);
        l.setGravity(Gravity.CENTER);
        l.setPadding(0, Ui.dp(ctx, 3), 0, 0);
        c.addView(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = Ui.dp(ctx, 6);
        c.setLayoutParams(lp);
        return c;
    }

    private void startFromRoutine() {
        Store store = Store.get(ctx);
        if (store.activeSession() != null) {
            act.toast("已有进行中的训练");
            return;
        }
        if (routine.items.isEmpty()) {
            act.toast("先添加动作");
            return;
        }
        com.ironlog.train.data.WorkoutSession s = new com.ironlog.train.data.WorkoutSession();
        s.id = Store.newId();
        s.name = routine.name;
        s.routineId = routine.id;
        s.startedAt = System.currentTimeMillis();
        for (RoutineExercise ri : routine.items) {
            com.ironlog.train.data.SessionExercise se =
                    new com.ironlog.train.data.SessionExercise(ri.exerciseId);
            se.restSec = ri.restSec;
            for (int i = 0; i < ri.sets; i++) {
                se.sets.add(new com.ironlog.train.data.SetEntry(ri.weight, ri.reps));
            }
            s.exercises.add(se);
        }
        store.addSession(s);
        act.push(new WorkoutScreen(act, s.id));
    }

    @Override
    public boolean onBack() {
        Store.get(ctx).save();
        act.pop();
        return true;
    }
}
