# PurrCare - Cat Care & Management (Android, Java, XML, Firebase)

**Developer Name:** Jakia Ferdous Juthi  
**Student ID / Roll:** 20  
**Batch:** D-85  
**Department:** Computer Science and Engineering (CSE)  
**University:** Dhaka International University

PurrCare helps you look after multiple cats: profiles, health records, feeding schedules, reminders with notifications, weight chart, expenses (in Bangladeshi Taka), photo growth diary and emergency information.

> **Honest status:** this project was written without access to the Android SDK, so it has **not been compiled or run on a device yet**. The XML files were checked to be well-formed, every `R.id / R.layout / R.drawable` used in Java was cross-checked against the XML, and the Java files were syntax-checked. Your first Gradle sync/build is the real test. If Android Studio shows an error, copy the red message and ask for a fix. Do not put "tested" in your lab report for anything you have not tested yourself.

## Features
Login/Register/Logout (Firebase Auth) + Remember Me, cat profiles (photo, auto age), health tracker with upcoming due dates, food tracker (mark done), reminders + notifications, weight tracker + chart, expense tracker (monthly + category totals), growth diary (photo grid), emergency info (clinic website + call contacts), dashboard, bottom navigation, shake-to-quick-action (accelerometer).

## Technologies
Java, XML (ConstraintLayout, LinearLayout, CardView, Material Components), RecyclerView + Adapter + ViewHolder, Firebase Authentication, Firebase Realtime Database (real-time listeners), SharedPreferences, Camera/Gallery intents, SensorManager + SensorEventListener, AlarmManager + notifications, implicit intents (DIAL, VIEW, SEND).
minSdk 24 (Android 7.0), compileSdk/targetSdk 34, Android Gradle Plugin 8.5.2, Gradle 8.7.

## Project structure
```text
PurrCare/
  build.gradle, settings.gradle, gradle.properties
  gradle/wrapper/gradle-wrapper.properties
  firebase_rules/database.rules.json
  docs/LAB_REPORT_OUTLINE.md
  app/
    build.gradle, proguard-rules.pro
    google-services.json
    src/main/AndroidManifest.xml
    src/main/java/com/purrcare/app/
      activities/
      adapters/
      models/
      firebase/
      utils/
      sensors/
      notifications/
    src/main/res/
```

## 1. Firebase setup
1. Go to https://console.firebase.google.com and create a project named PurrCare.
2. Authentication → Sign-in method → Enable Email/Password.
3. Create Realtime Database in locked mode.
4. Paste the content of `firebase_rules/database.rules.json` into the Rules tab and Publish.
5. Add an Android app with package name `com.purrcare.app`.
6. Download `google-services.json` and copy it into the `app/` folder.

Photos are stored as small Base64 text inside the Realtime Database. No Firebase Storage is required for this lab project.

## 2. Open in Android Studio
1. Extract the ZIP.
2. Android Studio → File → Open → select the PurrCare folder.
3. Wait for Gradle sync.
4. Install SDK Platform 34 if requested.
5. If `google-services.json` is missing, add it to the app folder and click Sync Now.

## 3. Run
Use an emulator (Device Manager → Create Device, API 34) or a real Android phone with USB debugging enabled. Press the green Run button.

## 4. Test the required features
- Register and log in.
- Add, edit and delete cat profiles.
- Test Health, Food, Reminders, Weight, Expense, Diary and Emergency.
- Test reminder notifications.
- Add at least two weight records to see the chart.
- Test emergency clinic website and contact calling.
- Test the Share PurrCare feature.
- Test shake-to-quick-action.
- Test real-time updates on two devices.
- Test form validation with empty fields.

## 5. Build an APK
1. Build → Build Bundle(s) / APK(s) → Build APK(s).
2. Click `locate` when the build finishes.
3. Find the APK at `PurrCare/app/build/outputs/apk/debug/app-debug.apk`.
4. Copy it to your phone and install it.

For a signed release APK: Build → Generate Signed App Bundle / APK → APK → create a keystore → select release. Keep the keystore and passwords safe.

## Known limitations
- Reminder alarms are re-created when you open the app after a phone restart (no boot receiver).
- Without exact alarm permission (Android 12+), notifications may arrive a few minutes late.
- Photos are stored in the database as text, so many large photos may slow down the diary.