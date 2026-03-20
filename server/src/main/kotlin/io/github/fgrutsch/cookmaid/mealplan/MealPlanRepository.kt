package io.github.fgrutsch.cookmaid.mealplan

import io.github.fgrutsch.cookmaid.recipe.RecipesTable
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid

interface MealPlanRepository {
    suspend fun findByUserAndDateRange(userId: Uuid, from: LocalDate, to: LocalDate): List<MealPlanItem>
    suspend fun create(userId: Uuid, day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItem
    suspend fun update(id: Uuid, day: LocalDate?, note: String?)
    suspend fun delete(id: Uuid)
    suspend fun isOwnedByUser(userId: Uuid, itemId: Uuid): Boolean
}

class PostgresMealPlanRepository : MealPlanRepository {

    private val joined = MealPlanItemsTable
        .join(RecipesTable, JoinType.LEFT, MealPlanItemsTable.recipeId, RecipesTable.id)

    override suspend fun findByUserAndDateRange(
        userId: Uuid,
        from: LocalDate,
        to: LocalDate,
    ): List<MealPlanItem> = suspendTransaction {
        joined.selectAll()
            .where {
                (MealPlanItemsTable.userId eq userId) and
                    (MealPlanItemsTable.day greaterEq from) and
                    (MealPlanItemsTable.day lessEq to)
            }
            .orderBy(MealPlanItemsTable.id, SortOrder.ASC)
            .map { it.toMealPlanItem() }
    }

    override suspend fun create(
        userId: Uuid,
        day: LocalDate,
        recipeId: Uuid?,
        note: String?,
    ): MealPlanItem = suspendTransaction {
        val row = MealPlanItemsTable.insertReturning {
            it[MealPlanItemsTable.userId] = userId
            it[MealPlanItemsTable.day] = day
            it[MealPlanItemsTable.recipeId] = recipeId
            it[MealPlanItemsTable.note] = note?.trim()
        }.single()

        val id = row[MealPlanItemsTable.id]
        joined.selectAll()
            .where { MealPlanItemsTable.id eq id }
            .single()
            .toMealPlanItem()
    }

    override suspend fun update(id: Uuid, day: LocalDate?, note: String?): Unit = suspendTransaction {
        MealPlanItemsTable.update({ MealPlanItemsTable.id eq id }) {
            if (day != null) it[MealPlanItemsTable.day] = day
            if (note != null) it[MealPlanItemsTable.note] = note.trim()
        }
    }

    override suspend fun delete(id: Uuid): Unit = suspendTransaction {
        MealPlanItemsTable.deleteWhere { MealPlanItemsTable.id eq id }
    }

    override suspend fun isOwnedByUser(userId: Uuid, itemId: Uuid): Boolean = suspendTransaction {
        MealPlanItemsTable.selectAll()
            .where { (MealPlanItemsTable.id eq itemId) and (MealPlanItemsTable.userId eq userId) }
            .count() > 0
    }

    private fun ResultRow.toMealPlanItem(): MealPlanItem {
        val recipeId = this[MealPlanItemsTable.recipeId]
        return if (recipeId != null) {
            MealPlanItem.Recipe(
                id = this[MealPlanItemsTable.id],
                day = this[MealPlanItemsTable.day],
                recipeId = recipeId,
                recipeName = this[RecipesTable.name],
            )
        } else {
            MealPlanItem.Note(
                id = this[MealPlanItemsTable.id],
                day = this[MealPlanItemsTable.day],
                name = requireNotNull(this[MealPlanItemsTable.note]),
            )
        }
    }
}

object MealPlanItemsTable : Table("meal_plan_items") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val day = date("day")
    val recipeId = uuid("recipe_id").references(RecipesTable.id).nullable()
    val note = text("note").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
