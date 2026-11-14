package com.ironlog.train.data;

/** Ready-made training splits so a plan can be created in two taps. */
public final class Templates {

    public static final String[] NAMES = {
            "全身 · 每周 2 练",
            "全身 · 每周 3 练",
            "上下肢 · 每周 4 练",
            "推拉腿 · 每周 3 练",
            "推拉腿 · 每周 6 练",
            "五分化 · 每周 5 练"
    };

    public static final String[] DESCS = {
            "周一 / 周四 · 每次全身，适合新手或时间少",
            "周一 / 周三 / 周五 · 全身循环，频率高",
            "上肢 / 下肢交替，周一二、周四五",
            "推 / 拉 / 腿 各一次，最经典的三分化",
            "推 / 拉 / 腿 各练两次，适合恢复好的人",
            "胸 / 背 / 腿 / 肩 / 手臂，每个部位单独一天"
    };

    private Templates() {
    }

    private static final String[][] PUSH = {
            {"c_bench_bb", "4", "8"},
            {"c_bench_incline_db", "3", "10"},
            {"s_press_db", "3", "10"},
            {"a_ext_rope", "3", "12"}
    };

    private static final String[][] PULL = {
            {"b_pulldown", "4", "10"},
            {"b_row_bb", "4", "8"},
            {"b_row_cable", "3", "12"},
            {"a_curl_db", "3", "12"}
    };

    private static final String[][] LEGS = {
            {"l_squat_bb", "4", "8"},
            {"l_leg_press", "3", "10"},
            {"l_curl", "3", "12"},
            {"l_calf_standing", "4", "15"}
    };

    private static final String[][] UPPER = {
            {"c_bench_bb", "4", "8"},
            {"b_pulldown", "4", "10"},
            {"s_press_db", "3", "10"},
            {"b_row_cable", "3", "12"},
            {"a_curl_db", "3", "12"},
            {"a_ext_rope", "3", "12"}
    };

    private static final String[][] LOWER = {
            {"l_squat_bb", "4", "8"},
            {"l_leg_press", "3", "10"},
            {"l_curl", "3", "12"},
            {"l_calf_standing", "4", "15"}
    };

    private static final String[][] FULL = {
            {"l_squat_bb", "3", "8"},
            {"c_bench_bb", "3", "8"},
            {"b_row_bb", "3", "10"},
            {"s_press_bb", "3", "10"}
    };

    private static final String[][] CHEST = {
            {"c_bench_bb", "4", "8"},
            {"c_bench_incline_db", "4", "10"},
            {"c_fly_cable", "3", "12"},
            {"c_dip_chest", "3", "10"}
    };

    private static final String[][] BACK = {
            {"b_pulldown", "4", "10"},
            {"b_row_bb", "4", "8"},
            {"b_row_cable", "3", "12"},
            {"b_deadlift_rdl", "3", "8"}
    };

    private static final String[][] SHOULDER = {
            {"s_press_db", "4", "10"},
            {"s_lateral_db", "4", "12"},
            {"s_rear_fly_db", "3", "12"},
            {"s_face_pull", "3", "15"}
    };

    private static final String[][] ARMS = {
            {"a_curl_bb", "4", "10"},
            {"a_curl_hammer", "3", "12"},
            {"a_ext_rope", "4", "12"},
            {"a_ext_skull", "3", "10"}
    };

    private static Routine make(String name, int days, String[][] exercises) {
        Routine r = new Routine();
        r.id = "r_" + Store.newId();
        r.name = name;
        r.days = days;
        r.updatedAt = System.currentTimeMillis();
        for (String[] e : exercises) {
            RoutineExercise ri = new RoutineExercise(e[0], Integer.parseInt(e[1]),
                    Integer.parseInt(e[2]), 0);
            ri.restSec = 90;
            r.items.add(ri);
        }
        return r;
    }

    /** Creates the routines of template {@code index} and returns how many were added. */
    public static int apply(Store store, int index) {
        java.util.List<Routine> created = new java.util.ArrayList<>();
        switch (index) {
            case 0:
                created.add(make("全身 A", 0b0000001, FULL));
                created.add(make("全身 B", 0b0001000, FULL));
                break;
            case 1:
                created.add(make("全身 A", 0b0000101, FULL));
                created.add(make("全身 B", 0b0010100, FULL));
                break;
            case 2:
                created.add(make("上肢 A", 0b0000001, UPPER));
                created.add(make("下肢 A", 0b0000010, LOWER));
                created.add(make("上肢 B", 0b0001000, UPPER));
                created.add(make("下肢 B", 0b0010000, LOWER));
                break;
            case 3:
                created.add(make("推日", 0b0000001, PUSH));
                created.add(make("拉日", 0b0000100, PULL));
                created.add(make("腿日", 0b0010000, LEGS));
                break;
            case 4:
                created.add(make("推日 A", 0b0000001, PUSH));
                created.add(make("拉日 A", 0b0000010, PULL));
                created.add(make("腿日 A", 0b0000100, LEGS));
                created.add(make("推日 B", 0b0001000, PUSH));
                created.add(make("拉日 B", 0b0010000, PULL));
                created.add(make("腿日 B", 0b0100000, LEGS));
                break;
            case 5:
                created.add(make("胸", 0b0000001, CHEST));
                created.add(make("背", 0b0000010, BACK));
                created.add(make("腿", 0b0000100, LEGS));
                created.add(make("肩", 0b0001000, SHOULDER));
                created.add(make("手臂", 0b0010000, ARMS));
                break;
            default:
                return 0;
        }
        for (Routine r : created) {
            store.routines.add(r);
        }
        store.save();
        return created.size();
    }
}
