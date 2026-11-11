<p align="center"><a href="README.md">English</a> · <strong>简体中文</strong></p>

# hypermusiccover-duo

面向小米折叠屏与 MiDuo 的 HyperOS 锁屏音乐、歌词和竖排通知胶囊，由
[pku-zhb](https://github.com/pku-zhb) 维护。

基于 [HyperMusicCover-Enhanced](https://github.com/Lewewe/HyperMusicCover-Enhanced)
与原始 [HyperMusicCover](https://github.com/zyl6932/HyperMusicCover)。本项目聚焦自用设备适配。

## 当前功能

- 方形封面、同步／逐词歌词与歌词翻译。
- MiDuo 右侧快捷按钮旁的竖排胶囊，按通知、实时卡片、音乐排序；上下滑切换，点击圆泡直接展开对应卡片。
- 时钟／内容区域及右侧控件旁的空隙上滑，依次展开播放器、实时卡片、通知；已有内容展开完后才交给系统解锁，快捷按钮与胶囊仍处理各自手势。
- 多选锁屏时收进胶囊的类别，或保持上次锁屏状态；内屏横屏保留独立类别设置和封面／歌词双栏布局。
- 原生播放器、时钟、通知重排与 AOD／亮屏状态衔接。
- 原生柔光玻璃，或跟随原生播放器的最终材质，包括 HyperLight 调整。
- 设置导出／导入与设备诊断计数。
- 独立的“锁屏显示全部通知”和“锁屏保留通知”；在 Duo 开启时，请关闭 HyperLight 对应选项。

全屏封面与背景切换、频谱和扩展页面仍待兼容性验证。自动更新暂未启用，将使用
本仓库的发布渠道。已确认行为和未解决问题见[适配进度](docs/fold-settings-roadmap.md)。
编译与单元测试通过不代表真机验收通过。

## 安装与迁移

要求：Root 的 HyperOS 4、Android 15+，以及支持现代 Xposed API 102 的 LSPosed。
竖排胶囊需要 MiDuo 的右侧快捷按钮布局。

应用包名和代码命名空间统一为 `com.zhuhuibin.hypermusiccover.duo`，名称为
`hypermusiccover-duo`。它与 Enhanced、旧 Fold 预览版
`com.zhuhuibin.hypermusiccover.fold` 分别安装，Android 不会自动转移应用数据。

1. 在旧版中导出设置，再处理旧应用。
2. 从 [Duo Releases](https://github.com/pku-zhb/hypermusiccover-duo/releases)
   获取已发布安装包，或自行构建。
3. 在 LSPosed 中停用旧音乐模块，启用 Duo 并勾选推荐作用域，两者不要同时启用。
4. 自行重启作用域后导入设置。已移除的选项不会被旧备份重新开启。

仓库建立不代表已有稳定版。预览版需要真机验收，不同签名的 APK 不能互相覆盖更新。
更新不再自动重启作用域，由使用者决定何时重启并加载新模块代码。

## 构建

```sh
git clone --recurse-submodules https://github.com/pku-zhb/hypermusiccover-duo.git
cd hypermusiccover-duo
tools/build.sh :app:testDebugUnitTest :app:assembleDebug
```

需要 JDK 21+、Android SDK 37 和 build-tools 37.0.0。本机脚本复用已安装工具链，
构建、测试报告和临时文件放在 `/Volumes/Harbor/DevCache/hypermusiccover-duo/`。
可用 `JAVA_HOME`、`ANDROID_HOME`、`GRADLE_USER_HOME` 指定其他工具链。
发布签名配置见英文 README；没有发布密钥时产物只使用调试签名。

保留的 `hyper-canvas` 是上游可选依赖，其提供方目前只信任 Enhanced 包名；
需要连同主程序身份与签名契约一起适配，不能仅靠重新编译恢复使用。

## 维护、反馈与许可

每完成并验证一个阶段就提交、推送。问题请提交到
[Duo Issues](https://github.com/pku-zhb/hypermusiccover-duo/issues)，附设备、系统／模块版本、
内外屏状态和复现步骤。

可以后台准备预览包，但每次安装前都要重新征得使用者确认。手机界面操作和重启由使用者完成。

感谢 Lewewe、zyl6932、puhboo、juren233、btm_m / HyperChanger 和应用致谢页列出的贡献者。
遵循 [AGPL-3.0](LICENSE)，保留 [NOTICE](NOTICE) 与依赖许可。本项目不沿用上游捐赠渠道。
