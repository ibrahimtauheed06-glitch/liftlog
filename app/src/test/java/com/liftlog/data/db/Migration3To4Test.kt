package com.liftlog.data.db

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.ResultSetMetaData

class Migration3To4Test {

    private lateinit var conn: Connection
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        createVersion3Schema(conn)
        db = createSupportSQLiteDatabaseProxy(conn)
    }

    @After
    fun tearDown() {
        conn.close()
    }

    private fun createVersion3Schema(c: Connection) {
        val stmt = c.createStatement()
        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS `exercises` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `equipment` TEXT NOT NULL,
                `incrementKg` REAL NOT NULL DEFAULT 2.5,
                `plateKg` REAL,
                `repLo` INTEGER NOT NULL DEFAULT 5,
                `repHi` INTEGER NOT NULL DEFAULT 8,
                `defaultSets` INTEGER NOT NULL DEFAULT 3,
                `defaultWeightKg` REAL NOT NULL DEFAULT 0.0,
                `restSec` INTEGER NOT NULL DEFAULT 150,
                `isCompound` INTEGER NOT NULL DEFAULT 1,
                `bwFactor` REAL NOT NULL DEFAULT 0.0,
                `primaryMuscles` TEXT NOT NULL DEFAULT '',
                `secondaryMuscles` TEXT NOT NULL DEFAULT '',
                `setupNote` TEXT,
                `homeCapKg` REAL,
                `variantOf` INTEGER,
                `archived` INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent()
        )
        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS `templates` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `orderIndex` INTEGER NOT NULL
            );
            """.trimIndent()
        )
        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS `template_slots` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `templateId` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `slotOrder` INTEGER NOT NULL,
                `supersetGroup` INTEGER
            );
            """.trimIndent()
        )
        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS `workouts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `templateId` INTEGER,
                `startTs` INTEGER NOT NULL,
                `endTs` INTEGER,
                `note` TEXT,
                `isDiscarded` INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent()
        )
        stmt.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS `set_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `workoutId` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `ts` INTEGER NOT NULL,
                `weightKg` REAL NOT NULL,
                `reps` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `rpe` REAL,
                `flagSwung` INTEGER NOT NULL DEFAULT 0,
                `flagPain` INTEGER NOT NULL DEFAULT 0,
                `note` TEXT
            );
            """.trimIndent()
        )
        stmt.close()
    }

    @Test
    fun testMigration3To4_preservesData_updatesCableCrunch_archivesHangingLegRaise_addsDeclineCrunch() {
        // 1. Seed initial data simulating user's phone state
        val s = conn.createStatement()

        // Exercises
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, isCompound, bwFactor, archived) VALUES (1, 'Bench Press', 'barbell', 3, 5, 8, 1, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, isCompound, bwFactor, archived) VALUES (2, 'Hack Squat', 'machine', 3, 6, 10, 1, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, isCompound, bwFactor, archived) VALUES (3, 'Romanian Deadlift', 'barbell', 3, 6, 10, 1, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, isCompound, bwFactor, archived) VALUES (4, 'Calf Raise', 'machine', 3, 8, 15, 0, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, isCompound, bwFactor, archived) VALUES (16, 'Hanging Leg Raise', 'bodyweight', 2, 8, 15, 0, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, isCompound, bwFactor, archived) VALUES (17, 'Leg Extension', 'machine', 2, 10, 15, 0, 0.0, 1)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, isCompound, bwFactor, archived) VALUES (18, 'Deadlift', 'barbell', 2, 5, 5, 1, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, isCompound, bwFactor, archived) VALUES (19, 'Leg Press', 'machine', 3, 8, 12, 1, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, isCompound, bwFactor, archived) VALUES (20, 'Reverse Hamstring Curl', 'machine', 3, 8, 12, 0, 0.0, 0)")
        // User's manual "cable crunches"
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, isCompound, bwFactor, archived) VALUES (21, 'cable crunches', 'cable', 2, 10, 15, 0, 0.0, 0)")

        // Templates
        s.executeUpdate("INSERT INTO templates (id, name, orderIndex) VALUES (1, 'Upper A', 0)")
        s.executeUpdate("INSERT INTO templates (id, name, orderIndex) VALUES (2, 'Lower A', 1)")
        s.executeUpdate("INSERT INTO templates (id, name, orderIndex) VALUES (3, 'Upper B', 2)")
        s.executeUpdate("INSERT INTO templates (id, name, orderIndex) VALUES (4, 'Lower B', 3)")

        // Slots Lower A: Hack Squat (0), Romanian Deadlift (1), Calf Raise (2), Hanging Leg Raise (3), Leg Extension (4)
        s.executeUpdate("INSERT INTO template_slots (id, templateId, exerciseId, slotOrder) VALUES (10, 2, 2, 0)")
        s.executeUpdate("INSERT INTO template_slots (id, templateId, exerciseId, slotOrder) VALUES (11, 2, 3, 1)")
        s.executeUpdate("INSERT INTO template_slots (id, templateId, exerciseId, slotOrder) VALUES (12, 2, 4, 2)")
        s.executeUpdate("INSERT INTO template_slots (id, templateId, exerciseId, slotOrder) VALUES (13, 2, 16, 3)")
        s.executeUpdate("INSERT INTO template_slots (id, templateId, exerciseId, slotOrder) VALUES (14, 2, 17, 4)")

        // Slots Lower B: Deadlift (0), Leg Press (1), Reverse Hamstring Curl (2), Calf Raise (3), cable crunches (4)
        s.executeUpdate("INSERT INTO template_slots (id, templateId, exerciseId, slotOrder) VALUES (20, 4, 18, 0)")
        s.executeUpdate("INSERT INTO template_slots (id, templateId, exerciseId, slotOrder) VALUES (21, 4, 19, 1)")
        s.executeUpdate("INSERT INTO template_slots (id, templateId, exerciseId, slotOrder) VALUES (22, 4, 20, 2)")
        s.executeUpdate("INSERT INTO template_slots (id, templateId, exerciseId, slotOrder) VALUES (23, 4, 4, 3)")
        s.executeUpdate("INSERT INTO template_slots (id, templateId, exerciseId, slotOrder) VALUES (24, 4, 21, 4)")

        // Workouts and logged sets linked to cable crunches (21) and Hanging Leg Raise (16)
        s.executeUpdate("INSERT INTO workouts (id, templateId, startTs, endTs, note, isDiscarded) VALUES (101, 4, 1000000, 1003600, 'Great workout', 0)")
        s.executeUpdate("INSERT INTO set_entries (id, workoutId, exerciseId, ts, weightKg, reps, type) VALUES (1, 101, 21, 1001000, 25.0, 12, 'working')")
        s.executeUpdate("INSERT INTO set_entries (id, workoutId, exerciseId, ts, weightKg, reps, type) VALUES (2, 101, 21, 1002000, 25.0, 11, 'working')")
        s.executeUpdate("INSERT INTO set_entries (id, workoutId, exerciseId, ts, weightKg, reps, type) VALUES (3, 101, 16, 1003000, 0.0, 15, 'working')")
        s.close()

        // 2. RUN MIGRATION 3 -> 4
        LiftLogDatabase.MIGRATION_3_4.migrate(db)

        // 3. ASSERTIONS:
        // (a) Existing data survives:
        val rsWorkout = conn.createStatement().executeQuery("SELECT id, templateId, note FROM workouts WHERE id = 101")
        assertTrue("Workout 101 must survive", rsWorkout.next())
        assertEquals("Great workout", rsWorkout.getString("note"))
        rsWorkout.close()

        val rsSets = conn.createStatement().executeQuery("SELECT id, exerciseId, weightKg, reps FROM set_entries WHERE id IN (1, 2, 3) ORDER BY id")
        assertTrue(rsSets.next())
        assertEquals(21L, rsSets.getLong("exerciseId"))
        assertEquals(25.0, rsSets.getDouble("weightKg"), 0.001)
        assertTrue(rsSets.next())
        assertEquals(21L, rsSets.getLong("exerciseId"))
        assertTrue(rsSets.next())
        assertEquals(16L, rsSets.getLong("exerciseId"))
        rsSets.close()

        // Hanging Leg Raise still exists and is archived
        val rsHlr = conn.createStatement().executeQuery("SELECT id, name, archived FROM exercises WHERE id = 16")
        assertTrue("Hanging Leg Raise row must survive", rsHlr.next())
        assertEquals(1, rsHlr.getInt("archived"))
        rsHlr.close()

        // (b) Exactly one Cable Crunch row
        val rsCable = conn.createStatement().executeQuery("SELECT * FROM exercises WHERE LOWER(name) IN ('cable crunch', 'cable crunches')")
        assertTrue("Cable Crunch row must exist", rsCable.next())
        val cableId = rsCable.getLong("id")
        assertEquals(21L, cableId) // Updated in place!
        assertEquals("Cable Crunch", rsCable.getString("name"))
        assertEquals("cable", rsCable.getString("equipment"))
        assertEquals(5.0, rsCable.getDouble("plateKg"), 0.001)
        assertEquals(5.0, rsCable.getDouble("incrementKg"), 0.001)
        assertEquals(3, rsCable.getInt("defaultSets"))
        assertEquals(10, rsCable.getInt("repLo"))
        assertEquals(15, rsCable.getInt("repHi"))
        assertEquals(75, rsCable.getInt("restSec"))
        assertEquals(0, rsCable.getInt("isCompound"))
        assertEquals(0.0, rsCable.getDouble("bwFactor"), 0.001)
        assertEquals("Abs", rsCable.getString("primaryMuscles"))
        assertEquals(0, rsCable.getInt("archived"))
        assertNotNull(rsCable.getString("setupNote"))
        assertFalse("There must be EXACTLY ONE Cable Crunch row", rsCable.next())
        rsCable.close()

        // Check Lower B slot 5 points to Cable Crunch
        val rsLowerBSlot5 = conn.createStatement().executeQuery("SELECT * FROM template_slots WHERE templateId = 4 AND slotOrder = 4")
        assertTrue("Lower B slot 5 must exist", rsLowerBSlot5.next())
        assertEquals(cableId, rsLowerBSlot5.getLong("exerciseId"))
        rsLowerBSlot5.close()

        // (c) No other exercise or template changed, Decline Crunch in Lower A slot 4
        val rsDecline = conn.createStatement().executeQuery("SELECT * FROM exercises WHERE LOWER(name) = 'decline crunch'")
        assertTrue("Decline Crunch must be inserted", rsDecline.next())
        val declineId = rsDecline.getLong("id")
        assertEquals("bodyweight", rsDecline.getString("equipment"))
        assertEquals(2.5, rsDecline.getDouble("incrementKg"), 0.001)
        assertEquals(10.0, rsDecline.getDouble("homeCapKg"), 0.001)
        assertEquals(3, rsDecline.getInt("defaultSets"))
        assertEquals(12, rsDecline.getInt("repLo"))
        assertEquals(20, rsDecline.getInt("repHi"))
        assertEquals(75, rsDecline.getInt("restSec"))
        assertEquals(0, rsDecline.getInt("isCompound"))
        assertEquals(0.0, rsDecline.getDouble("bwFactor"), 0.001)
        assertEquals("Abs", rsDecline.getString("primaryMuscles"))
        rsDecline.close()

        // Lower A slots check: 0=Hack Squat, 1=Romanian Deadlift, 2=Calf Raise, 3=Decline Crunch, 4=Leg Extension
        val rsLowerASlots = conn.createStatement().executeQuery("SELECT slotOrder, exerciseId FROM template_slots WHERE templateId = 2 ORDER BY slotOrder")
        assertTrue(rsLowerASlots.next())
        assertEquals(0, rsLowerASlots.getInt("slotOrder"))
        assertEquals(2L, rsLowerASlots.getLong("exerciseId")) // Hack Squat
        assertTrue(rsLowerASlots.next())
        assertEquals(1, rsLowerASlots.getInt("slotOrder"))
        assertEquals(3L, rsLowerASlots.getLong("exerciseId")) // Romanian Deadlift
        assertTrue(rsLowerASlots.next())
        assertEquals(2, rsLowerASlots.getInt("slotOrder"))
        assertEquals(4L, rsLowerASlots.getLong("exerciseId")) // Calf Raise
        assertTrue(rsLowerASlots.next())
        assertEquals(3, rsLowerASlots.getInt("slotOrder"))
        assertEquals(declineId, rsLowerASlots.getLong("exerciseId")) // Decline Crunch replaces Hanging Leg Raise!
        assertTrue(rsLowerASlots.next())
        assertEquals(4, rsLowerASlots.getInt("slotOrder"))
        assertEquals(17L, rsLowerASlots.getLong("exerciseId")) // Leg Extension
        assertFalse(rsLowerASlots.next())
        rsLowerASlots.close()

        // Hanging Leg Raise must NOT be in template_slots anywhere
        val rsHlrSlots = conn.createStatement().executeQuery("SELECT * FROM template_slots WHERE exerciseId = 16")
        assertFalse("Hanging Leg Raise must be removed from template slots", rsHlrSlots.next())
        rsHlrSlots.close()

        // Check Bench Press unchanged
        val rsBench = conn.createStatement().executeQuery("SELECT * FROM exercises WHERE id = 1")
        assertTrue(rsBench.next())
        assertEquals("Bench Press", rsBench.getString("name"))
        assertEquals(3, rsBench.getInt("defaultSets"))
        assertEquals(5, rsBench.getInt("repLo"))
        assertEquals(8, rsBench.getInt("repHi"))
        rsBench.close()
    }

    private fun createSupportSQLiteDatabaseProxy(conn: Connection): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
                    val methodName = method.name
                    if (methodName == "execSQL" && args != null) {
                        val sql = args[0] as String
                        if (args.size > 1 && args[1] != null) {
                            val bindArgs = args[1] as Array<out Any?>
                            val ps = conn.prepareStatement(sql)
                            bindArgs.forEachIndexed { idx, arg ->
                                ps.setObject(idx + 1, arg)
                            }
                            ps.executeUpdate()
                            ps.close()
                        } else {
                            val stmt = conn.createStatement()
                            stmt.executeUpdate(sql)
                            stmt.close()
                        }
                        return null
                    } else if (methodName == "query" && args != null) {
                        val sql = if (args[0] is String) args[0] as String else (args[0] as SupportSQLiteQuery).sql
                        val bindArgs = if (args.size > 1 && args[1] is Array<*>) args[1] as Array<out Any?> else null

                        val rs = if (bindArgs != null) {
                            val ps = conn.prepareStatement(sql)
                            bindArgs.forEachIndexed { idx, arg ->
                                ps.setObject(idx + 1, arg)
                            }
                            ps.executeQuery()
                        } else {
                            conn.createStatement().executeQuery(sql)
                        }
                        return createCursorProxy(rs)
                    }
                    return null
                }
            }
        ) as SupportSQLiteDatabase
    }

    private fun createCursorProxy(rs: ResultSet): Cursor {
        val md = rs.metaData
        return Proxy.newProxyInstance(
            Cursor::class.java.classLoader,
            arrayOf(Cursor::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
                    return when (method.name) {
                        "moveToFirst" -> rs.next()
                        "moveToNext" -> rs.next()
                        "close" -> {
                            rs.close()
                            null
                        }
                        "isClosed" -> rs.isClosed
                        "getLong" -> {
                            val idx = (args?.get(0) as Int) + 1
                            rs.getLong(idx)
                        }
                        "getInt" -> {
                            val idx = (args?.get(0) as Int) + 1
                            rs.getInt(idx)
                        }
                        "getString" -> {
                            val idx = (args?.get(0) as Int) + 1
                            rs.getString(idx)
                        }
                        "getDouble" -> {
                            val idx = (args?.get(0) as Int) + 1
                            rs.getDouble(idx)
                        }
                        "getColumnIndex" -> {
                            val name = args?.get(0) as String
                            for (i in 1..md.columnCount) {
                                if (md.getColumnName(i).equals(name, ignoreCase = true) ||
                                    md.getColumnLabel(i).equals(name, ignoreCase = true)
                                ) {
                                    return i - 1
                                }
                            }
                            -1
                        }
                        else -> null
                    }
                }
            }
        ) as Cursor
    }
}
