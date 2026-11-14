# IronLog（铁记）交接说明

> 用于换新会话时快速接续。新会话第一句话可以直接说：
> 「读 `ironlog/HANDOFF.md`，继续把模拟器跑起来验证 UI」。

---

## 1. 项目是什么

一个**完全离线**的健身训练记录 Android App，原生 Java 手写，无 Gradle、无 AndroidX、无第三方库、无位图、无网络权限。

- 包名：`com.ironlog.train`
- 应用名：铁记
- minSdk 26 / targetSdk 36
- 位置：工作区下的 `ironlog/`（本机路径见 `.gitignore` 之外，仓库内不写绝对路径）
- 产物：`dist/IronLog-1.0.0.apk`（110.2 KB）、`dist/IronLog-1.0.0-src.zip`
- 签名：`ironlog-debug.keystore`，别名 `ironlog`，密码 `android`
- 许可：**Apache-2.0**（`LICENSE` 已是官方全文 + `NOTICE`）。2026-11-14 由 GPL-3.0-or-later 改过来

**重要前提**：这是从零独立实现，不是对「训记」的破解或修改。用户原始的 `base.apk` 全程未被改动。

---

## 2. 改动时间线

时间取自会话时间戳，精确到分钟，部分为区间。

| 时间（2026-11-13/14） | 主题 | 改动内容 |
|---|---|---|
| 23:42 | 初始请求 | 用户要求破解训记 APK、去掉注册登录与充值。**拒绝**：涉及著作权、技术保护措施与付费绕过；改为提供同类功能的独立实现方案 |
| 23:44–23:52 | 立项与工具链 | 确认环境（JDK 21、SDK android-36、build-tools 36）；Google Maven 不可达，放弃 Gradle，改用 `aapt2 + javac + d8 + zipalign + apksigner` 手工构建；写完数据层、统计逻辑、10 个页面、100 动作库；**首包 72.5 KB，构建通过** |
| 23:52–23:58 | 体积质疑 | 用户质疑 73 KB 是否缺文件；列出 dex/清单/资源构成解释；同时开始把文字符号图标换成矢量图 |
| 23:58–00:02 | 矢量图标 | 生成 20 个 VectorDrawable，替换页签、返回、勾选、菜单、删除等全部 glyph，包体 85.8 KB |
| 00:02–00:08 | **启动 bug** | 用户反馈打开只有 "IronLog boot OK"。根因：最早的工具链测试 `com.ironlog.train.MainActivity` 未删，清单仍指向它。删除空壳、清单改指 `.ui.MainActivity`；**新增构建闸门**：清单声明的类必须存在于 dex；页面构建异常改为显示错误卡片而非崩溃 |
| 00:08–00:12 | 许可与工程化 | 起初选 GPL-3.0-or-later，加 `LICENSE`、`.gitignore`；构建脚本支持 `ANDROID_SDK_ROOT`/`ANDROID_HOME`，去掉 Windows 硬编码路径 |
| 00:12–00:27 | 四项需求 | ①明暗主题（运行时可切换调色板 + `recreate`）②计划并入「数据」页，页签 4→3，新增计划日历（实心点=已完成，空心圈=计划日）③训练暂停（`pausedAt`/`pausedAccumMs`，计时扣减）④选择动作页人体图 |
| 00:27–00:46 | 六项问题 | ①底部导航配色写死 → 收口到 `Ui.NAV/GRID`；切换主题跳回训练页 → `onSaveInstanceState` 保存页签 ②计时提前开始 → 延后到添加动作 ③肌肉图文案不够具体 ④选择页顶栏固定、返回箭头常驻 ⑤**「完成」永远判定空**：只统计打勾的组 → 改为「填了次数或重量就算完成」并自动补勾 ⑥计划改为分化模板（全身 2/3 练、上下肢 4 练、推拉腿 3/6 练、五分化 5 练） |
| 00:46–01:05 | 五项需求 | ①提醒时间改 `NumberPicker` 滚筒 ②身体数据按日期编辑（今天/往日分离，历史行可点进去改）③**动作级肌肉表**：95 个动作，每个标注主要/辅助肌肉 ④最近训练行显示动作清单 ⑤人体图重绘为 16 个独立部位 |
| 01:05–01:18 | 全面检查 | 修 4 个问题：自建动作人体图不亮（肌群名未展开）、未点亮肌肉与轮廓同色、翻月后选中日仍是上月、肌名「上部/长头」等后缀漏映射；新增 `CatalogTest`（714 条断言）；构建增加清单类校验 |
| 01:18–01:30 | 两项反馈 | ①选完动作后**不自动计时**，底部出现「开始训练 / 添加动作」，点开始才走表；首页「开始训练」旁加小「计划」按钮 ②**最近训练空白**：`Ui.col()` 默认 `MATCH_PARENT`，在水平行里挤掉左侧权重列 → 改显式 `WRAP_CONTENT` |
| 01:30–01:35 | 模拟器验证（首次会话） | AEHD 加速在放行权限后可用；但 SDK 内两个系统镜像都缺 `system.img`；开始后台下载 `x86_64-30_r11.zip`，下到 439.5/645 MB 会话结束 |
| 13:14–13:20 | **模拟器跑通** | Range 续传补齐 645 MB → 抽 `system.img`（3.2 GB）到工作区镜像目录 → 手写 AVD → `-sysdir` 指向工作区 + 完全权限启动（沙箱下 `.android` 与 AEHD 设备都不可写/不可开）→ `adb install` 成功 |
| 13:20–13:35 | 逐页截图 | 44 张截图，见 `UI-VERIFICATION.md` 与 `screenshots/`。首次真机级验证覆盖首页/训练/选动作/详情/日历/数据/我的/身体数据/编辑计划/各类对话框/明暗主题/权限流程 |
| 13:35–13:55 | 修 4 个 UI 缺陷 | ①模板列表对话框不渲染（`setItems`+`setMessage` 冲突，功能阻塞）②折线图单点重复日期+圆点错位 ③AlertDialog 平台默认底与配色不搭、列表对话框丢「取消」④数字框聚焦后追加而非替换。新增 `Theme.IronLog.Dialog(.Light)`、`dialog_bg_*.xml`、`Dialogs.list()`。重建 110.2 KB 并回装复验 |
| 15:47–16:05 | 换协议 + 准备开源 | 用户否决 GPL → 改 **Apache-2.0**（官方全文 11,358 B + `NOTICE`）。README 更新截图/验证/构建/限制章节；`.gitignore` 排除 12 GB 模拟器素材；`git init` 首次提交 |

---

## 3. 当前状态

**已完成并可构建**

```
APK      110.2 KB
Java     约 6600 行 / 33 个文件
dex      183 个类
测试     StatsTest 41 条 + CatalogTest 714 条，全过
```

**验证到什么程度**

| 项 | 状态 |
|---|---|
| `apksigner verify` | ✅ |
| `aapt2 dump badging` | ✅ 包名/标签/启动 Activity 正确 |
| 清单类存在于 dex | ✅ 构建时强制校验 |
| 权限最小化 | ✅ 仅 VIBRATE / POST_NOTIFICATIONS / READ_CALENDAR / WRITE_CALENDAR，**无 INTERNET** |
| 逻辑单元测试 | ✅ 755 条断言 |
| **模拟器运行 + 逐页 UI 验证** | ✅ API 30 / x86_64 / AEHD，见 `UI-VERIFICATION.md` 与 44 张截图 |
| API 35+ edge-to-edge / 预测性返回 | ❌ 只有 API 30 镜像 |
| 通知与计划提醒端到端 | ❌ 未验证 |
| 系统日历写入 | ⚠️ 权限流程通过；模拟器无日历账户，事件未落地 |

---

## 4. 架构

```
src/com/ironlog/train/
  data/     Exercise(muscle/family/primary/secondary), SetEntry, SessionExercise,
            WorkoutSession(startedAt/endedAt/pausedAt/pausedAccumMs/routineId),
            Routine(days 位掩码), RoutineExercise, BodyMetric,
            Catalog(95 动作), Templates(6 种分化模板),
            Store(单 JSON 持久化), Json(反射序列化)
  logic/    Stats(1RM/容量/PR/周统计/连续周), Fmt(日期数字格式)
  notify/   PlanReminders(每日 AlarmManager 提醒), ReminderReceiver,
            CalendarSync(CalendarContract 写入系统日历)
  ui/       MainActivity(3 页签 + 返回栈), 各 Screen,
            Ui(主题调色板/样式/输入框), Icons(矢量图标), Dialogs(统一圆角对话框), InsetsHelper
  ui/widget CalendarView(计划+完成双标记), BarChartView, LineChartView,
            MuscleMapView(正反面 16 部位)
res/drawable  26 个 VectorDrawable（18 个在用）+ dialog_bg_dark/light.xml
res/values    colors.xml / strings.xml / styles.xml（含 Theme.IronLog.Dialog(.Light)）
test/         StatsTest, CatalogTest（纯 JVM，无需 Android）
screenshots/  44 张模拟器实拍截图，逐页验证用
tools/        镜像+AVD+启动/截图脚本（见第 6 节）
```

**构建**：`powershell -ExecutionPolicy Bypass -File build.ps1`
不联网、不需要 Gradle，10 秒出包。**要在完全权限下跑**（见第 6 节）。

---

## 5. 行为契约（改代码前必读，`README.md` 同名章节）

**会话状态**

| 状态 | 判定 | 行为 |
|---|---|---|
| 空草稿 | `startedAt==0` 且无动作 | 返回/启动时丢弃 |
| 准备中 | `startedAt==0` 且有动作 | 不计时，等用户点开始 |
| 进行中 | `startedAt>0 && endedAt==0` | 自动保存 |
| 暂停中 | `pausedAt>0` | 计时冻结 |
| 已结束 | `endedAt>0` | 只读 |

**计时**：`now - startedAt - pausedAccumMs - (暂停中 ? now - pausedAt : 0)`

**完成判定**：`reps>0` 或 `weight>0` 或手动打勾；结束训练时自动补勾并剔除 0 组动作；一组都没有才提示放弃。

**计划**：`Routine.days` 位掩码 bit0=周一；日历实心点=当天有已结束训练，空心圈=当天有计划；计划完成的判定是当天存在同 `routineId`（或同名）的已结束训练。

**统计**：容量只算 NORMAL/DROP（WARMUP 排除）；e1RM = `w×(1+reps/30)`；连续周「本周为空不算断，连续两个空周才归零」。

---

## 6. 环境限制与模拟器（已跑通，照抄即可）

| 事实 | 说明 |
|---|---|
| PowerShell 的 HTTP 不通 | `Invoke-WebRequest` TLS 失败，但 **Node 的 fetch 正常**；下载脚本用 `node` |
| 沙箱下无法启动模拟器 | `-accel-check` 报 `Unable to open AEHD device: ERROR_ACCESS_DENIED`；x86_64 镜像强制要加速。**必须用完全权限（danger-full-access）启动** |
| 沙箱下 `~/.android` 不可写 | 模拟器会疯狂重试 `emu-last-feature-flags.protobuf.lock`。用 `ANDROID_EMULATOR_HOME=tools/emu-home` + `ANDROID_AVD_HOME=tools/avd` 重定向 |
| SDK 目录只读 | `%LOCALAPPDATA%\Android\Sdk\system-images` 写不进去，所以镜像放工作区，用 `-sysdir` 指过去（`ANDROID_SDK_ROOT` 对它无效） |
| 构建也要完全权限 | `build/classes.jar` 在沙箱模式下删不掉也写不了（DSH 文件沙箱拒绝，非 ACL 问题） |
| 镜像已就位 | `tools/sdk/system-images/android-30/default/x86_64/`（含 3.2 GB `system.img`）；原始 `tools/x86_64-30_r11.zip` 留有备份 |

**启动命令**（完全权限）：

```powershell
cd ironlog
$env:ANDROID_AVD_HOME="$PWD\tools\avd"
$env:ANDROID_EMULATOR_HOME="$PWD\tools\emu-home"
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd IronLog `
    -sysdir "$PWD\tools\sdk\system-images\android-30\default\x86_64" `
    -no-window -no-audio -no-boot-anim -no-snapshot -no-metrics -gpu swiftshader_indirect -port 5554
```

**截图与点击**：

```powershell
# 按文字点击（自动 dump uiautomator 找 bounds）、截图
powershell -ExecutionPolicy Bypass -File tools\ui.ps1 -Action tap -Target "开始训练"
powershell -ExecutionPolicy Bypass -File tools\ui.ps1 -Action shot -Name "48-xxx"
powershell -ExecutionPolicy Bypass -File tools\ui.ps1 -Action text     # 当前页所有可见文字
```

注意：`adb shell input tap` 用的是**设备像素**，截图预览常被缩放，按预览坐标点会偏；一律用 `tools/ui.ps1` 按文字点击。

**新会话怎么接**：镜像和 AVD 都在 `tools/` 里，直接按上面命令启动 → `adb install -r dist/IronLog-1.0.0.apk` → `am start -n com.ironlog.train/.ui.MainActivity` → `tools/ui.ps1` 走查。不要重新下载 645 MB。

---

## 7. 已知问题 / 待办

1. **数字输入框点击即全选**：改中间一位（60→65）要重打。若嫌烦，可改为「首次聚焦全选、再次点击定位光标」。
2. **身体数据表单**键盘弹出时需手动上滑才能看到「保存」（`adjustResize` 生效，只是没自动滚到按钮）。
3. **提醒时间滚筒**选中行沿用平台灰色，深色下偏暗。
4. **计划日历**里「今天」与「已完成」共用实心圆样式，重叠时含义不唯一。
5. **JSON 每次输入全量重写**：数据到几 MB 会卡，断电有写坏风险。三个方案待用户选：立即写（现状）/ 300ms 防抖 / 换 SQLite。
6. 首页每次回前台重建，滚动位置回到顶部。
7. 选择动作页单选模式看不到肌肉预览（点了就返回）。
8. 5 个图标未引用（`ic_sun/ic_moon/ic_settings/ic_body/ic_timer`），约 3KB。
9. 统计里「计划完成」是启发式（`routineId` 或同名）。
10. 手机重启后提醒不会自动恢复，需打开一次 App。
11. 许可证已换成 Apache-2.0 全文 + NOTICE（原 GPL 占位文件已删除）。
12. 只在 API 30 验证过；targetSdk 36 的 API 35+ 强制 edge-to-edge、预测性返回未验证。
13. 通知/闹钟未端到端验证；系统日历写入因模拟器无日历账户未能落地。

---

## 8. 已明确拒绝 / 已定的决策

- ❌ 破解、脱壳、改 smali、伪造支付状态：不做
- ❌ 借用训记的图标与图片：不做（全部自绘矢量）
- ❌ 重写为 React Native / Expo：不做，理由与结论见会话记录（包体、工具链、验证成本）
- ✅ 许可证 Apache-2.0（2026-11-14 由 GPL-3.0-or-later 改：APK 分发时 GPL 的源码随附义务太重，且需要专利授权与商标不授权）
- ✅ 计划以「分化」为核心，而非「每周几练」的日程表
- ✅ **署名包含 AI**：README「关于 AI 协作」、`NOTICE`、首次提交的 `Co-Authored-By` 都写上 DeepSeek Harness（deepseek-flash）。作者负责需求与验收，agent 负责实现与模拟器验证
- ✅ 计时由用户显式点「开始训练」触发
