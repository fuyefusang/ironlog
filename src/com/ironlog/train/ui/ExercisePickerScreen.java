package com.ironlog.train.ui;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ironlog.train.data.Catalog;
import com.ironlog.train.data.Exercise;
import com.ironlog.train.data.Store;
import com.ironlog.train.ui.widget.MuscleMapView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExercisePickerScreen extends Screen {

    public interface Callback {
        void onPicked(List<Exercise> picked);
    }

    private final Callback callback;
    private final boolean multi;
    private final Set<String> selected = new LinkedHashSet<>();
    private final Set<String> expanded = new LinkedHashSet<>();

    private String query = "";
    private String muscle = "全部";
    private LinearLayout listBox;
    private LinearLayout chipsRow;
    private TextView confirmBtn;
    private MuscleMapView mapView;
    private TextView mapLabel;
    private TextView muscleDetail;

    public ExercisePickerScreen(MainActivity a, boolean multi, Callback cb) {
        super(a);
        this.multi = multi;
        this.callback = cb;
    }

    private static String groupDetail(String m) {
        if ("胸".equals(m)) return "胸大肌 · 三角肌前束 · 肱三头肌";
        if ("背".equals(m)) return "背阔肌 · 斜方肌 · 竖脊肌 · 肱二头肌";
        if ("腿".equals(m)) return "股四头肌 · 腘绳肌 · 臀大肌 · 小腿三头肌";
        if ("肩".equals(m)) return "三角肌前束 / 中束 / 后束";
        if ("手臂".equals(m)) return "肱二头肌 · 肱三头肌 · 前臂";
        if ("核心".equals(m)) return "腹直肌 · 腹斜肌 · 竖脊肌";
        if ("有氧".equals(m)) return "心肺 · 全身大肌群";
        return "点开动作分组，能看到每个动作具体练哪些肌肉";
    }

    @Override
    protected void build() {
        // ------------------------------------------------------ fixed header
        LinearLayout header = Ui.row(ctx);
        header.setBackgroundColor(Ui.NAV);
        header.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 10), Ui.dp(ctx, 14), Ui.dp(ctx, 10));

        ImageView back = Icons.button(ctx, com.ironlog.train.R.drawable.ic_back, 22, Ui.TEXT, 6);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.pop();
            }
        });
        header.addView(back);

        LinearLayout titles = Ui.col(ctx);
        titles.setPadding(Ui.dp(ctx, 10), 0, 0, 0);
        titles.addView(Ui.label(ctx, multi ? "可多选" : "单选"));
        titles.addView(Ui.bold(ctx, "选择动作", 17f, Ui.TEXT));
        header.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView add = Icons.action(ctx, com.ironlog.train.R.drawable.ic_add, "新建",
                Ui.ACCENT, Ui.ACCENT_SOFT);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newExerciseDialog();
            }
        });
        header.addView(add);

        FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 64));
        hlp.gravity = Gravity.TOP;
        addView(header, hlp);

        // ---------------------------------------------------------- content
        LinearLayout body = Ui.col(ctx);
        body.setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 14), Ui.dp(ctx, 18),
                Ui.dp(ctx, multi ? 92 : 24));

        final EditText search = Ui.textInput(ctx, "搜索动作名称");
        Icons.start(search, ctx, com.ironlog.train.R.drawable.ic_search, Ui.TEXT3, 16, 8);
        search.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                query = s.toString().trim();
                fillList();
            }
        });
        body.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body.addView(Ui.spacer(ctx, 12));

        HorizontalScrollView hs = new HorizontalScrollView(ctx);
        hs.setHorizontalScrollBarEnabled(false);
        chipsRow = Ui.row(ctx);
        hs.addView(chipsRow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(hs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body.addView(Ui.spacer(ctx, 12));

        // ------------------------------------------------ muscle body map
        LinearLayout mapCard = Ui.col(ctx);
        mapCard.setBackground(Ui.bg(Ui.SURFACE, 14, ctx));
        Ui.pad(mapCard, 12, 10, 12, 10);

        LinearLayout mapHead = Ui.row(ctx);
        mapHead.addView(Ui.label(ctx, "训练部位"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        mapLabel = Ui.t(ctx, "全部".equals(muscle) ? "全部肌群" : muscle, 11.5f, Ui.ACCENT);
        mapHead.addView(mapLabel);
        mapCard.addView(mapHead);

        mapView = new MuscleMapView(ctx);
        mapView.setMuscle("全部".equals(muscle) ? "" : muscle);
        mapCard.addView(mapView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        muscleDetail = Ui.t(ctx, groupDetail(muscle), 10.5f, Ui.TEXT3);
        muscleDetail.setGravity(Gravity.CENTER);
        muscleDetail.setPadding(0, Ui.dp(ctx, 2), 0, Ui.dp(ctx, 4));
        mapCard.addView(muscleDetail);

        body.addView(mapCard);

        body.addView(Ui.spacer(ctx, 12));

        listBox = Ui.col(ctx);
        body.addView(listBox);

        buildChips();
        fillList();

        ScrollView sc = Ui.scroll(ctx, body);
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        slp.topMargin = Ui.dp(ctx, 64);
        addView(sc, slp);

        if (multi) {
            confirmBtn = Icons.button(ctx, com.ironlog.train.R.drawable.ic_check,
                    "添加 0 个动作", Ui.ON_ACCENT, Ui.ACCENT);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.BOTTOM;
            lp.leftMargin = Ui.dp(ctx, 18);
            lp.rightMargin = Ui.dp(ctx, 18);
            lp.bottomMargin = Ui.dp(ctx, 16);
            addView(confirmBtn, lp);
            confirmBtn.setVisibility(View.GONE);
            confirmBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    submit();
                }
            });
        }
    }

    private void buildChips() {
        chipsRow.removeAllViews();
        List<String> options = new ArrayList<>();
        options.add("全部");
        for (String m : Catalog.MUSCLES) {
            options.add(m);
        }
        for (final String m : options) {
            boolean on = m.equals(muscle);
            TextView chip = Ui.t(ctx, m, 12.5f, on ? Ui.ON_ACCENT : Ui.TEXT2);
            chip.setTypeface(Ui.MEDIUM);
            chip.setBackground(Ui.bg(on ? Ui.ACCENT : Ui.SURFACE2, 30, ctx));
            chip.setPadding(Ui.dp(ctx, 13), Ui.dp(ctx, 7), Ui.dp(ctx, 13), Ui.dp(ctx, 7));
            chip.setClickable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(ctx, 8);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    muscle = m;
                    expanded.clear();
                    buildChips();
                    fillList();
                    boolean all = "全部".equals(m);
                    if (mapView != null) {
                        mapView.setMuscle(all ? "" : m);
                    }
                    if (mapLabel != null) {
                        mapLabel.setText(all ? "全部肌群" : m);
                    }
                    if (muscleDetail != null) {
                        muscleDetail.setText(groupDetail(m));
                    }
                }
            });
            chipsRow.addView(chip, lp);
        }
    }

    /** Groups equipment variants of the same movement under one collapsible header. */
    private void fillList() {
        listBox.removeAllViews();
        List<Exercise> all = Store.get(ctx).allExercises();

        if (!query.isEmpty()) {
            int n = 0;
            for (Exercise e : all) {
                if (!matchesFilter(e)) {
                    continue;
                }
                if (!e.name.toLowerCase().contains(query.toLowerCase())) {
                    continue;
                }
                listBox.addView(exerciseRow(e, false));
                listBox.addView(Ui.spacer(ctx, 7));
                n++;
            }
            if (n == 0) {
                emptyHint();
            }
            updateConfirm();
            return;
        }

        Map<String, List<Exercise>> groups = new LinkedHashMap<>();
        for (Exercise e : all) {
            if (!matchesFilter(e)) {
                continue;
            }
            String key = e.familyOrName();
            List<Exercise> list = groups.get(key);
            if (list == null) {
                list = new ArrayList<>();
                groups.put(key, list);
            }
            list.add(e);
        }

        if (groups.isEmpty()) {
            emptyHint();
        }

        for (Map.Entry<String, List<Exercise>> en : groups.entrySet()) {
            listBox.addView(groupHeader(en.getKey(), en.getValue()));
            if (expanded.contains(en.getKey())) {
                for (Exercise e : en.getValue()) {
                    listBox.addView(exerciseRow(e, true));
                    listBox.addView(Ui.spacer(ctx, 7));
                }
            }
            listBox.addView(Ui.spacer(ctx, 7));
        }
        updateConfirm();
    }

    private boolean matchesFilter(Exercise e) {
        return "全部".equals(muscle) || muscle.equals(e.muscle);
    }

    private void emptyHint() {
        TextView empty = Ui.t(ctx, "没有匹配的动作，可以点右上角「新建」", 13f, Ui.TEXT3);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, Ui.dp(ctx, 30), 0, Ui.dp(ctx, 30));
        listBox.addView(empty);
    }

    private View groupHeader(final String family, List<Exercise> members) {
        boolean open = expanded.contains(family);
        LinearLayout row = Ui.row(ctx);
        row.setBackground(Ui.bg(Ui.SURFACE2, 12, ctx));
        Ui.pad(row, 14, 11, 12, 11);
        row.setClickable(true);
        Ui.ripple(row, Ui.DARK ? 0x1FFFFFFF : 0x14000000);

        int picked = 0;
        for (Exercise e : members) {
            if (selected.contains(e.id)) {
                picked++;
            }
        }

        LinearLayout texts = Ui.col(ctx);
        texts.addView(Ui.bold(ctx, family, 14f, Ui.TEXT));
        String sub = members.size() == 1
                ? members.get(0).equipment
                : members.size() + " 个变体 · " + equipmentSummary(members);
        texts.addView(Ui.t(ctx, sub, 10.5f, Ui.TEXT3));
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (picked > 0) {
            row.addView(Ui.pill(ctx, "已选 " + picked, Ui.ACCENT, Ui.ACCENT_SOFT));
        }

        ImageView chevron = Icons.iv(ctx, com.ironlog.train.R.drawable.ic_chevron_right,
                14, Ui.TEXT3);
        chevron.setRotation(open ? 90f : 0f);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(ctx, 14), Ui.dp(ctx, 14));
        clp.leftMargin = Ui.dp(ctx, 8);
        row.addView(chevron, clp);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expanded.contains(family)) {
                    expanded.remove(family);
                } else {
                    expanded.add(family);
                }
                fillList();
            }
        });
        return row;
    }

    private String equipmentSummary(List<Exercise> members) {
        Set<String> set = new LinkedHashSet<>();
        for (Exercise e : members) {
            set.add(e.equipment);
        }
        StringBuilder sb = new StringBuilder();
        for (String s : set) {
            if (sb.length() > 0) {
                sb.append("/");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    private View exerciseRow(final Exercise e, boolean indented) {
        boolean on = selected.contains(e.id);
        LinearLayout row = Ui.row(ctx);
        row.setBackground(Ui.bg(on ? Ui.ACCENT_SOFT : Ui.SURFACE, 12, ctx));
        if (on) {
            row.setBackground(Ui.strokeBg(Ui.ACCENT_SOFT, Ui.ACCENT, 12, 1, ctx));
        }
        Ui.pad(row, indented ? 26 : 14, 11, 14, 11);
        row.setClickable(true);
        Ui.ripple(row, Ui.DARK ? 0x1FFFFFFF : 0x14000000);

        LinearLayout texts = Ui.col(ctx);
        LinearLayout nameLine = Ui.row(ctx);
        nameLine.addView(Ui.bold(ctx, e.name, 14f, Ui.TEXT));
        TextView eq = Ui.t(ctx, "  " + e.equipment, 10f, Ui.TEXT3);
        nameLine.addView(eq);
        texts.addView(nameLine);

        String muscles = "主要：" + Ui.nz(e.primary, e.muscle);
        if (e.secondary != null && !e.secondary.isEmpty()) {
            muscles += " · 辅助：" + e.secondary;
        }
        TextView mtext = Ui.t(ctx, muscles, 10.5f, Ui.TEXT2);
        mtext.setMaxLines(2);
        mtext.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(mtext);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (on) {
            row.addView(Icons.iv(ctx, com.ironlog.train.R.drawable.ic_check, 16, Ui.ACCENT));
        }

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (multi) {
                    if (selected.contains(e.id)) {
                        selected.remove(e.id);
                    } else {
                        selected.add(e.id);
                    }
                    showExerciseMuscles(e);
                    fillList();
                } else {
                    List<Exercise> one = new ArrayList<>();
                    one.add(e);
                    if (callback != null) {
                        callback.onPicked(one);
                    }
                    act.pop();
                }
            }
        });
        return row;
    }

    /** Lights up exactly the muscles this movement trains. */
    private void showExerciseMuscles(Exercise e) {
        if (mapView != null) {
            String csv = e.primary;
            if (e.secondary != null && !e.secondary.isEmpty()) {
                csv = e.primary + "," + e.secondary;
            }
            mapView.setMuscles(csv);
        }
        if (mapLabel != null) {
            mapLabel.setText(e.name);
        }
        if (muscleDetail != null) {
            String text = "主要：" + Ui.nz(e.primary, e.muscle);
            if (e.secondary != null && !e.secondary.isEmpty()) {
                text += "　辅助：" + e.secondary;
            }
            muscleDetail.setText(text);
        }
    }

    private void updateConfirm() {
        if (!multi || confirmBtn == null) {
            return;
        }
        if (selected.isEmpty()) {
            confirmBtn.setVisibility(View.GONE);
            return;
        }
        confirmBtn.setVisibility(View.VISIBLE);
        confirmBtn.setText("添加 " + selected.size() + " 个动作");
    }

    private void newExerciseDialog() {
        final EditText name = Ui.textInput(ctx, "动作名称，如：俯身哑铃划船");
        LinearLayout wrap = Ui.col(ctx);
        wrap.setPadding(Ui.dp(ctx, 20), Ui.dp(ctx, 8), Ui.dp(ctx, 20), 0);
        wrap.addView(name);

        new android.app.AlertDialog.Builder(ctx)
                .setTitle("新建动作")
                .setView(wrap)
                .setPositiveButton("创建", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        String n = name.getText().toString().trim();
                        if (n.isEmpty()) {
                            act.toast("名称不能为空");
                            return;
                        }
                        chooseMuscle(n);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void chooseMuscle(final String name) {
        new android.app.AlertDialog.Builder(ctx)
                .setTitle("属于哪个肌群")
                .setItems(Catalog.MUSCLES, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        final String m = Catalog.MUSCLES[which];
                        new android.app.AlertDialog.Builder(ctx)
                                .setTitle("使用什么器械")
                                .setItems(Catalog.EQUIPMENT, new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface d2, int w2) {
                                        createExercise(name, m, Catalog.EQUIPMENT[w2]);
                                    }
                                })
                                .show();
                    }
                })
                .show();
    }

    private void createExercise(String name, String muscle, String equipment) {
        Store store = Store.get(ctx);
        Exercise e = new Exercise("u_" + Store.newId(), name, muscle, equipment,
                name, muscle, "", true);
        store.customExercises.add(e);
        store.save();
        selected.add(e.id);
        expanded.add(name);
        act.toast("已创建：" + name);
        fillList();
    }

    private void submit() {
        if (selected.isEmpty()) {
            act.pop();
            return;
        }
        Store store = Store.get(ctx);
        List<Exercise> picked = new ArrayList<>();
        for (String id : selected) {
            Exercise e = store.exercise(id);
            if (e != null) {
                picked.add(e);
            }
        }
        if (callback != null) {
            callback.onPicked(picked);
        }
        act.pop();
    }

    @Override
    public boolean onBack() {
        return false;
    }
}
