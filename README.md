
![1](./app/src/main/res/mipmap-xhdpi/ic_launcher.png)
# FingerprintPayXposed
让微信、支付宝在支持指纹识别的手机上使用指纹支付.

> ⚠️ **非官方分支**: 本项目是基于 [FingerprintPay](https://github.com/eritpchy/FingerprintPay) 的一个**独立分支**，代码由 **AI (DeepSeek V4 Flash) 辅助生成**，非原作者 eritpchy 的官方版本。使用风险自负，不保证与上游版本行为一致。

> ⚠️ **警告**: 使用本模块可能导致微信/支付宝风控、封号等问题，**概不负责**！

## 特点
- 使用 **LibXposed API 101**（仅 LSPosed）
- 移除 Magisk/Riru/Zygisk 模块等已过时组件
- 移除淘宝 QQ 云闪付等软件支持，仅支持微信支付宝
- 支持 **BiometricPrompt** 平台 API（Android 12+）
- 密码使用 **AES/GCM + AndroidKeyStore** 加密存储

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
| 微信   | 我 → 设置 → **长按任意设置项**弹出指纹支付设置 |


## 详细教程
- [支付宝](./doc/Alipay)
- [微信](./doc/WeChat)

## 当前版本 (v8.5.3)
- 🐛 修复微信指纹验证成功后密码偶尔无法解密（概率性失败）：加解密失败时自动重新认证重试。
- 🐛 修复微信指纹验证成功后不自动输入密码、取消指纹框后键盘不弹出、切换支付方式后再次验证不输密码等问题。
- 兼容新旧版微信支付键盘（`MyKeyboardWindow` 及 8.0.7x 新增的 `HkWxKeyboardWindow`/`MiniAppKeyboardWindow`）。
- 当前作用域仅包含微信和支付宝；仓库中保留的 QQ、淘宝及云闪付代码属于历史兼容实现，不代表当前支持承诺。
- 构建链已统一使用 Java 17 toolchain，并更新 Android Gradle Plugin、Kotlin、AndroidX、Compose、Gson、OkHttp 和 RemotePreferences 依赖。
- 应用关闭 Android Auto Backup，并通过 `dataExtractionRules` 排除云备份和设备迁移数据。
- `REQUEST_INSTALL_PACKAGES` 仅用于应用内检查更新后的 APK 安装。
- Release 使用 `wifikeyxposed.keystore` 签名。

## 更新内容 (v8.5.0)
### 🆕 微信指纹支付（初步确认可用）
- **进入方式**：在微信设置页面**长按任意设置项**即可弹出指纹支付设置对话框
- 支持 BiometricPrompt 平台 API 进行指纹认证
- 认证成功后自动模拟点击数字键盘输入密码
- 密码使用 AES/GCM + AndroidKeyStore 加密存储

### 📋 自 v8.0.1 以来的变更
#### 修复
- 修复支付宝 `KeyPermanentlyInvalidatedException`：指纹录入变更时密钥失效，自动删除并重新生成
- 修复支付宝 `UnrecoverableKeyException`：不可恢复密钥异常处理
- 修复设置对话框 `ClassCastException`：DecorView 强转 LinearLayout 异常
- 修复 `initFingerPrintLock` 中 Context 不是 Activity 的问题（影响指纹认证弹窗）

#### 新增
- 新增 PullDownListView.onItemLongClick Hook（长按设置入口）
- 新增 MyKeyboardWindow.setInputEditText Hook（支付键盘检测）
- 新增 AlertDialogImpl.showTipsImpl Hook（系统错误弹窗拦截）
- 新增指纹密钥失效时的用户友好提示（支持中英文）

#### 重构
- 移除旧版设置注入方式（ListView HeaderView / 右上角菜单）
- 移除未使用的旧版代码文件
- 更新 LibXposed API 到 101.0.1

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
