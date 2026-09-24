# 发布检查清单（应用内更新）

应用内「设置 → 检查更新」以 **GitHub Releases** 为源。每次发版按下列步骤执行：

## 1. 改版本号
编辑 `app/build.gradle.kts`：

```kotlin
versionCode = 2        // 每次递增
versionName = "1.0.1"  // 语义化版本
```

## 2. 本地构建并自测
```bat
gradlew.bat testDebugUnitTest     :: 38 个 JVM 单测应全部通过
gradlew.bat assembleDebug         :: 产出 app/build/outputs/apk/debug/app-debug.apk
```
> 发布资产用 debug APK 即可（签名可安装）；如已配置签名，也可用 `assembleRelease`。
> 发布前建议真机回归一遍 README 的「功能自测清单」。

## 3. 推送代码
```bat
git add -A
git commit -m "v1.0.1"
git push -u origin main
```

## 4. 创建 GitHub Release
1. 在 GitHub 仓库页 `Releases → Draft a new release`；
2. **Tag 命名为 `v` + versionName**（如 `v1.0.1`）——App 内按 tag 比较版本，格式不可省略；
3. Title 可写版本号；Description 写更新说明（App 内会展示）；
4. 把 `app-debug.apk` 作为 **asset** 上传（文件名以 `.apk` 结尾即可，App 会自动取第一个 `.apk` 资产）。

## 5. 手机端验证
1. 打开 App → 设置 → 检查更新：应提示「发现新版本 v1.0.1」+ 更新说明；
2. 点「下载并安装」→ 下载完成后点「安装」；
3. 首次安装新版本包时系统会要求为拾光打开「允许安装未知应用」，按提示开启；
4. 升级后数据保留（Room 数据库不随安装包更换而丢失）。

## 常见问题
- **检查更新一直提示「仓库暂无发布版本」**：说明还没发布过 Release，或 Tag 名不带 `v` 前缀。
- **提示「新版本缺少 APK 资产」**：Release 未上传 `.apk` 文件。
- **提示「网络不可用」**：手机无网络或无法访问 api.github.com。