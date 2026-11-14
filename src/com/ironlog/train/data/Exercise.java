package com.ironlog.train.data;

public class Exercise {
    public String id;
    public String name;
    public String muscle;      // 胸 / 背 / 腿 / 肩 / 手臂 / 核心 / 有氧
    public String equipment;   // 杠铃 / 哑铃 / 器械 / 绳索 / 自重 / 其他
    /** 同一动作的不同器械变体归为一组，例如"卧推"下含杠铃/哑铃两个版本。 */
    public String family;
    /** 主要刺激的肌肉，逗号分隔。 */
    public String primary;
    /** 协同发力的肌肉，逗号分隔。 */
    public String secondary;
    public boolean custom;

    public Exercise() {
    }

    public Exercise(String id, String name, String muscle, String equipment,
                    String family, String primary, String secondary, boolean custom) {
        this.id = id;
        this.name = name;
        this.muscle = muscle;
        this.equipment = equipment;
        this.family = family;
        this.primary = primary;
        this.secondary = secondary;
        this.custom = custom;
    }

    public String familyOrName() {
        return family == null || family.isEmpty() ? name : family;
    }
}
