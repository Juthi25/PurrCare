# Lab Report Outline - PurrCare (Mobile Application Development Lab)

Check this outline against your course instructions and rename sections if your teacher uses different titles.
Add only real screenshots, real links and real test results.

## 1. Introduction
- What PurrCare is, the problem it solves (cat owners forgetting vaccines, feeding, expenses), objectives, tools (Android Studio, Java, XML, Firebase).

## 2. App Features (with screenshots)
| Feature | What it does | Screenshot |
|---|---|---|
| Splash / Login / Register | Firebase Auth, validation, Remember Me | [Insert screenshot] |
| Home dashboard | Welcome, totals, upcoming reminders, latest weight, monthly expense | [Insert screenshot] |
| My Cats (add/edit/delete) | Profiles with photo, auto age | [Insert screenshot] |
| Health tracker | Records + upcoming due dates | [Insert screenshot] |
| Food tracker | Schedules, mark done | [Insert screenshot] |
| Reminders | Notifications | [Insert screenshot] |
| Weight tracker | History + chart | [Insert screenshot] |
| Expense tracker | Monthly total, category summary (Taka) | [Insert screenshot] |
| Growth diary | Photo grid | [Insert screenshot] |
| Emergency info | Clinic website (ACTION_VIEW), call contacts (ACTION_DIAL) | [Insert screenshot] |
| Shake quick action | Accelerometer dialog | [Insert screenshot] |
| Share app | ACTION_SEND | [Insert screenshot] |

## 3. Components used, alternatives and reasons
| Requirement | Used | Alternative | Why this choice |
|---|---|---|---|
| Screens/navigation | Activities + Intents + BottomNavigationView | Fragments + Navigation Component | Required by the lab, simpler to understand |
| Layouts | ConstraintLayout, LinearLayout, CardView | RelativeLayout, Jetpack Compose | Standard XML approach |
| Lists | RecyclerView + Adapter + ViewHolder | ListView | Efficient, recycles views |
| Auth | Firebase Authentication | Own server, SQLite login | Secure, no server needed |
| Database | Firebase Realtime Database | SQLite/Room, Firestore | Real-time listeners, per-user cloud data |
| Settings | SharedPreferences | DataStore | Simple key/value |
| Photos | Camera/Gallery intents + Base64 in database | Firebase Storage | Storage needs a paid plan |
| Sensor | Accelerometer (SensorManager) | Gyroscope, light sensor | Shake is easy to demonstrate |
| Notifications | AlarmManager + NotificationManager | WorkManager | Fires at an exact time |
| Implicit intents | ACTION_DIAL, ACTION_VIEW, ACTION_SEND | Direct call (needs permission) | Safe: user confirms |

## 4. Firebase database structure
```
users/{userId}/profile | cats/{catId} | healthRecords/{id} | foodSchedules/{id} | reminders/{id}
              weightRecords/{id} | expenses/{id} | diary/{id} | emergencyContacts/{id} | settings/clinicUrl
```
Each record has a unique id (push key); cat-related records store `catId`. Include the security rules (`firebase_rules/database.rules.json`) and a screenshot of your Firebase console.

## 5. GitHub repository
[Insert your GitHub repository link here]

## 6. Problems faced and solutions
(Write your own real problems, e.g. Gradle sync errors, google-services.json package mismatch, notification permission on Android 13, ...)

## 7. Conclusion
Summary, what you learned, future work (dark mode, cat vaccination auto-reminders, cloud photo storage).

## 8. References
Android Developers documentation, Firebase documentation, Material Design guidelines (add exact URLs you used).
