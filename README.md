
![1](./app/src/main/res/mipmap-xhdpi/ic_launcher.png)
# FingerprintPayXposed
让微信、支付宝在支持指纹识别的手机上使用指纹支付.

> ⚠️ **非官方分支**: 本项目是基于 [FingerprintPay](https://github.com/eritpchy/FingerprintPay) 的一个**独立分支**，代码由 **AI (DeepSeek V4 Flash) 辅助生成**，非原作者 eritpchy 的官方版本。使用风险自负，不保证与上游版本行为一致。

## 特点
- 使用 **LibXposed API 101**（仅 LSPosed）
- 移除 Magisk/Riru/Zygisk 模块等已过时组件
- 最低 Android 12+（API 31+）

## 最低要求
- 有指纹硬件
- **Android 12+**（API 31+）
- **[LSPosed](https://github.com/LSPosed/LSPosed)** 框架（或兼容 LibXposed API 101 的其他框架）

> ⚠️ 已移除：Magisk / Riru / Zygisk / Xposed（旧版）支持

## 实现原理
1. 利用 [LSPosed](https://github.com/LSPosed/LSPosed) 加载指纹支付模块
2. 在模块设置中录入应用的「支付密码」
3. 使用 **Android KeyStore + BiometricPrompt**（TEE）加密保存支付密码
4. 支付界面弹出时，验证指纹后自动解密并填充密码
5. 通过模拟点击数字键盘完成输入

## 使用步骤
1. 安装 [LSPosed](https://github.com/LSPosed/LSPosed) 框架
2. 从 [Releases](https://github.com/TYOPXN360/FingerprintPayXposed/releases) 下载并安装最新 APK
3. 在 LSPosed Manager 中启用模块，作用域勾选目标应用
4. 重启目标应用，进入设置 → 开启指纹支付并录入支付密码

## 设置入口
| 软件 | 路径 |
| ------ | -------------------------------- |
| 支付宝 | 我的 → 设置 → 支付设置 → 指纹设置 |
| 微信   | 我 → 设置 → 指纹设置 |

> 淘宝、QQ、云闪付未经测试，理论上兼容，可自行尝试

## 详细教程
- [支付宝](./doc/Alipay)
- [微信](./doc/WeChat)

## 更新内容 (v8.0.0)
### 🚀 重大变更
- **移除第三方指纹库** `FingerprintIdentify`，改用 Android 平台 `BiometricPrompt`
- **移除 Magisk/Riru/Zygisk 模块支持**，仅支持 LSPosed（LibXposed API 101）
- **最低 API 提升至 31**（Android 12+）
- 默认 `useBiometricApi` 为 true，移除设置页中相关开关选项

### 🔧 Bug修复
- 修复数字键盘按键映射错位问题
- 修复极速付款模式无键盘时无法触发指纹
- 移除半屏"付款给xx"页面提前弹出指纹弹窗的误触发
- 修复确认按钮检测逻辑

### ⚡ 性能
- APK 体积从 40MB 缩减至 **3.0MB**（R8 全混淆 + 资源压缩）
- 移除日志中的密码泄露风险

### ⚠️ 注意
- **≤v7.5.1 版本加密存储的密码不兼容**，需在 v8.0.0 中重新录入

> ⚠️ **警告**: 本版本代码由 AI (DeepSeek V4 Flash) 辅助生成，非原作者官方版本。

## 常见问题
1. **插件已安装但应用内看不见菜单？** \
   检查 LSPosed 作用域是否正确勾选，尝试重启应用或重启手机
2. **提示"系统繁忙"或"密码错误"？** \
   v8.0.0 已修复数字键盘映射问题，请升级并重新录入支付密码
3. **支付宝弹出刷脸验证？** \
   这是支付宝自身风控机制，与本模块无关

## 致谢
- [FingerprintPay (eritpchy)](https://github.com/eritpchy/FingerprintPay)
- [LSPosed](https://github.com/LSPosed/LSPosed)
- [WechatFp](https://github.com/dss16694/WechatFp)

## 提示
1. 本软件的网络功能仅限检查自身更新，欢迎 Review 代码
2. 支付宝版本升级后可能不兼容，请关注 Release 更新
3. QQ交流群: [665167891](https://h5.qun.qq.com/h5/qun-share-page/?_wv=1027&k=fCZf_WEKL1Rj_N0gi9JgkH7bfnKj11Wy&authKey=acNcoIs325Uco7v2JZY4NObRFA3sJU%2FWI1%2FH64DkP50cn6HBRUzBZ9cvZGNqmzGi&market_channel_source=665167891_1&noverify=0&group_code=665167891)

<img src="./doc/qq_group.jpg" alt="QQ交流群: 665167891" width="500">
