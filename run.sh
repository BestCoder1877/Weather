adb disconnect
adb connect 100.126.5.68:5555
./gradlew installDebug
adb shell am start -n com.bestcoder1877.weather/.MainActivity
