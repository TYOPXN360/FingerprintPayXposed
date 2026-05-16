adb logcat -v time -d | Select-String "FingerprintPay\]|Biometric\]" | Select-Object -Last 40
