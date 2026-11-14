package com.ironlog.train.ui;

import android.text.Editable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ironlog.train.data.BodyMetric;
import com.ironlog.train.data.Store;
import com.ironlog.train.logic.Fmt;
import com.ironlog.train.ui.widget.LineChartView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BodyScreen extends Screen {

    private EditText inWeight;
    private EditText inFat;
    private EditText inWaist;
    private EditText inChest;
    private EditText inArm;
    private EditText inThigh;
    private EditText inHip;
    /** Which date the form edits; history rows switch this to that day. */
    private LocalDate editDate = LocalDate.now();

    public BodyScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected void build() {
        final Store store = Store.get(ctx);

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
        titles.addView(Ui.label(ctx, "体重 / 围度"));
        titles.addView(Ui.title(ctx, "身体数据"));
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (!editDate.equals(LocalDate.now())) {
            TextView backToday = Icons.action(ctx, com.ironlog.train.R.drawable.ic_calendar,
                    "回到今天", Ui.ACCENT, Ui.ACCENT_SOFT);
            backToday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editDate = LocalDate.now();
                    rebuild();
                }
            });
            head.addView(backToday);
        }
        body.addView(head);

        body.addView(Ui.spacer(ctx, 16));

        // ------------------------------------------------------------- chart
        List<BodyMetric> sorted = new ArrayList<>(store.metrics);
        Collections.sort(sorted, new Comparator<BodyMetric>() {
            @Override
            public int compare(BodyMetric a, BodyMetric b) {
                return Long.compare(a.date, b.date);
            }
        });

        LinearLayout chartCard = Ui.card(ctx);
        chartCard.addView(Ui.sectionTitle(ctx, "体重变化"));
        chartCard.addView(Ui.spacer(ctx, 8));
        LineChartView chart = new LineChartView(ctx);
        chart.setAccent(Ui.GREEN);
        chart.setEmptyText("还没有体测记录");
        int n = 0;
        for (BodyMetric m : sorted) {
            if (m.weight > 0) {
                n++;
            }
        }
        long[] dates = new long[n];
        double[] vals = new double[n];
        int i = 0;
        for (BodyMetric m : sorted) {
            if (m.weight > 0) {
                dates[i] = m.date;
                vals[i] = m.weight;
                i++;
            }
        }
        chart.setSeries(dates, vals);
        chartCard.addView(chart, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(chartCard);

        body.addView(Ui.spacer(ctx, 14));

        // -------------------------------------------------------------- form
        LocalDate today = LocalDate.now();
        BodyMetric todayMetric = null;
        for (BodyMetric m : store.metrics) {
            if (Fmt.date(m.date).equals(editDate)) {
                todayMetric = m;
            }
        }

        LinearLayout form = Ui.card(ctx);
        String formTitle;
        if (editDate.equals(today)) {
            formTitle = todayMetric != null ? "更新今天的记录" : "记录今天";
        } else {
            formTitle = "编辑 " + Fmt.shortDate(Fmt.millis(editDate)) + " 的记录";
        }
        form.addView(Ui.sectionTitle(ctx, formTitle));
        form.addView(Ui.spacer(ctx, 12));

        LinearLayout row1 = Ui.row(ctx);
        inWeight = field(row1, "体重 kg", todayMetric != null && todayMetric.weight > 0
                ? Fmt.num1(todayMetric.weight) : "");
        inFat = field(row1, "体脂 %", todayMetric != null && todayMetric.bodyFat > 0
                ? Fmt.num1(todayMetric.bodyFat) : "");
        form.addView(row1);

        form.addView(Ui.spacer(ctx, 10));

        LinearLayout row2 = Ui.row(ctx);
        inWaist = field(row2, "腰围 cm", todayMetric != null && todayMetric.waist > 0
                ? Fmt.num1(todayMetric.waist) : "");
        inChest = field(row2, "胸围 cm", todayMetric != null && todayMetric.chest > 0
                ? Fmt.num1(todayMetric.chest) : "");
        form.addView(row2);

        form.addView(Ui.spacer(ctx, 10));

        LinearLayout row3 = Ui.row(ctx);
        inArm = field(row3, "臂围 cm", todayMetric != null && todayMetric.arm > 0
                ? Fmt.num1(todayMetric.arm) : "");
        inThigh = field(row3, "腿围 cm", todayMetric != null && todayMetric.thigh > 0
                ? Fmt.num1(todayMetric.thigh) : "");
        form.addView(row3);

        form.addView(Ui.spacer(ctx, 10));

        LinearLayout row4 = Ui.row(ctx);
        inHip = field(row4, "臀围 cm", todayMetric != null && todayMetric.hip > 0
                ? Fmt.num1(todayMetric.hip) : "");
        form.addView(row4);

        form.addView(Ui.spacer(ctx, 14));

        TextView save = Ui.primaryButton(ctx, "保存");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMetric();
            }
        });
        form.addView(save, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(form);

        // -------------------------------------------------------------- list
        if (!store.metrics.isEmpty()) {
            body.addView(Ui.spacer(ctx, 18));
            body.addView(Ui.sectionTitle(ctx, "历史记录"));
            body.addView(Ui.spacer(ctx, 10));

            List<BodyMetric> desc = new ArrayList<>(store.metrics);
            Collections.sort(desc, new Comparator<BodyMetric>() {
                @Override
                public int compare(BodyMetric a, BodyMetric b) {
                    return Long.compare(b.date, a.date);
                }
            });
            for (final BodyMetric m : desc) {
                body.addView(metricRow(m));
                body.addView(Ui.spacer(ctx, 7));
            }
        }

        addView(Ui.scroll(ctx, body), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private EditText field(LinearLayout parent, String hint, String value) {
        LinearLayout c = Ui.col(ctx);
        EditText e = Ui.numberInput(ctx, hint);
        e.setText(value);
        e.setTextSize(14f);
        c.addView(e, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 42)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = Ui.dp(ctx, 8);
        c.setLayoutParams(lp);
        parent.addView(c);
        return e;
    }

    private void saveMetric() {
        Store store = Store.get(ctx);
        BodyMetric m = null;
        for (BodyMetric existing : store.metrics) {
            if (Fmt.date(existing.date).equals(editDate)) {
                m = existing;
            }
        }
        if (m == null) {
            m = new BodyMetric();
            m.date = Fmt.millis(editDate);
            store.metrics.add(m);
        }
        m.weight = Dialogs.parse(inWeight.getText().toString());
        m.bodyFat = Dialogs.parse(inFat.getText().toString());
        m.waist = Dialogs.parse(inWaist.getText().toString());
        m.chest = Dialogs.parse(inChest.getText().toString());
        m.arm = Dialogs.parse(inArm.getText().toString());
        m.thigh = Dialogs.parse(inThigh.getText().toString());
        m.hip = Dialogs.parse(inHip.getText().toString());

        if (m.weight == 0 && m.bodyFat == 0 && m.waist == 0 && m.chest == 0
                && m.arm == 0 && m.thigh == 0 && m.hip == 0) {
            act.toast("至少填一项");
            return;
        }
        store.save();
        act.toast("已保存");
        rebuild();
    }

    private View metricRow(final BodyMetric m) {
        LinearLayout row = Ui.row(ctx);
        row.setBackground(Ui.bg(Ui.SURFACE, 12, ctx));
        Ui.pad(row, 14, 11, 14, 11);
        row.setClickable(true);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editDate = Fmt.date(m.date);
                rebuild();
            }
        });

        LinearLayout left = Ui.col(ctx);
        left.addView(Ui.num(ctx, Fmt.shortDate(m.date), 13f, Ui.TEXT));
        StringBuilder sb = new StringBuilder();
        if (m.weight > 0) sb.append("体重 ").append(Fmt.num1(m.weight)).append("  ");
        if (m.bodyFat > 0) sb.append("体脂 ").append(Fmt.num1(m.bodyFat)).append("%  ");
        if (m.waist > 0) sb.append("腰 ").append(Fmt.num1(m.waist)).append("  ");
        if (m.chest > 0) sb.append("胸 ").append(Fmt.num1(m.chest)).append("  ");
        if (m.arm > 0) sb.append("臂 ").append(Fmt.num1(m.arm)).append("  ");
        if (m.thigh > 0) sb.append("腿 ").append(Fmt.num1(m.thigh)).append("  ");
        if (m.hip > 0) sb.append("臀 ").append(Fmt.num1(m.hip));
        left.addView(Ui.t(ctx, sb.toString().trim(), 10.5f, Ui.TEXT3));
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView del = Icons.button(ctx, com.ironlog.train.R.drawable.ic_close, 14, Ui.TEXT3, 5);
        del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Store.get(ctx).metrics.remove(m);
                Store.get(ctx).save();
                rebuild();
            }
        });
        row.addView(del);
        return row;
    }
}
