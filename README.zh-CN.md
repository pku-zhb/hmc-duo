<p align="center"><a href="README.md">English</a> · <strong>简体中文</strong></p>

# hmc-duo

面向小米折叠屏的 HyperOS 锁屏音乐、歌词和独立竖排通知胶囊，由
[pku-zhb](https://github.com/pku-zhb) 维护。

基于 [HyperMusicCover-Enhanced](https://github.com/Lewewe/HyperMusicCover-Enhanced)
与原始 [HyperMusicCover](https://github.com/zyl6932/HyperMusicCover)。适配聚焦维护者的折叠屏设备，
尚未建立其他机型的兼容性保证。

0.2.9 在折叠导致系统播放器卡片替换时同步交接按钮绑定，不再等待新媒体事件或延迟重试。
通知压住前景时保留原生背景，明确重开页面后才恢复封面背景。
同时包含 0.2.8 对页面、背景和时钟提交及恢复责任的统一，去掉普通封面／歌词切换时不必要的延迟重排。
内屏保留系统卡片宽度，普通页面回到安全窗口中轴，仅封面＋歌词分栏。
播放器改为 75%／100% 两档，AOD 按可见两行收掉进度条留白；发布混淆后胶囊和快捷底板也能
跟随最终材质。见[本轮记录](docs/releases/0.2.9.md)，主机检查与设备结果分开记录。

0.2.5 是此前候选，包含 0.2.4 的全部修复（含高德残留状态阻断“保持上次锁屏”的修复），
并加入维护者提供的微信赞赏码，可查看或保存原图到相册。既有 0.2.4 归档保持不变。
设置按用途分组，补齐必要的前置条件说明，保留原有配置键。通知左右滑动的柔和边缘
改为逐张卡片处理，并覆盖播放器、实时卡片与半遮挡通知；内屏竖屏支持相同的时钟位置选项，
音乐玻璃也可从当前封面背景取色。构建通过与实际视觉验收分开记录，见[本轮记录](docs/releases/0.2.5.md)。

## 功能

- 方形／全屏封面、同步／逐词歌词、歌词效果及可选翻译。前景封面与背景独立设置：
  可选原生壁纸、封面背景或动态背景，并调节模糊和暗度。
- Duo 独立管理卡片宽度与右侧手电筒／相机布局。放大的触摸区域与动画使用统一坐标，
  启动及取消保留系统安全流程，无需 MiDuo 或 MiDuo Notification。
- 只显示的电量／网络控件与独立的锁屏状态栏开关。外屏与内屏封面／歌词页面的时钟可选缩小、
  收到日期行、在右上状态图标显示；日期行可选时间在左侧或右侧。
  普通 AOD 保留系统呈现，全屏 AOD 可保留选中的小时间。
- HyperOS、ColorOS 两种竖排胶囊。点击胶囊或圆泡展开卡片，上下滑切换；HyperOS 到首尾停止，
  胶囊弹性跟手，气泡融合分裂保留原生材质。超过三个项目自动缩短胶囊，圆形仍放不下时从顶部隐藏，
  隐藏项目仍可逐项展开。
- 空白内容区上滑依次展开播放器、实时卡片、通知，下滑收回；全收起时左侧整片区域都可操作。
  实际卡片区域保留系统滚动、左右清除、点击、长按与播放器按钮／进度条操作，全部展开后上滑解锁。
- 播放器原有封面位置轮换封面、歌词与返回时钟动作，不增加按钮，也不挤动标题。
  内屏横屏额外提供封面与歌词同时展示的第四种状态。
- 多选锁屏时收进胶囊的类别，或保持上次锁屏状态；内屏横屏保留独立类别设置与封面／歌词双栏布局。
- 原生玻璃材质、独立的锁屏通知显示与保留选项；使用 Duo 的通知选项时关闭 HyperLight 对应功能。
- 设置导出／导入、通知播放器定制，以及 Duo 自己的发布／更新渠道；翻译凭据不会写入设置备份。

新增行为、内屏布局和 AOD 切换仍需真机验收，编译成功不等于设备验证通过。
已确认结果及本轮验收范围见[当前进度](docs/fold-settings-roadmap.md)。

## 安装与迁移

要求：Root 的 HyperOS 4、Android 15+，以及支持现代 Xposed API 102 的 LSPosed。
应用包名与代码命名空间均为 `com.zhuhuibin.hypermusiccover.duo`。

1. 在旧应用中导出设置，再处理旧应用。
2. 从 [Duo Releases](https://github.com/pku-zhb/hmc-duo/releases) 获取已发布的安装包，或自行构建。
3. 在 LSPosed 启用 Duo 并勾选推荐作用域；停用旧音乐模块，以及 MiDuo／MiDuo Notification 的 SystemUI 挂载。
4. 准备好后自行重启相关作用域，再导入设置。

Duo 与 Enhanced、旧 Fold 包分别安装，Android 不会自动转移应用数据。
旧备份中的已移除选项会被丢弃，原先没有生效的背景配置迁移为原生壁纸；
只有应用外观设置的备份不会改变模块背景。

更新不会自动重启作用域。不同签名的 APK 无法互相覆盖，现有预览包沿用当前签名，
正式发布密钥需要单独配置，不能把 release 构建视为已经完成正式签名。
参见[发布流程](docs/releasing.md)。

## 构建

版本号采用 `X.Y.Z`，版本码为
`(major * 10000 + minor * 100 + patch) * 1000`；`0.2.2` 对应 `202000`。
发布安装包使用 `hmc-duo-X.Y.Z.apk`。

需要 JDK 21+、Android SDK 37 和 build-tools 37.0.0。

```sh
git clone https://github.com/pku-zhb/hmc-duo.git
cd hmc-duo
```

维护者已配置的 Mac 环境使用：

```sh
tools/build.sh :app:testDebugUnitTest :app:assembleDebug
```

其他环境请设置 `JAVA_HOME`、`ANDROID_HOME`，并指定源码目录之外的绝对构建路径：

```sh
export HMC_BUILD_ROOT=/absolute/path/to/duo-build
./gradlew -I tools/gradle-init.gradle :app:testDebugUnitTest :app:assembleDebug
```

维护脚本把构建输出与临时文件放在指定外置构建卷，未挂载时停止执行。
签名材料不得提交到仓库或同步盘，正式构建要求见发布流程。

## 反馈与许可

问题请提交到 [Duo Issues](https://github.com/pku-zhb/hmc-duo/issues)，
附设备、系统／模块版本、内外屏方向和复现步骤。每个版本只列本轮变更的验收项目。

感谢 Lewewe、zyl6932、puhboo、juren233、btm_m / HyperChanger 和应用致谢页列出的贡献者。
遵循 [AGPL-3.0](LICENSE)，保留 [NOTICE](NOTICE) 与依赖许可。
“关于 → 支持 Duo”可查看维护者提供的微信赞赏码，并将原图保存到相册；不展示其他收款渠道。
