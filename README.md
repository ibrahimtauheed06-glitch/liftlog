# LiftLog — Offline Personal Workout Tracker

LiftLog is a 100% offline, privacy-first personal workout tracker for Android built with modern Android development standards. It features deterministic double progression algorithms, automatic rest timing, bodyweight & protein tracking, volume distribution analytics, and safe local backup/export.

---

## 🛡️ Hard Constraints & Architecture Compliance

1. **100% Offline & Private**:
   - `android.permission.INTERNET` is **never declared** in `AndroidManifest.xml` and cannot be requested.
   - `android:allowBackup="false"` is enforced to prevent unintended cloud sync.
   - Zero analytics, telemetry, cloud services, AI, or ML libraries.
   - All predictions and target calculations are plain, deterministic formulas implemented in a pure-Kotlin `com.liftlog.engine` package with JUnit tests.
2. **Modern Stack**:
   - **Language**: Kotlin 1.9 (JVM 17 target).
   - **UI**: Jetpack Compose + Material 3 (dark theme enforced by default).
   - **Persistence**: Room Database with `exportSchema = true` and versioned non-destructive migrations (`MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATION_3_4`).
   - **Settings**: Jetpack DataStore Preferences.
   - **State**: ViewModel + Kotlin StateFlow / Flow.
   - **Dependency Injection**: Manual DI via `AppContainer` (no Hilt/Koin).
   - **Compatibility**: `minSdk = 26` (Android 8.0 Oreo), `targetSdk = 34`.
   - **Single Module**: `:app` module.
3. **Speed & Ergonomics**:
   - Logging a set requires at most **2 taps** when prefilled values are correct (or 1 tap on the large green/amber `LOG SET` button).
   - Steppers with large tap targets for weight (`+/- incrementKg`) and reps (`+/- 1`).
   - Active workout screen stays awake using `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` via `DisposableEffect`.
4. **Historical Data Integrity**:
   - History is **never hard-deleted**: exercises can be archived, renamed, or merged into parent exercises using `variantOf`.
   - Past sets are completely editable; all derived values (e1RM, PRs, volume, and next-session progression) recompute on the fly.
   - Active workout state is persisted to SQLite and DataStore on **every logged set**, allowing seamless resumption after process death or app restart.

---

## ⚙️ Deterministic Progression Engine (`com.liftlog.engine`)

All formulas reside in the pure-Kotlin `com.liftlog.engine` package and are tested via JUnit:

1. **e1RM (Estimated 1-Rep Max)**:
   $$\text{e1RM} = \text{load} \times \left(1 + \frac{\text{reps} + (10 - \text{RPE})}{30}\right)$$
   - When RPE is not specified, it defaults to **9.0**.
   - $\text{load} = \text{weightKg} + \text{bwFactor} \times \text{bodyweight7DayAvg}$.
   - Only calculated for $\text{reps} \le 12$. For sets with $>12$ reps, e1RM returns null, and progression directly compares weight and reps.
   - "Top set" is defined as the highest-e1RM working set with neither pain nor swung flags.
2. **Double Progression Targets**:
   - **(a)** If all valid working sets hit `repHi`: target advances to $\text{weight} + \text{incrementKg}$, with reps resetting to `repLo`.
   - **(b)** Otherwise: target stays at the same weight, aiming for $+1\text{ rep}$ on the weakest set ($\min(\text{reps}) + 1$).
   - **(c) Back-off**: If the top set fails to hit `repLo` across **2 consecutive sessions**, the engine suggests a $-7.5\%$ back-off load (rounded to `incrementKg`), targeting `repLo`.
   - **(d) Reps-Only Exercises** (e.g. `incrementKg == 0`): When all sets hit `repHi`, the engine suggests extending `repHi` by $+2\text{ reps}$.
3. **Gap Detection**:
   - Session last completed **10–20 days ago**: weight is scaled to **95%** of last working weight (rounded to `incrementKg`).
   - Session last completed **21+ days ago**: weight is scaled to **90%** of last working weight (rounded to `incrementKg`).
   - A `"Return after N days"` chip is displayed on the exercise card.
4. **Stall Detection**:
   - Flagged when there is no new best e1RM across the last 3 completed sessions of an exercise.
   - For load-0 exercises (`weightKg == 0` and `bwFactor == 0`, e.g. bodyweight decline crunches), e1RM is meaningless; stall detection instead compares top-set reps across the last 3 completed sessions (stalled if no new best reps in the last 3 sessions). Progression remains unchanged: hitting `repHi` on all sets adds `incrementKg` and resets reps to `repLo`.
5. **Deload Engine**:
   - If $\ge 3$ exercises are stalled within the last 14 days, a Deload banner appears on the Train tab.
   - **Accept**: Next 7 days use 90% weight and 1 fewer set ($\min 1\text{ set}$).
   - **Dismiss**: Snoozes the deload prompt for 14 days.
6. **Set Flags**:
   - `swung` sets: Excluded from e1RM, PRs, and progression; **still counted toward muscle volume**.
   - `pain` sets: Excluded from e1RM, PRs, progression, **and volume**.
   - If $\ge 2$ pain flags occur on an exercise within 14 days, a warning chip is displayed: *"2+ pain flags recently: consider lowering load or swapping"*.
7. **Personal Records (PRs)**:
   - Evaluates best e1RM and best reps at a specific weight.
   - Shows an instant celebration banner when logged.
8. **Warm-up Sets**:
   - The first compound lift of each workout automatically prefills a warm-up set at **60%** of target weight.
   - Optional setting: 2-set ramp (**50%** and **75%**).
   - Warm-up sets are excluded from all stats and volume metrics.
9. **HOME Phase Mode**:
   - Never suggests weights above `homeCapKg` (dumbbell lifts default to 10kg cap).
   - Pushes reps up to 30. Once 30 reps is reached, displays: `"3s lowering / single-arm / harder variation"`.
   - *"Return to gym"* button scales the next session to 90% of the last gym working weight.
10. **Plate Calculator**:
    - Calculates plates required per side for barbell lifts based on `barWeight` (default 20kg) and `availablePlates` (e.g., 20, 15, 10, 5, 2.5, 1.25).
    - Cable/machine stack weights formatted as `"X plates (Ykg)"` when `plateKg` is configured.

---

## ⏱️ Rest Timer (`RestTimerService`)

- **Automatic Trigger**: Starts automatically upon logging any working or drop set.
- **Duration**:
  - Compound lifts: 150 seconds.
  - Isolation lifts: 75 seconds.
  - Supersets: 30 seconds after the first exercise of a pair, normal rest after the second.
- **Process & Screen-Off Survival**:
  - Runs as an Android Foreground Service with an ongoing notification.
  - Calculates remaining time from a stored epoch end timestamp (`targetEndTs`) rather than naive interval ticks.
  - Survives process restarts and screen-off.
- **Notification & In-App Controls**:
  - `+15s`, `-15s`, and `Skip` actions directly in the notification and on the in-app banner.
  - Vibration pattern and notification chime when the timer reaches zero.
  - Automatically requests `POST_NOTIFICATIONS` permission on Android 13+ (API 33+).

---

## 🥗 Protein & Bodyweight Tracking

- **Protein Tab**:
  - Precise grams input (no rigid preset buttons) + time picker (defaulting to current time).
  - Respects customizable `dayRolloverHour` (default 4:00 AM).
  - Shows total grams, remaining grams, progress bar, 7-day average, and days target was hit ($X/7$).
  - Full history with inline editing and deletion.
- **Bodyweight Tracker**:
  - Log scale weight with timestamp.
  - Computes and displays the rolling 7-day average weight used for bodyweight load calculations.

---

## 📊 Analytics & Offline Charts

- **Timeframe Toggle**: Weekly (default) or Monthly.
- **Exercise Comparison**: Last session top set vs previous session top set with e1RM delta percentage.
- **Volume Breakdown**: Weekly hard sets per muscle group (primary = 1.0, secondary = 0.5) against `weeklySetTarget`.
- **Canvas Charts** (100% offline, pure Compose Canvas):
  - Exercise e1RM trend line.
  - Weekly hard sets per muscle bars.
  - Daily protein bars with dashed target threshold line.
  - Bodyweight trend line.

---

## 💾 Local Backup & SAF Export

- **Export**: Generates full `liftlog_backup_<timestamp>.json` alongside three CSV files (`sets.csv`, `protein.csv`, `bodyweight.csv`) into any user-selected directory via Android's Storage Access Framework.
- **Import**: Allows restoring from a JSON backup with user confirmation:
  - **Merge**: Merges backup entries with existing records.
  - **Replace**: Clears the current database and restores backup state exactly.
- **14-Day Reminder**: Displays a banner when no export has occurred in 14+ days.

---

## 🏋️ Seed Routine Data

Pre-seeded with a 4-day Upper/Lower split (all fields fully editable):

| Routine | Exercises |
| :--- | :--- |
| **Upper A (Mon)** | Bench Press (barbell, 3x5-8, +2.5kg, compound)<br>Weighted Pull-up (bw, 3x5-8, +2.5kg, compound)<br>Seated/Chest-Supported Row (cable, 3x8-12, +5kg)<br>Cuffed Lateral Raise (cable, 5kg plate, 3x10-20, +5kg)<br>Cuffed Triceps Extension (cable, 5kg plate, 2x8-12, +5kg)<br>Strict Pad Curl (dumbbell, 2x8-12, +2.5kg, 10kg cap) |
| **Lower A (Tue)** | Hack Squat (machine, 3x6-10, +5kg, compound)<br>Romanian Deadlift (barbell, 3x6-10, +2.5kg, compound)<br>Calf Raise (machine, 3x8-15, +5kg)<br>Decline Crunch (bw, 3x12-20, +2.5kg, 10kg cap, bwFactor 0)<br>Leg Extension (machine, 2x10-15, +5kg, archived by default)<br>Hanging Leg Raise (archived by default) |
| **Upper B (Thu)** | Smith Overhead Press (machine, 3x6-10, +2.5kg, compound)<br>Weighted Dip (bw, 3x6-10, +2.5kg, compound)<br>Lat Pulldown (cable, 3x8-12, +5kg)<br>Cuffed Lateral Raise (cable, shared)<br>Rear Delt Fly/Face Pull (cable, 2x12-20, +5kg)<br>Hammer Curl (dumbbell, 2x8-12, +2.5kg, 10kg cap)<br>Overhead Triceps Extension (cable, 2x8-12, +5kg) |
| **Lower B (Fri)** | Deadlift (barbell, 2x5, +2.5kg, compound)<br>Leg Press (machine, 3x8-12, +5kg, compound)<br>Reverse Hamstring Curl (machine, 3x8-12, +5kg)<br>Calf Raise (machine, shared)<br>Cable Crunch (cable, 5kg plate, 3x10-15, +5kg, bwFactor 0) |

---

## 📝 List of Assumptions & Sensible Decisions

1. **Database Seed Timing**: Database seeding executes upon initial database creation (`RoomDatabase.Callback.onCreate`) via `database.seedInitialData()`. If the user imports data or wipes the database, the seed does not overwrite existing data.
2. **Bodyweight Average Fallback**: If no bodyweight entries exist in the 7-day window, `bodyweight7DayAvg` defaults to `0.0` (for weighted pullups/dips without scale entries, the added weight alone is tracked until a bodyweight is logged).
3. **Weakest Set Progression**: For double progression condition (b) (when not all sets hit `repHi`), the next target is $\min(\text{reps}) + 1$.
4. **Deload Dismiss Snooze**: When the user dismisses a Deload prompt, it is snoozed for 14 calendar days by recording `lastDeloadDismissTs`.
5. **Pain Flag Volume Exclusion**: As specified, `flagPain` sets are excluded from e1RM, PRs, progression, and volume. `flagSwung` sets are excluded from e1RM, PRs, and progression, but still counted as volume.
6. **Superset Rest Alternation**: When exercises share a `supersetGroup` in a template, logging a set on the first slot starts a 30s rest timer; logging a set on the second slot starts the exercise's standard rest duration (75s or 150s).
7. **Screen Wake Lock**: When viewing the active workout screen, `FLAG_KEEP_SCREEN_ON` is acquired and released when navigating away.
8. **Two-Stage Workout Flow**: Tapping any template on the Train tab opens a view-only preview screen showing planned exercises, sets, and rep ranges. The workout only officially starts when the user taps "START WORKOUT", preventing accidental session creation.
9. **Workout Duration Timer**: When an active workout begins, an overall elapsed timer starts from `startTs` and ticks continuously at the top of the active workout screen.
10. **Discard & Recycle Bin**: Workouts can be discarded and moved into a safe local Recycle Bin (accessible in More > Recycle Bin) where they can be restored or permanently removed.
11. **Direct Keyboard Input**: Set rows feature editable numeric text fields for both weight and reps alongside `+/-` steppers for instant, flexible adjustments.
12. **Workout Template Editor**: Users can add, reorder, remove exercises, or create entirely new custom templates in More > Workout Templates.
13. **Streamlined Experience**: Plate calculator, cardio/runs, and HOME/GYM phase mode have been removed in favor of a clean, gym-focused lifting tracker.
14. **Quick Exercise Swap**: If a machine or equipment is taken in the gym, lifters can tap the swap icon on the exercise card to choose an alternative for that session only; the underlying template is never modified.
15. **Warmup Ramp Generator**: Multi-step progressive warmups (e.g. Barbell 10 reps, 60% 5 reps, 80% 2 reps) can be generated and selected with a single tap directly above the input fields.
16. **Previous Session Note Peek**: Displays cues and notes from the last completed session directly on the active exercise card.
17. **10-Second Rest Warning Tone**: Plays a subtle audio pip with audio ducking and a light vibration cue 10 seconds before the rest timer expires so lifters can prepare without looking at their phone.
18. **Lowkey Progression Readiness (Analytics Tab Only)**: A dedicated dashboard in Analytics categorizes lifts into Ready to Advance (+load), Building Reps (+1 rep), Back-off Advised, or Stalled, with zero intrusive popups during workouts. Also provides an estimated 1RM, 3RM, 5RM, 8RM, 10RM, 12RM reference table based on all-time bests.
19. **Smoothed 7-Day Bodyweight Trendline**: Overlays a rolling 7-day average on the bodyweight chart to filter daily water-weight noise.
20. **Offline Evening Protein Reminder**: Local AlarmManager notification at 8:00 PM if logged protein is below the daily target (customizable/toggleable in Settings).
21. **Automated Monthly Rolling Local Backup**: Automatically creates/overwrites a silent local JSON backup file (`liftlog_auto_backup.json`) once a month in device storage.

---

## 🛠️ Verification & Build Commands

- **Run all engine unit tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- **Compile and package Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
  The generated APK is located at:
  `app/build/outputs/apk/debug/app-debug.apk`
