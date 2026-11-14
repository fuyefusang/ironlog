package com.ironlog.train.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import com.ironlog.train.ui.Ui;

import java.util.HashSet;
import java.util.Set;

/**
 * Front/back anatomy diagram. Each muscle group is its own shape so a single
 * movement can light up exactly the muscles it trains.
 */
public class MuscleMapView extends View {

    private final Set<String> active = new HashSet<>();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();

    public MuscleMapView(Context c) {
        super(c);
        p.setTypeface(Ui.MEDIUM);
    }

    /** Highlights the muscles listed in {@code csv} (Chinese names, comma separated). */
    public void setMuscles(String csv) {
        active.clear();
        if (csv != null && !csv.isEmpty()) {
            for (String raw : csv.split("[,，、/]")) {
                String m = raw.trim();
                if (m.isEmpty()) {
                    continue;
                }
                // custom exercises store the broad group as their primary "muscle"
                String[] group = groupFor(m);
                if (group != null) {
                    for (String g : group) {
                        active.addAll(mapName(g));
                    }
                } else {
                    active.addAll(mapName(m));
                }
            }
        }
        invalidate();
    }

    private static String[] groupFor(String m) {
        if ("胸".equals(m)) return groupMuscles("胸");
        if ("背".equals(m)) return groupMuscles("背");
        if ("腿".equals(m)) return groupMuscles("腿");
        if ("肩".equals(m)) return groupMuscles("肩");
        if ("手臂".equals(m)) return groupMuscles("手臂");
        if ("核心".equals(m)) return groupMuscles("核心");
        if ("有氧".equals(m)) return groupMuscles("有氧");
        return null;
    }

    /** Highlights a whole body region (used by the muscle filter chips). */
    public void setMuscle(String group) {
        active.clear();
        if (group == null || group.isEmpty()) {
            invalidate();
            return;
        }
        for (String m : groupMuscles(group)) {
            active.addAll(mapName(m));
        }
        invalidate();
    }

    private static String[] groupMuscles(String g) {
        if ("胸".equals(g)) return new String[]{"胸大肌", "三角肌前束", "肱三头肌"};
        if ("背".equals(g)) return new String[]{"背阔肌", "斜方肌", "竖脊肌", "肱二头肌"};
        if ("腿".equals(g)) return new String[]{"股四头肌", "腘绳肌", "臀大肌", "小腿"};
        if ("肩".equals(g)) return new String[]{"三角肌前束", "三角肌中束", "三角肌后束"};
        if ("手臂".equals(g)) return new String[]{"肱二头肌", "肱三头肌", "前臂"};
        if ("核心".equals(g)) return new String[]{"腹直肌", "腹斜肌", "竖脊肌"};
        if ("有氧".equals(g)) return new String[]{"心肺"};
        return new String[0];
    }

    private static Set<String> mapName(String m) {
        Set<String> out = new HashSet<>();
        String n = m.replace("（", "").replace("）", "").replace("(", "").replace(")", "")
                .replace("上部", "").replace("下部", "").replace("中部", "")
                .replace("长头", "").replace("短头", "").trim();
        switch (n) {
            case "胸大肌": out.add("chest"); break;
            case "腹直肌": out.add("abs"); break;
            case "腹斜肌": out.add("obliques"); break;
            case "三角肌前束": out.add("frontDelt"); break;
            case "三角肌中束": out.add("sideDelt"); break;
            case "三角肌后束": out.add("rearDelt"); break;
            case "肱二头肌": case "肱肌": out.add("biceps"); break;
            case "肱三头肌": out.add("triceps"); break;
            case "肱桡肌": case "前臂": case "前臂屈肌": case "前臂伸肌": out.add("forearm"); break;
            case "背阔肌": out.add("lats"); break;
            case "斜方肌": case "菱形肌": out.add("traps"); break;
            case "竖脊肌": case "腰方肌": out.add("erector"); break;
            case "臀大肌": case "臀中肌": case "臀小肌": out.add("glutes"); break;
            case "股四头肌": out.add("quads"); break;
            case "腘绳肌": out.add("hams"); break;
            case "内收肌": out.add("adductors"); break;
            case "小腿": case "小腿三头肌": case "腓肠肌": case "比目鱼肌": out.add("calves"); break;
            case "髂腰肌": out.add("abs"); break;
            case "心肺": case "全身": case "心脏": out.add("all"); break;
            default: break;
        }
        return out;
    }

    private boolean on(String key) {
        return active.contains("all") || active.contains(key);
    }

    private int tone(String key) {
        if (on(key)) {
            return (Ui.ACCENT & 0x00FFFFFF) | 0xD0000000;
        }
        return Ui.DARK ? 0xFF2E3945 : 0xFFC9D3DF;
    }

    /** Base silhouette colour, kept clearly distinct from an un-highlighted muscle. */
    private int bodyTone() {
        return Ui.DARK ? 0xFF1B222B : 0xFFE4E9F0;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), Ui.dp(getContext(), 186));
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth();
        int h = getHeight();
        float top = Ui.dp(getContext(), 6);
        float labelH = Ui.dp(getContext(), 20);
        float bodyH = h - top - labelH;
        float s = bodyH / 118f;

        float figW = 58f * s;
        float gap = Math.max(Ui.dp(getContext(), 24), (w - figW * 2) * 0.16f);
        float startX = (w - figW * 2 - gap) / 2f + figW / 2f;

        drawFront(cv, startX, top, s);
        drawBack(cv, startX + figW + gap, top, s);

        p.setStyle(Paint.Style.FILL);
        p.setTextSize(Ui.dp(getContext(), 9.5f));
        p.setTextAlign(Paint.Align.CENTER);
        p.setColor(Ui.TEXT3);
        cv.drawText("正面", startX, h - Ui.dp(getContext(), 4), p);
        cv.drawText("背面", startX + figW + gap, h - Ui.dp(getContext(), 4), p);
    }

    // ------------------------------------------------------------- silhouettes

    private void body(Canvas cv, float cx, float top, float s) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(bodyTone());
        cv.drawCircle(cx, top + 8 * s, 7.6f * s, p);                       // head
        cv.drawRect(cx - 2.6f * s, top + 14 * s, cx + 2.6f * s, top + 18 * s, p); // neck
        r.set(cx - 13 * s, top + 17 * s, cx + 13 * s, top + 58 * s);      // torso
        cv.drawRoundRect(r, 6 * s, 6 * s, p);
        r.set(cx - 21.5f * s, top + 18 * s, cx - 14 * s, top + 56 * s);   // arms
        cv.drawRoundRect(r, 4 * s, 4 * s, p);
        r.set(cx + 14 * s, top + 18 * s, cx + 21.5f * s, top + 56 * s);
        cv.drawRoundRect(r, 4 * s, 4 * s, p);
        r.set(cx - 12 * s, top + 57 * s, cx - 1.5f * s, top + 104 * s);   // legs
        cv.drawRoundRect(r, 5 * s, 5 * s, p);
        r.set(cx + 1.5f * s, top + 57 * s, cx + 12 * s, top + 104 * s);
        cv.drawRoundRect(r, 5 * s, 5 * s, p);
    }

    private void drawFront(Canvas cv, float cx, float top, float s) {
        body(cv, cx, top, s);
        p.setStyle(Paint.Style.FILL);

        // deltoids
        p.setColor(tone("frontDelt"));
        cv.drawCircle(cx - 12.5f * s, top + 20f * s, 4.6f * s, p);
        cv.drawCircle(cx + 12.5f * s, top + 20f * s, 4.6f * s, p);

        // chest
        p.setColor(tone("chest"));
        r.set(cx - 11f * s, top + 22.5f * s, cx - 1f * s, top + 34f * s);
        cv.drawRoundRect(r, 2.5f * s, 2.5f * s, p);
        r.set(cx + 1f * s, top + 22.5f * s, cx + 11f * s, top + 34f * s);
        cv.drawRoundRect(r, 2.5f * s, 2.5f * s, p);

        // abs
        p.setColor(tone("abs"));
        r.set(cx - 6.5f * s, top + 36f * s, cx + 6.5f * s, top + 54f * s);
        cv.drawRoundRect(r, 2f * s, 2f * s, p);

        // obliques
        p.setColor(tone("obliques"));
        r.set(cx - 10.5f * s, top + 36f * s, cx - 7.5f * s, top + 54f * s);
        cv.drawRoundRect(r, 1.5f * s, 1.5f * s, p);
        r.set(cx + 7.5f * s, top + 36f * s, cx + 10.5f * s, top + 54f * s);
        cv.drawRoundRect(r, 1.5f * s, 1.5f * s, p);

        // biceps / forearms
        p.setColor(tone("biceps"));
        r.set(cx - 20.5f * s, top + 24f * s, cx - 15f * s, top + 38f * s);
        cv.drawRoundRect(r, 2.5f * s, 2.5f * s, p);
        r.set(cx + 15f * s, top + 24f * s, cx + 20.5f * s, top + 38f * s);
        cv.drawRoundRect(r, 2.5f * s, 2.5f * s, p);

        p.setColor(tone("forearm"));
        r.set(cx - 20.5f * s, top + 39f * s, cx - 15.2f * s, top + 55f * s);
        cv.drawRoundRect(r, 2.5f * s, 2.5f * s, p);
        r.set(cx + 15.2f * s, top + 39f * s, cx + 20.5f * s, top + 55f * s);
        cv.drawRoundRect(r, 2.5f * s, 2.5f * s, p);

        // quads + adductors
        p.setColor(tone("quads"));
        r.set(cx - 11f * s, top + 59f * s, cx - 2.5f * s, top + 88f * s);
        cv.drawRoundRect(r, 3 * s, 3 * s, p);
        r.set(cx + 2.5f * s, top + 59f * s, cx + 11f * s, top + 88f * s);
        cv.drawRoundRect(r, 3 * s, 3 * s, p);

        p.setColor(tone("adductors"));
        r.set(cx - 2.2f * s, top + 60f * s, cx - 0.3f * s, top + 82f * s);
        cv.drawRoundRect(r, 1f * s, 1f * s, p);
        r.set(cx + 0.3f * s, top + 60f * s, cx + 2.2f * s, top + 82f * s);
        cv.drawRoundRect(r, 1f * s, 1f * s, p);

        // calves
        p.setColor(tone("calves"));
        r.set(cx - 10f * s, top + 90f * s, cx - 3.5f * s, top + 102f * s);
        cv.drawRoundRect(r, 3 * s, 3 * s, p);
        r.set(cx + 3.5f * s, top + 90f * s, cx + 10f * s, top + 102f * s);
        cv.drawRoundRect(r, 3 * s, 3 * s, p);
    }

    private void drawBack(Canvas cv, float cx, float top, float s) {
        body(cv, cx, top, s);
        p.setStyle(Paint.Style.FILL);

        // rear delts
        p.setColor(tone("rearDelt"));
        cv.drawCircle(cx - 12.5f * s, top + 20f * s, 4.6f * s, p);
        cv.drawCircle(cx + 12.5f * s, top + 20f * s, 4.6f * s, p);

        // traps
        p.setColor(tone("traps"));
        path.reset();
        path.moveTo(cx, top + 18f * s);
        path.lineTo(cx + 8.5f * s, top + 25f * s);
        path.lineTo(cx, top + 34f * s);
        path.lineTo(cx - 8.5f * s, top + 25f * s);
        path.close();
        cv.drawPath(path, p);

        // lats
        p.setColor(tone("lats"));
        r.set(cx - 11.5f * s, top + 26f * s, cx - 1f * s, top + 50f * s);
        cv.drawRoundRect(r, 3 * s, 3 * s, p);
        r.set(cx + 1f * s, top + 26f * s, cx + 11.5f * s, top + 50f * s);
        cv.drawRoundRect(r, 3 * s, 3 * s, p);

        // erectors
        p.setColor(tone("erector"));
        r.set(cx - 3.4f * s, top + 35f * s, cx - 0.6f * s, top + 55f * s);
        cv.drawRoundRect(r, 1.2f * s, 1.2f * s, p);
        r.set(cx + 0.6f * s, top + 35f * s, cx + 3.4f * s, top + 55f * s);
        cv.drawRoundRect(r, 1.2f * s, 1.2f * s, p);

        // triceps / forearms
        p.setColor(tone("triceps"));
        r.set(cx - 20.5f * s, top + 24f * s, cx - 15f * s, top + 40f * s);
        cv.drawRoundRect(r, 2.5f * s, 2.5f * s, p);
        r.set(cx + 15f * s, top + 24f * s, cx + 20.5f * s, top + 40f * s);
        cv.drawRoundRect(r, 2.5f * s, 2.5f * s, p);

        p.setColor(tone("forearm"));
        r.set(cx - 20.5f * s, top + 41f * s, cx - 15.2f * s, top + 55f * s);
        cv.drawRoundRect(r, 2.5f * s, 2.5f * s, p);
        r.set(cx + 15.2f * s, top + 41f * s, cx + 20.5f * s, top + 55f * s);
        cv.drawRoundRect(r, 2.5f * s, 2.5f * s, p);

        // glutes
        p.setColor(tone("glutes"));
        r.set(cx - 11f * s, top + 54f * s, cx - 0.6f * s, top + 63f * s);
        cv.drawRoundRect(r, 3.5f * s, 3.5f * s, p);
        r.set(cx + 0.6f * s, top + 54f * s, cx + 11f * s, top + 63f * s);
        cv.drawRoundRect(r, 3.5f * s, 3.5f * s, p);

        // hamstrings
        p.setColor(tone("hams"));
        r.set(cx - 11f * s, top + 63f * s, cx - 2.5f * s, top + 88f * s);
        cv.drawRoundRect(r, 3 * s, 3 * s, p);
        r.set(cx + 2.5f * s, top + 63f * s, cx + 11f * s, top + 88f * s);
        cv.drawRoundRect(r, 3 * s, 3 * s, p);

        // calves
        p.setColor(tone("calves"));
        r.set(cx - 10f * s, top + 90f * s, cx - 3.5f * s, top + 102f * s);
        cv.drawRoundRect(r, 3 * s, 3 * s, p);
        r.set(cx + 3.5f * s, top + 90f * s, cx + 10f * s, top + 102f * s);
        cv.drawRoundRect(r, 3 * s, 3 * s, p);
    }
}
