# PurrCare - Cat Care & Management (Android, Java, XML, Firebase)

PurrCare helps you look after multiple cats: profiles, health records, feeding schedules, reminders with notifications,
weight chart, expenses (in Bangladeshi Taka), photo growth diary and emergency information.

> **Honest status:** this project was written without access to the Android SDK, so it has **not been compiled or run on a
> device yet**. The XML files were checked to be well-formed, every `R.id / R.layout / R.drawable` used in Java was
> cross-checked against the XML, and the Java files were syntax-checked. Your first Gradle sync/build is the real test.
> If Android Studio shows an error, copy the red message and ask for a fix. Do not put "tested" in your lab report
> for anything you have not tested yourself.

## Features
Login/Register/Logout (Firebase Auth) + Remember Me, cat profiles (photo, auto age), health tracker with upcoming due dates,
food tracker (mark done), reminders + notifications, weight tracker + chart, expense tracker (monthly + category totals),
growth diary (photo grid), emergency info (clinic website + call contacts), dashboard, bottom navigation,
shake-to-quick-action (accelerometer).

## Technologies
Java, XML (ConstraintLayout, LinearLayout, CardView, Material Components), RecyclerView + Adapter + ViewHolder,
Firebase Authentication, Firebase Realtime Database (real-time listeners), SharedPreferences, Camera/Gallery intents,
SensorManager + SensorEventListener, AlarmManager + notifications, implicit intents (DIAL, VIEW, SEND).
minSdk 24 (Android 7.0), compileSdk/targetSdk 34, Android Gradle Plugin 8.5.2, Gradle 8.7.

## Project structure
```
PurrCare/
  build.gradle, settings.gradle, gradle.properties
  gradle/wrapper/gradle-wrapper.properties
  firebase_rules/database.rules.json      <- paste into Firebase console
  docs/LAB_REPORT_OUTLINE.md
  app/
    build.gradle, proguard-rules.pro
    google-services.json                  <- YOU add this (not included)
    src/main/AndroidManifest.xml
    src/main/java/com/purrcare/app/
      PurrCareApp.java
      activities/     Splash, Login, Register, Home, Cats, AddEditCat, Health, Reminders, More,
                      Food, Weight, Expense, Diary, Emergency, BaseActivity
      adapters/       CatAdapter, RecordAdapter (generic), DiaryAdapter
      models/         Cat, HealthRecord, FoodSchedule, Reminder, WeightRecord, Expense, DiaryEntry, EmergencyContact
      firebase/       FirebaseHelper (all database code)
      utils/          Constants, DateTimeUtils, UiUtils, ImageUtils, ImagePickerHelper, PrefsManager, WeightChartView, SimpleTextWatcher
      sensors/        ShakeDetector
      notifications/  NotificationHelper, ReminderReceiver, ReminderScheduler
    src/main/res/     layout, drawable (vector icons + cat art), values, menu, mipmap (launcher icon), xml
```

## 1. Firebase setup (required, free "Spark" plan is enough)
1. Go to https://console.firebase.google.com -> **Add project** (name: PurrCare).
2. **Authentication** -> Get started -> **Sign-in method** -> enable **Email/Password**.
3. **Realtime Database** -> Create database -> choose a location -> start in **locked mode**.
   (Create the database BEFORE downloading google-services.json, so the database URL is inside it.)
4. Realtime Database -> **Rules** tab -> paste the content of `firebase_rules/database.rules.json` -> **Publish**.
5. Project settings (gear) -> **Add app** -> Android. Package name must be exactly: `com.purrcare.app`.
   SHA-1 is not needed for e-mail/password login. Download **google-services.json**.
6. Copy `google-services.json` into the `app/` folder (next to `app/build.gradle`).

Photos are stored as small Base64 text inside the Realtime Database (no Firebase Storage needed, because Storage now
usually requires upgrading to a paid plan). This is fine for a lab project; keep photos small.

## 2. Open in Android Studio
1. Extract the ZIP. 2. Android Studio -> **File > Open** -> choose the `PurrCare` folder (the one with `settings.gradle`).
3. Wait for **Gradle sync** (needs internet the first time; Android Studio downloads Gradle 8.7 and libraries).
4. If asked to install SDK Platform 34, click Install. If it suggests upgrading the Android Gradle Plugin, you may choose
   "Remind me later" (the project is set up for 8.5.2).
5. If sync says `File google-services.json is missing`, do step 1.6 above and click **Sync Now**.

Note: the wrapper scripts (`gradlew`) are not included because they need a binary jar that could not be downloaded
here. Android Studio does not need them. If you want them: in Android Studio terminal run `gradle wrapper` or use
Android Studio's built-in Gradle.

## 3. Run
Use an emulator (Device Manager -> Create Device, API 34) or a real phone (Settings -> About phone -> tap Build number
7 times -> Developer options -> USB debugging ON, connect USB). Press the green **Run** button.

## 4. Test the required features
- Register -> Home appears; close app and reopen: still logged in (Remember Me is ON after register).
  Log out, log in with Remember me unchecked, close app (swipe away) and reopen: Login screen appears.
- My Cats -> + -> add a cat with a photo (camera/gallery) -> card appears; edit; delete (confirmation).
- Health / Food / Reminders / Weight / Expense / Diary / Emergency: add, edit, delete from each screen.
- Reminders: add one 2 minutes in the future, allow notification permission, wait (screen off is fine).
- Weight: add 2+ records for a cat to see the line chart.
- Emergency: save clinic URL -> Visit button opens browser; add contact -> Call opens the dialer (no auto call).
- More -> Share PurrCare opens the share sheet.
- Shake: shake the phone twice quickly -> quick action dialog. On the emulator: ... (three dots) -> Virtual sensors -> Move.
- Real-time: open the same account on two devices, add a cat on one; it appears on the other.
- Validation: press Save with empty fields -> red errors, no crash.

## 5. Build an APK for your phone
1. **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. When it finishes click **locate** in the popup. The file is:
   `PurrCare/app/build/outputs/apk/debug/app-debug.apk`
3. Copy it to your phone (USB/Telegram/Drive), open it, allow "install unknown apps", install.
For a signed release APK: **Build > Generate Signed App Bundle / APK > APK**, create a keystore, choose `release`.
(Keep the keystore file and passwords safe.)

## Known limitations
- Reminder alarms are re-created when you open the app after a phone restart (no boot receiver).
- Without the "exact alarm" permission (Android 12+ settings), notifications may arrive a few minutes late.
- Photos live in the database as text, so very many large photos will slow the diary down.
