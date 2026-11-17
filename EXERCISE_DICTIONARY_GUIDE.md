# Exercise Dictionary Guide & Schema

You can edit either `exercise_dictionary.csv` or `exercise_dictionary.json` directly in the project root.
When you are done making changes or adding new exercises, let me know and I will import your changes directly into the app and database without data loss.

---

### Available Fields & Data Types

| Field | Type | Description | Allowed / Example Values |
|---|---|---|---|
| `id` | Integer | Unique identifier. Keep existing IDs untouched to preserve history. For new exercises, omit or set to a new number. | `1`, `2`, `23` |
| `name` | String | Display name of the exercise | `"Incline Dumbbell Press"`, `"Cable Crunch"` |
| `equipment` | String | Equipment category used for warm-up and progression rules | `barbell`, `dumbbell`, `cable`, `machine`, `bodyweight` |
| `defaultSets` | Integer | Default target number of sets per session | `2`, `3`, `4` |
| `repLo` | Integer | Lower bound of the target repetition range | `5`, `6`, `8`, `10`, `12` |
| `repHi` | Integer | Upper bound of the target repetition range | `8`, `10`, `12`, `15`, `20` |
| `restSec` | Integer | Prescribed rest timer duration in seconds | `75` (isolation), `150` (compounds), `180` (deadlift) |
| `incrementKg` | Decimal | Smallest load progression increment in kg | `2.5` (barbell/dumbbell), `5.0` (cables/machines), `0.0` (calisthenics reps-only) |
| `plateKg` | Decimal (optional) | Starting plate / pin weight in kg | `5.0` (cables), or empty / `null` |
| `defaultWeightKg` | Decimal | Base weight if starting fresh with no history | `0.0` or initial test weight |
| `isCompound` | Boolean | Whether the exercise is a multi-joint compound (triggers 60% warm-up set on first compound) | `true` or `false` |
| `bwFactor` | Decimal | Bodyweight multiplier for e1RM & weighted bodyweight exercises | `1.0` (Weighted Pull-up, Weighted Dip), `0.0` (everything else) |
| `homeCapKg` | Decimal (optional) | Dumbbell/equipment maximum ceiling in Home phase | `10.0` or empty / `null` |
| `primaryMuscles` | String | Primary muscle group targeted (counts as 1 full set for volume) | `"Chest"`, `"Lats"`, `"Quads"`, `"Hamstrings"`, `"Side Delts"`, `"Abs"` |
| `secondaryMuscles` | String | Secondary muscle groups (counts as 0.5 set for volume) | `"Triceps, Front Delts"`, `"Biceps, Upper Back"` |
| `setupNote` | String (optional) | Setup/form cues displayed on the exercise card | Form cues, posture instructions |
| `archived` | Boolean | Whether the exercise is archived from the active exercise selection list | `false` (active), `true` (archived) |

---

### Tips for Editing
1. **Adding New Exercises**: Add a new row to `exercise_dictionary.csv` or a new JSON object in `exercise_dictionary.json`.
2. **Changing Reps / Sets / Rest**: Change `repLo`, `repHi`, `defaultSets`, and `restSec` directly on any exercise.
3. **Form Cues**: Add or improve `setupNote` for any exercise to have helpful form reminders while training.
