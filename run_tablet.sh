adb connect 100.91.121.36:5555
./gradlew installDebug
adb shell am start -n com.bestcoder1877.weather/.MainActivity
