# IronLog UI 验证报告（模拟器实跑）

> 2026-11-14 13:14 起，Android 30 / x86_64 / AEHD 加速模拟器，1080×2280 @ 440dpi。
> 全部截图在 `screenshots/`，文件名与下文一致。

---

## 1. 怎么跑起来的（可复现）

| 步骤 | 做法 | 坑 |
|---|---|---|
| 系统镜像 | `tools/resume-sysimg.mjs` 用 HTTP Range 断点续传到 `tools/x86_64-30_r11.zip`（676,379,777 B） | 上个会话只下到 439.5 MB；`fs.createWriteStream` 追加即可续传 |
| 解压 | 从 zip 里单独抽 `x86_64/system.img`（3.2 GB）到 `tools/sdk/system-images/android-30/default/x86_64/` | 该目录**只读**（SDK 在 AppData 下），所以整套镜像放在工作区，不走 `%LOCALAPPDATA%` |
| AVD | 手写 `tools/avd/IronLog.ini` + `IronLog.avd/config.ini`（pixel_4、x86_64、swiftshader_indirect） | 没有 cmdline-tools，`avdmanager` 用不了 |
| 启动 | `emulator -avd IronLog -sysdir <工作区镜像目录> -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect` | 见下两条 |
| 加速 | **必须**在完全权限（danger-full-access）下启动 | 沙箱下 `-accel-check` 报 `Unable to open AEHD device: ERROR_ACCESS_DENIED`，x86_64 镜像会直接拒绝启动 |
| 写入目录 | `ANDROID_AVD_HOME=tools/avd`、`ANDROID_EMULATOR_HOME=tools/emu-home` | 沙箱下 `~/.android/emu-last-feature-flags.protobuf.lock` 写不进去，模拟器会刷几十万行错误 |

`-sysdir` 是关键：模拟器默认从**自身所在 SDK**（`%LOCALAPPDATA%\Android\Sdk\system-images\...`）找镜像，那里缺 `system.img` 且不可写；`-sysdir` 指到工作区即可，`ANDROID_SDK_ROOT` 对它无效。

构建同样需要在完全权限下跑：`ironlog/build/classes.jar` 在沙箱模式下删不掉也写不了（不是 ACL 问题，诊断脚本判定 `NOT_THIS_CLASS`，属于 DSH 文件沙箱的拒绝）。

---

## 2. 逐页验证结果

| 页面 | 截图 | 结论 |
|---|---|---|
| 首页（空） | `01-home.png` | ✅ 日期、三格统计、开始训练/计划、空态 |
| 首页（有记录 + 计划） | `40-home-with-plans.png` | ✅ 连续周徽标、快速开始计划列表、最近训练行、草稿「训练进行中」卡片 |
| 训练页（空） | `41-workout-empty2.png` | ✅ 顶栏固定、空态提示、底部双按钮 |
| 训练页（加动作后） | `42-workout-sets-fixed.png` | ✅ 「上次 60×82」提示、3 组默认值、添加一组 |
| 训练页（计时中） | `08-workout-running.png` | ✅ 计时 00:06 起走、双按钮变单个「添加动作」 |
| 训练页（填组） | `09-set-filled.png` | ✅ 数字键盘、kg/次 后缀 |
| 选动作（全部） | `03-picker.png` | ✅ 搜索框、肌群筛选 chip、人体图正/背面、分组列表 |
| 选动作（展开） | `04-picker-variants.png` | ✅ 每个动作的「主要/辅助」肌肉文案 |
| 选动作（选中） | `05-picker-selected.png` | ✅ 人体图高亮胸/前束/三头、已选计数、浮动「添加 1 个动作」 |
| 训练详情 | `11-session-detail.png` | ✅ 时长/容量/组数/动作数、每组明细、e1RM、删除入口 |
| 训练日历 | `04-history.png` | ✅ 月历、完成标记、当天记录、本月全部 |
| 数据页 | `12-stats-top.png` / `24-plans-visible.png` / `31-fixed-chart.png` | ✅ 总览、计划日历（实心=已完成，空心圈=计划日）、训练计划卡、力量趋势、每周容量、肌群分布、个人记录 |
| 我的 | `15-profile.png` / `19-light-profile.png` | ✅ 昵称、身体数据摘要、深色模式开关、偏好、提醒与日历 |
| 身体数据 | `16-body-metric-dialog.png` / `17-body-saved.png` | ✅ 7 个围度输入、保存后变「更新今天的记录」、趋势图、历史记录 |
| 编辑计划 | `36-routine-edit.png` | ✅ 名称/备注、每周安排 chip、每个动作的组数/次数/重量/休息 |
| 新建计划对话框 | `32-fixed-dialog-light.png` | ✅ 修复后：圆角、配色跟随主题、有「取消」 |
| 选择分化（模板） | `33-fixed-template-list.png` | ✅ 修复后：提示语 + 6 个模板 + 取消 |
| 提醒时间滚筒 | `45-dark-dialog.png` | ✅ NumberPicker 时/分、深色主题下可读 |
| 日历权限 | `46-calendar-permission.png` | ✅ 运行时申请 READ/WRITE_CALENDAR，允许后无异常（模拟器无日历账户，故 0 条事件） |

---

## 3. 本轮发现并修好的问题

| # | 问题 | 证据 | 修法 |
|---|---|---|---|
| 1 | **选择分化对话框只显示说明文字，模板列表根本不渲染** —— 功能完全不可用 | `26-template-dialog.png` | `setItems` 与 `setMessage` 同时设置时平台只显示 message。改为 `Dialogs.list()`：说明文字放进 `setCustomTitle`，列表用 `setItems` |
| 2 | 力量趋势/体重趋势**只有一个数据点**时：横轴两端重复同一日期、圆点画在正中与标签对不齐、看不出数值 | `24-plans-visible.png`、`17-body-saved.png` | `LineChartView` 单点时圆点下方居中画唯一日期，并在圆点上方直接标数值（`31-fixed-chart.png` 为修复后） |
| 3 | 所有 `AlertDialog` 用平台默认底（深色 `#424242` / 浅色纯白、直角），和应用的圆角卡片配色不搭；列表型对话框还会丢掉「取消」按钮 | `18-reminder-picker.png`、`25-new-plan-dialog.png` | 新增 `Theme.IronLog.Dialog(.Light)` + `dialog_bg_dark/light.xml`（18dp 圆角、描边），`Dialogs.show()` 再把标题/正文/按钮颜色按当前调色板刷一遍；列表对话框统一走 `Dialogs.list()` |
| 4 | 数字输入框聚焦后是**追加**而不是替换：字段里已有 8，输入 12 得到 812 | `09-set-filled.png` | `Ui.input()` 里在手指抬起后 `post(selectAll)`（`setSelectAllOnFocus` 会被光标落点覆盖）。组数/次数输入框还顺手去掉了和右侧「kg / 次」重复的 hint |

构建后重新安装验证：`42/44/fixed-*` 系列截图。

---

## 4. 仍然存在的问题（未改，记录在案）

1. **数字框点击即全选**：想改中间一位（比如 60 改 65）现在得删了重打。取舍是短数字更常见，若用户嫌烦可改成「首次聚焦全选、再次点击定位光标」。
2. **身体数据表单在键盘弹出时**要手动上滑才看得到「保存」（`windowSoftInputMode=adjustResize` 生效，只是没自动滚到按钮）。
3. **提醒时间滚筒**选中行是灰色（平台 NumberPicker 自带配色），深色下略暗，可用但不够亮。
4. **计划日历**里「今天」和「已完成」共用实心圆样式，当两者重叠时含义不唯一。
5. 模拟器没有日历账户，**写入系统日历**只能验证到权限流程与不崩溃，写事件本身未验证。
6. 只在 **API 30** 上跑过；targetSdk 36 的 API 35+ 强制 edge-to-edge、预测性返回等行为没在本机验证。
7. 通知/闹钟（`AlarmManager` 计划提醒）未做端到端验证。

---

## 5. 结论

- 应用在真实 Android 运行时上**能装、能起、三个页签全部可达**，没有崩溃、没有 ANR。
- 计时的启动/走表、组数据的保存与回填、训练结束后首页/数据页的统计联动都是真的（4,920 kg = 60×82，e1RM 224 = 60×(1+82/30)）。
- 按钮文案、日期、单位、中文字距、明暗两套调色板在 1080×2280 上排版正常，没有被状态栏/导航栏遮挡（`InsetsHelper` 生效）。
- 本轮修掉的 4 个问题里有 1 个是**功能性阻塞**（模板列表不渲染），其余 3 个是明显影响观感/手感的缺陷。修复后包体 110.2 KB。
