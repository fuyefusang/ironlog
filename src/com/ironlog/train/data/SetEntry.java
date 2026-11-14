package com.ironlog.train.data;

public class SetEntry {
    public static final int NORMAL = 0;
    public static final int WARMUP = 1;
    public static final int DROP = 2;

    public double weight;
    public int reps;
    public double rpe;
    public boolean done;
    public int type = NORMAL;
    /** 次数组内附加：器械/自重动作可只记次数 */
    public boolean bodyweight;

    public SetEntry() {
    }

    public SetEntry(double weight, int reps) {
        this.weight = weight;
        this.reps = reps;
    }

    public SetEntry(double weight, int reps, boolean done) {
        this.weight = weight;
        this.reps = reps;
        this.done = done;
    }

    public SetEntry copy() {
        SetEntry s = new SetEntry(weight, reps, done);
        s.rpe = rpe;
        s.type = type;
        s.bodyweight = bodyweight;
        return s;
    }
}
