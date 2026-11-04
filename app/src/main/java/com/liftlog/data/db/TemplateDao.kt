package com.liftlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.liftlog.data.entity.Template
import com.liftlog.data.entity.TemplateSlot
import kotlinx.coroutines.flow.Flow

data class TemplateWithSlots(
    val template: Template,
    val slots: List<TemplateSlot>
)

@Dao
interface TemplateDao {

    @Query("SELECT * FROM templates ORDER BY orderIndex ASC")
    fun getAllTemplates(): Flow<List<Template>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): Template?

    @Query("SELECT * FROM template_slots WHERE templateId = :templateId ORDER BY slotOrder ASC")
    fun getSlotsForTemplate(templateId: Long): Flow<List<TemplateSlot>>

    @Query("SELECT * FROM template_slots WHERE templateId = :templateId ORDER BY slotOrder ASC")
    suspend fun getSlotsForTemplateSync(templateId: Long): List<TemplateSlot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: Template): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<Template>): List<Long>

    @Update
    suspend fun updateTemplate(template: Template)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TemplateSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<TemplateSlot>): List<Long>

    @Query("DELETE FROM template_slots WHERE templateId = :templateId")
    suspend fun deleteSlotsForTemplate(templateId: Long)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun getCount(): Int
}
