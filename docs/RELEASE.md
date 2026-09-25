# 发布检查清单（应用内更新）

应用内「设置 → 检查更新」为**双源**：**Gitee（码云）默认优先**（国内直连稳定），GitHub 备用；
检查更新或下载 APK 时默认源失败，自动回退到备用源。
两个源各自发布同名 Release（结构一致：tag 带 `v` 前缀 + `.apk` 资产）。每次发版按下述步骤执行。

## 1. 改版本号
编辑 `app/build.gradle.kts`：

```kotlin
versionCode = 3        // 每次递增
versionName = "1.0.2"  // 语义化版本（tag 必须为 v1.0.2）
```

## 2. 本地构建并自测
```bat
gradlew.bat testDebugUnitTest     :: 60 个 JVM 单测应全部通过
gradlew.bat assembleDebug         :: 产出 app/build/outputs/apk/debug/app-debug.apk
cp app/build/outputs/apk/debug/app-debug.apk dist\studymate-v1.0.2.apk
```
> 发布资产用 debug APK 即可（签名可安装）；如已配置签名，也可用 `assembleRelease`。
> 发布前建议真机回归一遍 README 的「功能自测清单」。

## 3. 推送代码（GitHub）
```bat
git add -A
git commit -m "v1.0.2"
git push -u origin main
```

## 4. 同步镜像到 Gitee（一次性 + 每次发版）

**方式 A：本地双远端（推荐，可随时同步）**
1. 在 [gitee.com](https://gitee.com) 注册/登录，新建**空仓库**，名称建议与 GitHub 同名 `studymate-android`；
2. 本地仓库添加远端并推送：
```bat
git remote add gitee https://gitee.com/<你的Gitee用户名>/studymate-android.git
git push gitee main
```
3. 以后每次发版：`git push origin main` 后再 `git push gitee main`。

**方式 B：Gitee 网页导入（一次性镜像快照，不自动同步）**
Gitee → 右上角 `＋ → 从 GitHub/GitLab 导入仓库` → 粘贴 `https://github.com/lmhy006/studymate-android.git` → 导入。
（后续若走此方式，代码同步需手动再导入或改用方式 A。）

> 代码里备用源仓库名：`AppConfig.GITEE_REPO`（默认 `lmhy006/studymate-android`）。如果你的 Gitee 用户名不同，把这里改成 `你的Gitee用户名/studymate-android` 并重新构建。

## 5. 创建 Release（GitHub + Gitee 各一份）

**GitHub（命令行，需先 `gh auth login`）**
```powershell
gh release create v1.0.2 "D:\dsh_ws\11\dist\studymate-v1.0.2.apk" --repo lmhy006/studymate-android --title "拾光 StudyMate v1.0.2" --notes "更新说明（App 内会展示）"
```

**Gitee（网页操作）**
1. 打开 Gitee 仓库页 → `发行版/Releases → 新建发行版`；
2. **Tag 填 `v1.0.2`**（必须与 GitHub 一致，带 `v` 前缀）；
3. 标题与说明照抄 GitHub 那份；
4. 附件上传 `studymate-v1.0.2.apk`（文件名以 `.apk` 结尾）。

> Gitee 的 Release 与 GitHub 相互独立，需在两边各建一次；App 内任一源可用即能更新。

## 6. 手机端验证
1. 打开 App → 设置 → 检查更新：应提示「发现新版本 v1.0.2」+ 更新说明；
2. 点「下载并安装」→ 下载完成后点「安装」；
3. 首次安装新版本包时系统会要求为拾光打开「允许安装未知应用」，按提示开启；
4. 升级后数据保留（Room 数据库不随安装包更换而丢失）；
5. 如想验证 Gitee 源：在手机上断掉直连 GitHub 的网络（或用仅能访问国内站点的网络）后再「检查更新」，应自动走 Gitee。

## 常见问题
- **检查更新一直提示「仓库暂无发布版本」**：两个源都没 Release，或 Tag 名不带 `v` 前缀。
- **提示「新版本缺少 APK 资产」**：两个源都没上传 `.apk` 资产。
- **提示「网络不可用」**：手机无网络，或 GitHub 与 Gitee 均不可达。
- **双源仍失败但家里有代理**：代理可用时 GitHub 源即可正常工作（双源只是增加国内直连的成功率）。