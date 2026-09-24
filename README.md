# 拾光 StudyMate

一个 **Android 学习辅助 APP**：倒数日 + 习惯打卡 + 日历日程 + 设置（主题/关于/检查更新），核心功能**完全离线**、数据保存在本机（Room 数据库），采用极简 Material3 风格，并支持**桌面小组件**（iQOO 12 Pro 等 Android 14 设备可直接添加）。

---

## 功能一览

| 需求 | 实现 | 说明 |
| --- | --- | --- |
| 1. 倒数日 | ✅ | 设定目标日期，显示“还有 N 天 / 就是今天 / 已过 N 天”；支持备注 |
| 2. 习惯打卡 | ✅ | 每日 / 每周 / 自定义每 N 天；**双模式（每个习惯自选）**：周期模式（固定周期日，每周期 1 次）/ 自由模式（随时打卡，打卡起算 N 天、超期缺卡、打卡重置）；**月历打卡视图**、**连续打卡统计**（当前/最长/累计）、**断签/缺卡提示**（APP 内横幅 + 列表红字） |
| 3. 日历日程 | ✅ | **周视图（BIT101 式节次课表）**：13 节（08:00 起），节次分界线/边框/今日高亮可开关；月视图（月历 + 当日议程）；单次日程 + **批量周期日程**（日期范围 × 星期多选 × 时间段）；**导入 BIT101 课表 JSON** 一键转周期日程 |
| 4. 本地持久化 | ✅ | Room（SQLite），关闭 APP 数据不丢失；核心功能无需联网 |
| 4(UI). 极简风格 | ✅ | 参照 BIT101 设计：白/浅灰底、细描边卡片、克制配色、Material3；底部四 Tab（日程/倒数/打卡/设置）；主题可切换 |
| 5. 桌面小组件 | ✅ | 倒数日小组件（2×2 / 4×1），桌面直接显示倒计时天数；每日自动刷新 + 修改后即时刷新 |
| 6. 设置与更新 | ✅ | 亮/暗/跟随系统主题切换；**日程设置**（周六/周日/今日高亮/分界线/边框开关 + 导入 BIT101 课表）；关于（版本信息）；**应用内检查更新**：读取 GitHub 最新 Release，有新版时下载 APK 并调用系统安装器更新 |

> 说明：按你的选择，**不申请任何系统通知权限**；断签/待打卡提醒均在 APP 内展示。后续若需要系统通知提醒，可在“设置”中开启 `POST_NOTIFICATIONS` 后再接入。
>
> “每周”与“每 N 天”按**周期锚点起算**：创建日起第 7 天/第 N 天为一个周期日，每周期最多打卡 1 次（如需“每周固定星期几”，可在后续版本加选）。

## 功能自测清单（装到手机后逐项验证）

**倒数日**
- [ ] 右下角 `+` 添加倒数日（标题/日期/备注），列表显示“还有 N 天”
- [ ] 目标日为今天显示“就是今天”；过去的显示“已过 N 天”
- [ ] 点卡片可编辑/删除；“置顶桌面”后卡片出现“桌面展示中”标
- [ ] 桌面添加「拾光倒数日」小组件（2×2 / 4×1），数字与 App 一致，改日期后小组件自动更新

**打卡**
- [ ] 新建习惯：可选【周期模式】（固定周期日，每周期最多 1 次）或【自由模式】（随时打卡，打卡后 N 天内再打即连续，超期记缺卡，下次打卡重新起算）
- [ ] 周期模式：到期日出现“打卡”；自由模式：任意一天都可“打卡”，列表显示“截止 X月X日”
- [ ] 两种模式的月历视图：已打卡实心点、断签/缺卡红点、今天圆圈
- [ ] 展开卡片看到“当前连续 X 次 / 最长 X 次 / 累计 X 次”；漏打后出现“已断签/已缺卡 N 次”红字与顶部横幅

**日程**
- [ ] 周视图（课表式）：新建“单次日程”后按时间段显示色块；点空白时段可快速新建
- [ ] 新建“批量周期日程”（日期范围 + 星期多选 + 时间段），周/月视图自动展开每一天
- [ ] 月视图：有日程的日期显示数量角标；点日期看当日议程，可删除

**通用**
- [ ] 杀掉 App 进程后再打开，全部数据仍在（Room 本地持久化）
- [ ] 设置页：主题可切换 跟随系统 / 亮色 / 暗色，切完立即生效并记住
- [ ] 设置 → 检查更新：无新版本提示“已是最新”；有新版时下载并调用系统安装器
- [ ] 日程编辑校验错误直接在弹层内展示，最新一条覆盖上一条，不会被弹层遮挡

---

## 提交应用更新（发布新版本）

应用内更新以 **GitHub Release** 为源（当前仓库 `lmhy006/studymate-android`）。完整检查清单见 **【docs/RELEASE.md】**，速览：

1. 修改 `app/build.gradle.kts` 的 `versionName`（如 `1.0.1`），构建出新的 debug/release APK；
2. 推送代码后，在 GitHub 创建 Release，**tag 命名为 `v` + 版本号**（如 `v1.0.1`）；
3. 把 APK 作为该 Release 的 **asset** 上传（文件名以 `.apk` 结尾即可）；
4. 手机端在 App 内「设置 → 检查更新」即可检测到新版本并下载安装。

> 首次安装新版本更新包时，系统会提示“允许安装未知应用”，需要为拾光打开该开关；升级后无需卸载，数据保留。
>
> 第一次发布也可以直接用 `gradlew.bat assembleDebug` 产出的 `app/build/outputs/apk/debug/app-debug.apk` 作为 Release 资产；在仓库发布任何版本之前，App 内“检查更新”会提示“仓库暂无发布版本”（与网络错误区分开）。

## 导入 BIT101 课表（JSON）

设置 → 日程设置 → 导入课表（JSON）。格式与 BIT101 课程对象同构：

```json
{
  "courses": [
    {
      "name": "线性代数",
      "teacher": "张老师",
      "classroom": "良乡1-101",
      "weekday": 1,
      "start_section": 1,
      "end_section": 2,
      "weeks": [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]
    }
  ]
}
```

- `weekday`：1=周一 … 7=周日；`start_section/end_section`：1..13 节（时间按默认节次表 08:00–20:55 换算）；
- `weeks`：教学周数组，导入时取首尾周作为日期范围（非连续周次按首尾覆盖，见“已知限制”）；
- 导入需选择“学期开始日期”（默认今天），每条课程将生成一条周期日程，备注带教师与周次；
- 未填 `weeks` 时按 16 周处理；颜色按导入顺序自动分配。

> BIT101 目前没有导出功能，因此首次使用需要你手动把课表整理为上述 JSON 一次，之后可复用同一份文件（也便于后续批量修改重新导入）。

---

## 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Kotlin | 2.1.20 | 语言 |
| Jetpack Compose (BOM) | 2025.04.01 | UI（Material3） |
| AGP / Gradle | 8.9.2 / 8.11.1 | 构建 |
| Room | 2.7.1 (KSP) | 本地数据库 |
| Glance | 1.1.1 | 桌面小组件 |
| WorkManager | 2.10.1 | 小组件后台刷新 |
| compileSdk / minSdk / targetSdk | 35 / 26 / 35 | 兼容 Android 8.0+，适配 Android 14（iQOO 12 Pro） |

## 项目结构

```
StudyMate/
├─ settings.gradle.kts / build.gradle.kts / gradle.properties
├─ gradle/wrapper/            # Gradle Wrapper 配置（见下方“首次构建”）
├─ app/
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ main/
│     │  ├─ AndroidManifest.xml
│     │  ├─ res/              # 图标、主题、小组件配置(countdown_widget_info.xml)
│     │  └─ java/com/shiguang/app/
│     │     ├─ StudyMateApp.kt / MainActivity.kt
│     │     ├─ core/          # 纯 Kotlin 逻辑（可单测）：DateUtils / Recurrence / Streak / Countdown
│     │     ├─ data/          # Room：entities / daos / AppDatabase / repositories / AppContainer
│     │     ├─ ui/            # Compose：theme / components / countdown / habit / schedule
│     │     └─ widget/        # Glance 小组件 + 刷新 Worker + 系统事件接收器
│     └─ test/                # 核心逻辑单元测试
└─ README.md
```

---

## 首次构建（在你的电脑上运行）

**环境要求**：Android Studio（2024.2+，自带 JDK）、首次同步需联网以下载依赖。

> ✅ **已在本机验证**：`assembleDebug` 构建通过（APK 约 12.2 MB），**33 个 JVM 单元测试全部通过**，`lintDebug` 0 错误；`gradle/wrapper/gradle-wrapper.jar` 已随仓库提供，首次打开无需再处理 wrapper。

1. 用 Android Studio 打开本项目根目录（`File → Open` 选择本项目文件夹）。
2. 等待 Gradle 同步完成（会自动下载 Gradle 8.11.1 与依赖，首次较慢）。
3. 若提示 `Android SDK 未找到`：`Settings → Languages & Frameworks → Android SDK` 安装 **SDK Platform 35** 与 **Build-Tools 35**（Android Studio 会引导）。
4. 点击 ▶ Run 选择模拟器或真机（真机需开启 USB 调试）。

命令行构建（可选，需 JDK 17+ 与 `local.properties`）：

```
gradlew.bat assembleDebug
gradlew.bat test            # 运行单元测试
```

---

## iQOO 12 Pro 添加倒数日小组件

1. 先安装并打开 App 一次，添加一个倒数日（可选：在倒数日卡片里点“置顶桌面”，小组件将显示该事项；否则显示目标日最近的）。
2. 回到桌面：**长按桌面空白处** → **添加工具/小部件** → 找到「拾光倒数日」→ 拖到桌面。
3. 拖拽边缘可切换 2×2 或 4×1 尺寸；点按小组件可直接打开 App。

兼容性说明：小组件基于标准 AppWidget/Glance 实现。Glance 对 **vivo（含 iQOO）系统 `goAsync` 兼容问题自动切换到 WorkManager 通道**更新，配合 App 内每日刷新 Worker 与系统时间变化刷新，跨天倒计时会自动更新，无需打开 App。

---

## 数据与隐私

- 全部数据保存在 **Room（SQLite）本地数据库**，**不上传任何数据**。
- 网络权限（INTERNET）**仅用于“检查更新”**：用户主动点击时才访问 GitHub API 并下载 APK；其余功能完全离线，离线状态不降级。
- `REQUEST_INSTALL_PACKAGES` 仅用于安装下载的更新包。
- 删除应用会清除数据；卸载前如需保留，可自行备份 `/data/data/com.shiguang.app/databases/studymate.db`。
- 数据库 schema 导出在 `app/schemas/`，后续升级版本可做迁移。

---

## 设计说明

- 极简 Material3：白/浅灰背景 + 描边卡片 + 单一品牌色（靛蓝 `#4460F0`），信息层级靠字重与留白，不用渐变与重阴影；自动跟随系统深色模式。
- 交互参考 BIT101-Android：底部弹层（BottomSheet）编辑、周课表配色块日程、月历角标计数。
- 底部四 Tab：**日程 / 倒数 / 打卡 / 设置**。

## 已知限制与后续路线

- 周视图日程重叠时采用叠加显示，暂无冲突避让算法；
- 导入课表的周次按首尾周处理：`weeks:[1,16]` 之外的中间周也会显示（如需精确周次过滤，后续版本加）；
- 习惯日历默认展示当前月，卡片内可翻月；
- 未做系统通知（按你的选择）；需要时可在「设置」加开关。
- 后续可加：课程 API 拉取（BIT101 风格）、数据导出/导入、更多小组件形态（打卡进度、今日日程）、农历/生日模式倒数。

---

## 单元测试

核心逻辑（日期、周期展开、连续打卡统计、倒数计算、日程物化、版本比较）均为纯 Kotlin，已附带 JUnit 测试：

```
app/src/test/java/com/shiguang/app/
├─ core/
│  ├─ DateUtilsTest.kt        # 7 例
│  ├─ RecurrenceTest.kt       # 7 例
│  ├─ StreakTest.kt           # 6 例
│  └─ CountdownTest.kt        # 4 例
├─ ui/schedule/
│  └─ ScheduleModelsTest.kt   # 6 例（单次/批量周期日程物化）
└─ update/
   └─ AppVersionTest.kt       # 3 例（版本号比较 / 更新判定）
```

合计 **33 个 JVM 用例全部通过**；`lintDebug` 静态检查 **0 错误 0 警告**。
另外 Room 数据层集成测试位于 `app/src/androidTest`（3 例：倒数日置顶唯一、打卡级联删除、日程周期规则），在真机/模拟器上运行（Android Studio 对测试类右键 → Run，需连接设备）。