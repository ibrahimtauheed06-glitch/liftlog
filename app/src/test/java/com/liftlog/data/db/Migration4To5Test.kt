package com.liftlog.data.db

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import org.junit.After
import org.junit.Assert.assertEquals
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

class Migration4To5Test {

    private lateinit var conn: Connection
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        createVersion4Schema(conn)
        db = createSupportSQLiteDatabaseProxy(conn)
    }

    @After
    fun tearDown() {
        conn.close()
    }

    private fun createVersion4Schema(c: Connection) {
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
    fun testMigration4To5_updatesSeededAndUserExercises_preservesHistory() {
        val s = conn.createStatement()

        // 1. Seed existing exercises simulating user's phone state
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, restSec, incrementKg, isCompound, bwFactor, archived) VALUES (3, 'Seated/Chest-Supported Row', 'cable', 3, 8, 12, 75, 5.0, 0, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, restSec, incrementKg, plateKg, isCompound, bwFactor, archived) VALUES (4, 'Cuffed Lateral Raise', 'cable', 3, 10, 20, 75, 5.0, 5.0, 0, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, restSec, incrementKg, isCompound, bwFactor, archived) VALUES (9, 'Lat Pulldown', 'cable', 3, 8, 12, 75, 5.0, 0, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, restSec, incrementKg, isCompound, bwFactor, archived) VALUES (18, 'Deadlift', 'barbell', 2, 5, 5, 180, 2.5, 1, 0.0, 0)")

        // User-added exercises on phone
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, restSec, incrementKg, isCompound, bwFactor, archived) VALUES (23, 'Lat pullover', 'machine', 3, 5, 8, 150, 2.5, 0, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, restSec, incrementKg, isCompound, bwFactor, archived) VALUES (24, 'rows infront of mirror near abs', 'barbell', 3, 5, 8, 150, 2.5, 0, 0.0, 0)")
        s.executeUpdate("INSERT INTO exercises (id, name, equipment, defaultSets, repLo, repHi, restSec, incrementKg, isCompound, bwFactor, archived) VALUES (25, 'db lateral raises', 'barbell', 3, 10, 20, 150, 2.5, 0, 0.0, 0)")

        // Workout and sets linked to user exercises
        s.executeUpdate("INSERT INTO workouts (id, templateId, startTs, endTs, note, isDiscarded) VALUES (200, 1, 2000000, 2003600, 'Leg day note', 0)")
        s.executeUpdate("INSERT INTO set_entries (id, workoutId, exerciseId, ts, weightKg, reps, type) VALUES (50, 200, 23, 2001000, 40.0, 10, 'working')")
        s.executeUpdate("INSERT INTO set_entries (id, workoutId, exerciseId, ts, weightKg, reps, type) VALUES (51, 200, 24, 2002000, 30.0, 8, 'working')")
        s.executeUpdate("INSERT INTO set_entries (id, workoutId, exerciseId, ts, weightKg, reps, type) VALUES (52, 200, 25, 2003000, 10.0, 15, 'working')")
        s.close()

        // 2. Run Migration 4 -> 5
        LiftLogDatabase.MIGRATION_4_5.migrate(db)

        // 3. Verify Part A: Seeded Exercises
        // 1. Seated/Chest-Supported Row: restSec 90
        val rsRow = conn.createStatement().executeQuery("SELECT restSec FROM exercises WHERE id = 3")
        assertTrue(rsRow.next())
        assertEquals(90, rsRow.getInt("restSec"))
        rsRow.close()

        // 2. Lat Pulldown: restSec 90
        val rsLat = conn.createStatement().executeQuery("SELECT restSec FROM exercises WHERE id = 9")
        assertTrue(rsLat.next())
        assertEquals(90, rsLat.getInt("restSec"))
        rsLat.close()

        // 3. Cuffed Lateral Raise: repLo 8, repHi 15
        val rsCuff = conn.createStatement().executeQuery("SELECT repLo, repHi, restSec, incrementKg, plateKg FROM exercises WHERE id = 4")
        assertTrue(rsCuff.next())
        assertEquals(8, rsCuff.getInt("repLo"))
        assertEquals(15, rsCuff.getInt("repHi"))
        assertEquals(75, rsCuff.getInt("restSec"))
        assertEquals(5.0, rsCuff.getDouble("incrementKg"), 0.001)
        assertEquals(5.0, rsCuff.getDouble("plateKg"), 0.001)
        rsCuff.close()

        // 4. Deadlift: repHi 8, repLo stays 5
        val rsDl = conn.createStatement().executeQuery("SELECT repLo, repHi, defaultSets, restSec FROM exercises WHERE id = 18")
        assertTrue(rsDl.next())
        assertEquals(5, rsDl.getInt("repLo"))
        assertEquals(8, rsDl.getInt("repHi"))
        assertEquals(2, rsDl.getInt("defaultSets"))
        assertEquals(180, rsDl.getInt("restSec"))
        rsDl.close()

        // 4. Verify Part B: User-added exercises
        // 5. "db lateral raises" -> "Dumbbell Lateral Raise" (id 25)
        val rsDbLat = conn.createStatement().executeQuery("SELECT * FROM exercises WHERE id = 25")
        assertTrue(rsDbLat.next())
        assertEquals("Dumbbell Lateral Raise", rsDbLat.getString("name"))
        assertEquals("dumbbell", rsDbLat.getString("equipment"))
        assertEquals(3, rsDbLat.getInt("defaultSets"))
        assertEquals(8, rsDbLat.getInt("repLo"))
        assertEquals(15, rsDbLat.getInt("repHi"))
        assertEquals(75, rsDbLat.getInt("restSec"))
        assertEquals(2.5, rsDbLat.getDouble("incrementKg"), 0.001)
        assertEquals(10.0, rsDbLat.getDouble("homeCapKg"), 0.001)
        assertEquals(0, rsDbLat.getInt("isCompound"))
        assertEquals(0.0, rsDbLat.getDouble("bwFactor"), 0.001)
        assertEquals("Side Delts", rsDbLat.getString("primaryMuscles"))
        assertEquals("", rsDbLat.getString("secondaryMuscles"))
        assertTrue(rsDbLat.getString("setupNote").contains("scapular plane"))
        rsDbLat.close()

        // 6. "rows infront of mirror near abs" -> "Unilateral Chest-Supported Row" (id 24)
        val rsUniRow = conn.createStatement().executeQuery("SELECT * FROM exercises WHERE id = 24")
        assertTrue(rsUniRow.next())
        assertEquals("Unilateral Chest-Supported Row", rsUniRow.getString("name"))
        assertEquals("machine", rsUniRow.getString("equipment"))
        assertEquals(3, rsUniRow.getInt("defaultSets"))
        assertEquals(8, rsUniRow.getInt("repLo"))
        assertEquals(12, rsUniRow.getInt("repHi"))
        assertEquals(90, rsUniRow.getInt("restSec"))
        assertEquals(2.5, rsUniRow.getDouble("incrementKg"), 0.001)
        assertEquals(0, rsUniRow.getInt("isCompound"))
        assertEquals(0.0, rsUniRow.getDouble("bwFactor"), 0.001)
        assertEquals("Upper Back", rsUniRow.getString("primaryMuscles"))
        assertEquals("Lats, Biceps", rsUniRow.getString("secondaryMuscles"))
        assertTrue(rsUniRow.getString("setupNote").contains("Chest on pad"))
        rsUniRow.close()

        // 7. "Lat pullover" -> "Lat Pullover" (id 23)
        val rsPo = conn.createStatement().executeQuery("SELECT * FROM exercises WHERE id = 23")
        assertTrue(rsPo.next())
        assertEquals("Lat Pullover", rsPo.getString("name"))
        assertEquals("machine", rsPo.getString("equipment"))
        assertEquals(3, rsPo.getInt("defaultSets"))
        assertEquals(8, rsPo.getInt("repLo"))
        assertEquals(12, rsPo.getInt("repHi"))
        assertEquals(90, rsPo.getInt("restSec"))
        assertEquals(2.5, rsPo.getDouble("incrementKg"), 0.001)
        assertEquals(0, rsPo.getInt("isCompound"))
        assertEquals("Lats", rsPo.getString("primaryMuscles"))
        assertTrue(rsPo.getString("setupNote").contains("Slight elbow bend"))
        rsPo.close()

        // 5. Verify data integrity: historical sets linked to IDs 23, 24, 25 survive
        val rsSets = conn.createStatement().executeQuery("SELECT id, exerciseId, weightKg FROM set_entries WHERE id IN (50, 51, 52) ORDER BY id")
        assertTrue(rsSets.next())
        assertEquals(23L, rsSets.getLong("exerciseId"))
        assertEquals(40.0, rsSets.getDouble("weightKg"), 0.001)
        assertTrue(rsSets.next())
        assertEquals(24L, rsSets.getLong("exerciseId"))
        assertEquals(30.0, rsSets.getDouble("weightKg"), 0.001)
        assertTrue(rsSets.next())
        assertEquals(25L, rsSets.getLong("exerciseId"))
        assertEquals(10.0, rsSets.getDouble("weightKg"), 0.001)
        rsSets.close()
    }

    private fun createSupportSQLiteDatabaseProxy(c: Connection): SupportSQLiteDatabase {
        val handler = InvocationHandler { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    val sql = args[0] as String
                    if (args.size > 1 && args[1] != null) {
                        val bindArgs = args[1] as Array<*>
                        val pstmt = c.prepareStatement(sql)
                        bindArgs.forEachIndexed { idx, arg ->
                            when (arg) {
                                null -> pstmt.setNull(idx + 1, java.sql.Types.NULL)
                                is String -> pstmt.setString(idx + 1, arg)
                                is Int -> pstmt.setInt(idx + 1, arg)
                                is Long -> pstmt.setLong(idx + 1, arg)
                                is Double -> pstmt.setDouble(idx + 1, arg)
                                is Float -> pstmt.setFloat(idx + 1, arg)
                                else -> pstmt.setObject(idx + 1, arg)
                            }
                        }
                        pstmt.executeUpdate()
                        pstmt.close()
                    } else {
                        val s = c.createStatement()
                        s.executeUpdate(sql)
                        s.close()
                    }
                    null
                }
                "query" -> {
                    val sql = when {
                        args[0] is String -> args[0] as String
                        args[0] is SupportSQLiteQuery -> (args[0] as SupportSQLiteQuery).sql
                        else -> args[0].toString()
                    }
                    val bindArgs = if (args.size > 1 && args[1] is Array<*>) args[1] as Array<*> else null
                    val rs = if (bindArgs != null) {
                        val pstmt = c.prepareStatement(sql)
                        bindArgs.forEachIndexed { idx, arg ->
                            when (arg) {
                                null -> pstmt.setNull(idx + 1, java.sql.Types.NULL)
                                is String -> pstmt.setString(idx + 1, arg)
                                is Int -> pstmt.setInt(idx + 1, arg)
                                is Long -> pstmt.setLong(idx + 1, arg)
                                is Double -> pstmt.setDouble(idx + 1, arg)
                                else -> pstmt.setObject(idx + 1, arg)
                            }
                        }
                        pstmt.executeQuery()
                    } else {
                        val s = c.createStatement()
                        s.executeQuery(sql)
                    }
                    createCursorProxy(rs)
                }
                else -> null
            }
        }
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            handler
        ) as SupportSQLiteDatabase
    }

    private fun createCursorProxy(rs: ResultSet): Cursor {
        val md: ResultSetMetaData = rs.metaData
        val colCount = md.columnCount
        val colNames = (1..colCount).map { md.getColumnLabel(it) }

        val handler = InvocationHandler { _, method, args ->
            when (method.name) {
                "moveToFirst" -> rs.next()
                "moveToNext" -> rs.next()
                "close" -> rs.close()
                "isClosed" -> rs.isClosed
                "getLong" -> rs.getLong((args[0] as Int) + 1)
                "getInt" -> rs.getInt((args[0] as Int) + 1)
                "getString" -> rs.getString((args[0] as Int) + 1)
                "getDouble" -> rs.getDouble((args[0] as Int) + 1)
                "getColumnIndex" -> {
                    val name = args[0] as String
                    colNames.indexOfFirst { it.equals(name, ignoreCase = true) }
                }
                else -> null
            }
        }
        return Proxy.newProxyInstance(
            Cursor::class.java.classLoader,
            arrayOf(Cursor::class.java),
            handler
        ) as Cursor
    }
}
