package com.ironlog.train.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ironlog.train.data.BodyMetric;
import com.ironlog.train.data.Store;
import com.ironlog.train.data.WorkoutSession;
import com.ironlog.train.logic.Fmt;
import com.ironlog.train.logic.Stats;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;

public class ProfileScreen extends Screen {

    public ProfileScreen(MainActivity a) {
        super(a);
    }

    @Override
    protected void build() {
        final Store store = Store.get(ctx);
        List<WorkoutSession> finished = store.finishedSessions();

        LinearLayout body = Ui.col(ctx);
        body.setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 20), Ui.dp(ctx, 18), Ui.dp(ctx, 28));

        // ---------------------------------------------------------- header
        LinearLayout titles = Ui.col(ctx);
        titles.addView(Ui.label(ctx, "本机账号"));
        titles.addView(Ui.title(ctx, "我的"));
        body.addView(titles);

        body.addView(Ui.spacer(ctx, 18));

        // --------------------------------------------------------- profile
        LinearLayout profile = Ui.row(ctx);
        profile.setBackground(Ui.bg(Ui.SURFACE, 16, ctx));
        Ui.pad(profile, 16, 16, 16, 16);

        TextView avatar = new TextView(ctx);
        String initial = store.settings.displayName.isEmpty() ? "铁" : store.settings.displayName.substring(0, 1);
        avatar.setText(initial);
        avatar.setTextSize(20f);
        avatar.setTypeface(Ui.BOLD);
        avatar.setTextColor(Ui.ON_ACCENT);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Ui.circle(Ui.ACCENT, ctx));
        profile.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(ctx, 46), Ui.dp(ctx, 46)));

        LinearLayout nameBox = Ui.col(ctx);
        nameBox.setPadding(Ui.dp(ctx, 14), 0, 0, 0);
        String nick = store.settings.displayName.isEmpty() ? "点击设置昵称" : store.settings.displayName;
        nameBox.addView(Ui.bold(ctx, nick, 16f, Ui.TEXT));
        nameBox.addView(Ui.t(ctx, "无需登录 · 数据只在这台设备", 11.5f, Ui.TEXT3));
        profile.addView(nameBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView edit = Ui.t(ctx, "编辑", 12f, Ui.TEXT2);
        Icons.start(edit, ctx, com.ironlog.train.R.drawable.ic_edit, Ui.TEXT2, 12, 4);
        edit.setPadding(Ui.dp(ctx, 8), Ui.dp(ctx, 6), 0, Ui.dp(ctx, 6));
        edit.setClickable(true);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialogs.prompt(ctx, "昵称", "怎么称呼你", store.settings.displayName,
                        new Dialogs.TextCallback() {
                            @Override
                            public void onText(String text) {
                                store.settings.displayName = text;
                                store.save();
                                rebuild();
                            }
                        });
            }
        });
        profile.addView(edit);

        profile.setClickable(true);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.push(new BodyScreen(act));
            }
        });
        body.addView(profile);

        body.addView(Ui.spacer(ctx, 14));

        // ----------------------------------------------------- body metrics
        LinearLayout bodyCard = Ui.card(ctx);
        LinearLayout bh = Ui.row(ctx);
        bh.addView(Ui.sectionTitle(ctx, "身体数据"),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView open = Ui.t(ctx, "记录", 12f, Ui.ACCENT);
        Icons.end(open, ctx, com.ironlog.train.R.drawable.ic_chevron_right, Ui.ACCENT, 11, 1);
        open.setClickable(true);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.push(new BodyScreen(act));
            }
        });
        bh.addView(open);
        bodyCard.addView(bh);
        bodyCard.addView(Ui.spacer(ctx, 12));

        BodyMetric latest = null;
        for (BodyMetric m : store.metrics) {
            if (latest == null || m.date > latest.date) {
                latest = m;
            }
        }
        LinearLayout metrics = Ui.row(ctx);
        metrics.addView(bodyStat("体重", latest != null && latest.weight > 0
                ? Fmt.num1(latest.weight) : "--", "kg"), Ui.weight(ctx, 1));
        metrics.addView(bodyStat("体脂", latest != null && latest.bodyFat > 0
                ? Fmt.num1(latest.bodyFat) : "--", "%"), Ui.weight(ctx, 1));
        metrics.addView(bodyStat("腰围", latest != null && latest.waist > 0
                ? Fmt.num1(latest.waist) : "--", "cm"), Ui.weight(ctx, 1));
        bodyCard.addView(metrics);
        if (latest != null) {
            bodyCard.addView(Ui.spacer(ctx, 8));
            bodyCard.addView(Ui.t(ctx, "更新于 " + Fmt.iso(latest.date), 10.5f, Ui.TEXT3));
        }
        body.addView(bodyCard);

        body.addView(Ui.spacer(ctx, 14));

        // ------------------------------------------------------- preferences
        LinearLayout prefs = Ui.card(ctx);
        prefs.addView(Ui.sectionTitle(ctx, "外观"));
        prefs.addView(Ui.spacer(ctx, 10));

        prefs.addView(toggleRow("深色模式", store.settings.darkMode, new Runnable() {
            @Override
            public void run() {
                store.settings.darkMode = !store.settings.darkMode;
                store.save();
                Ui.applyTheme(store.settings.darkMode);
                act.recreate();
            }
        }));

        body.addView(prefs);
        body.addView(Ui.spacer(ctx, 14));

        LinearLayout prefs2 = Ui.card(ctx);
        prefs2.addView(Ui.sectionTitle(ctx, "偏好"));
        prefs2.addView(Ui.spacer(ctx, 10));

        prefs2.addView(toggleRow("训练时保持屏幕常亮", store.settings.keepAwake,
                new Runnable() {
                    @Override
                    public void run() {
                        store.settings.keepAwake = !store.settings.keepAwake;
                        store.save();
                        rebuild();
                    }
                }));

        prefs2.addView(Ui.spacer(ctx, 10));
        prefs2.addView(toggleRow("休息结束震动提醒", store.settings.restSound,
                new Runnable() {
                    @Override
                    public void run() {
                        store.settings.restSound = !store.settings.restSound;
                        store.save();
                        rebuild();
                    }
                }));
        body.addView(prefs2);

        body.addView(Ui.spacer(ctx, 14));

        // ---------------------------------------------------------- reminders
        LinearLayout remind = Ui.card(ctx);
        remind.addView(Ui.sectionTitle(ctx, "提醒与日历"));
        remind.addView(Ui.t(ctx, "当天有计划但还没练时提醒你", 11.5f, Ui.TEXT3));
        remind.addView(Ui.spacer(ctx, 10));

        remind.addView(toggleRow("每天提醒", store.settings.reminderEnabled, new Runnable() {
            @Override
            public void run() {
                final boolean enable = !store.settings.reminderEnabled;
                if (!enable) {
                    store.settings.reminderEnabled = false;
                    store.save();
                    com.ironlog.train.notify.PlanReminders.schedule(ctx);
                    rebuild();
                    return;
                }
                Runnable done = new Runnable() {
                    @Override
                    public void run() {
                        store.settings.reminderEnabled = true;
                        store.save();
                        com.ironlog.train.notify.PlanReminders.schedule(ctx);
                        act.toast("已开启，每天 " + String.format(java.util.Locale.US, "%02d:%02d",
                                store.settings.reminderHour, store.settings.reminderMinute) + " 检查");
                        rebuild();
                    }
                };
                if (Build.VERSION.SDK_INT >= 33) {
                    act.ensurePermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, done);
                } else {
                    done.run();
                }
            }
        }));

        remind.addView(Ui.spacer(ctx, 10));

        LinearLayout timeRow = Ui.row(ctx);
        timeRow.setClickable(true);
        timeRow.addView(Ui.t(ctx, "提醒时间", 13.5f, Ui.TEXT),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        timeRow.addView(Ui.num(ctx, String.format(java.util.Locale.US, "%02d:%02d",
                store.settings.reminderHour, store.settings.reminderMinute), 13f, Ui.ACCENT));
        timeRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialogs.time(ctx, store.settings.reminderHour, store.settings.reminderMinute,
                        new Dialogs.TimeCallback() {
                            @Override
                            public void onTime(int hour, int minute) {
                                store.settings.reminderHour = hour;
                                store.settings.reminderMinute = minute;
                                store.save();
                                com.ironlog.train.notify.PlanReminders.schedule(ctx);
                                rebuild();
                            }
                        });
            }
        });
        remind.addView(timeRow);

        remind.addView(Ui.spacer(ctx, 12));

        TextView syncCal = Icons.ghost(ctx, com.ironlog.train.R.drawable.ic_calendar,
                "写入系统日历（未来 4 周）");
        syncCal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.ensurePermissions(new String[]{
                        android.Manifest.permission.READ_CALENDAR,
                        android.Manifest.permission.WRITE_CALENDAR
                }, new Runnable() {
                    @Override
                    public void run() {
                        act.toast("正在写入…");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final int n = com.ironlog.train.notify.CalendarSync.export(ctx, 4);
                                act.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (n >= 0) {
                                            act.toast("已写入 " + n + " 条日历事件");
                                        } else if (n == com.ironlog.train.notify.CalendarSync.ERR_NO_CALENDAR) {
                                            act.toast("没找到可写日历，请先在日历 App 里建一个本地日历");
                                        } else {
                                            act.toast("写入失败，请检查日历权限");
                                        }
                                    }
                                });
                            }
                        }).start();
                    }
                });
            }
        });
        remind.addView(syncCal, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        remind.addView(Ui.spacer(ctx, 8));
        remind.addView(Ui.t(ctx, "写入后由手机日历负责提醒；重复同步会先清掉旧的计划事件。",
                10.5f, Ui.TEXT3));
        body.addView(remind);

        body.addView(Ui.spacer(ctx, 14));

        // -------------------------------------------------------------- data
        LinearLayout data = Ui.card(ctx);
        data.addView(Ui.sectionTitle(ctx, "数据"));
        data.addView(Ui.t(ctx, "导出一个 JSON 文件，可换机恢复", 11.5f, Ui.TEXT3));
        data.addView(Ui.spacer(ctx, 12));

        TextView export = Icons.ghost(ctx, com.ironlog.train.R.drawable.ic_export, "导出备份");
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportBackup();
            }
        });
        data.addView(export, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        data.addView(Ui.spacer(ctx, 8));

        TextView importBtn = Icons.ghost(ctx, com.ironlog.train.R.drawable.ic_import, "导入备份");
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importBackup();
            }
        });
        data.addView(importBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        data.addView(Ui.spacer(ctx, 8));

        TextView wipe = Icons.button(ctx, com.ironlog.train.R.drawable.ic_trash,
                "清空全部数据", Ui.RED, 0x1FE2605F);
        wipe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialogs.confirm(ctx, "清空数据", "所有训练、计划和体测数据都会被删除，且无法恢复。", new Runnable() {
                    @Override
                    public void run() {
                        store.wipe();
                        act.toast("已清空");
                        act.reloadCurrent();
                    }
                });
            }
        });
        data.addView(wipe, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(data);

        body.addView(Ui.spacer(ctx, 14));

        // ------------------------------------------------------------- about
        LinearLayout about = Ui.card(ctx);
        about.addView(Ui.sectionTitle(ctx, "关于"));
        about.addView(Ui.spacer(ctx, 8));
        about.addView(Ui.t(ctx, "IronLog 铁记 1.0.0", 12.5f, Ui.TEXT2));
        about.addView(Ui.t(ctx, "离线优先的训练记录工具，没有账号、没有内购、没有广告。"
                + "所有数据保存在本机应用目录，卸载即删除。", 11.5f, Ui.TEXT3));
        body.addView(about);

        addView(Ui.scroll(ctx, body), new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private View bodyStat(String label, String value, String unit) {
        LinearLayout c = Ui.col(ctx);
        c.addView(Ui.label(ctx, label));
        LinearLayout line = Ui.row(ctx);
        line.addView(Ui.num(ctx, value, 18f, Ui.TEXT));
        line.addView(Ui.t(ctx, " " + unit, 9.5f, Ui.TEXT3));
        c.addView(line);
        return c;
    }

    private View toggleRow(String label, boolean on, final Runnable onTap) {
        LinearLayout row = Ui.row(ctx);
        row.setClickable(true);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTap.run();
            }
        });

        row.addView(Ui.t(ctx, label, 13.5f, Ui.TEXT),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout track = Ui.row(ctx);
        track.setBackground(Ui.bg(on ? Ui.ACCENT : Ui.SURFACE2, 20, ctx));
        track.setGravity(on ? Gravity.END : Gravity.START);
        track.setPadding(Ui.dp(ctx, 3), Ui.dp(ctx, 3), Ui.dp(ctx, 3), Ui.dp(ctx, 3));
        View knob = new View(ctx);
        knob.setBackground(Ui.circle(on ? Ui.ON_ACCENT : Ui.TEXT3, ctx));
        track.addView(knob, new LinearLayout.LayoutParams(Ui.dp(ctx, 16), Ui.dp(ctx, 16)));
        row.addView(track, new LinearLayout.LayoutParams(Ui.dp(ctx, 38), Ui.dp(ctx, 22)));
        return row;
    }

    // ------------------------------------------------------------------ io

    private void exportBackup() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "ironlog-backup-" + Fmt.iso(System.currentTimeMillis()) + ".json");
        act.startForResult(i, new MainActivity.ResultHandler() {
            @Override
            public void onResult(int resultCode, Intent data) {
                if (resultCode != android.app.Activity.RESULT_OK || data == null) {
                    return;
                }
                Uri uri = data.getData();
                if (uri == null) {
                    return;
                }
                try {
                    OutputStream os = ctx.getContentResolver().openOutputStream(uri);
                    if (os == null) {
                        act.toast("无法写入");
                        return;
                    }
                    os.write(Store.get(ctx).exportJson().getBytes(Charset.forName("UTF-8")));
                    os.flush();
                    os.close();
                    act.toast("备份已保存");
                } catch (Exception e) {
                    act.toast("导出失败：" + e.getMessage());
                }
            }
        });
    }

    private void importBackup() {
        Dialogs.confirm(ctx, "导入备份", "导入会覆盖当前全部数据，确定吗？", new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                act.startForResult(i, new MainActivity.ResultHandler() {
                    @Override
                    public void onResult(int resultCode, Intent data) {
                        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
                            return;
                        }
                        Uri uri = data.getData();
                        if (uri == null) {
                            return;
                        }
                        try {
                            InputStream is = ctx.getContentResolver().openInputStream(uri);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            byte[] buf = new byte[8192];
                            int n;
                            while (is != null && (n = is.read(buf)) > 0) {
                                bos.write(buf, 0, n);
                            }
                            if (is != null) {
                                is.close();
                            }
                            String text = new String(bos.toByteArray(), Charset.forName("UTF-8"));
                            boolean ok = Store.get(ctx).importJson(text);
                            act.toast(ok ? "导入成功" : "文件格式不对");
                            if (ok) {
                                act.reloadCurrent();
                            }
                        } catch (Exception e) {
                            act.toast("导入失败：" + e.getMessage());
                        }
                    }
                });
            }
        });
    }
}
