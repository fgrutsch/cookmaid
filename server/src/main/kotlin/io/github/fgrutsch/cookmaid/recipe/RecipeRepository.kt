package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.catalog.CatalogItemsTable
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.catalog.ItemCategoriesTable
import io.github.fgrutsch.cookmaid.catalog.ItemCategory
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.LessOp
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.QueryParameter
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.Uuid

interface RecipeRepository {
    suspend fun findByUserId(userId: Uuid, cursor: Instant?, limit: Int, search: String?, tag: String?): RecipePage
    suspend fun findById(id: Uuid): Recipe?
    suspend fun findTagsByUserId(userId: Uuid): List<String>
    suspend fun create(userId: Uuid, data: RecipeData): Recipe
    suspend fun update(id: Uuid, data: RecipeData)

    suspend fun delete(id: Uuid)
    suspend fun isOwnedByUser(userId: Uuid, recipeId: Uuid): Boolean
}

class PostgresRecipeRepository : RecipeRepository {

    override suspend fun findByUserId(
        userId: Uuid,
        cursor: Instant?,
        limit: Int,
        search: String?,
        tag: String?,
    ): RecipePage = suspendTransaction {
        var condition: Op<Boolean> = RecipesTable.userId eq userId

        if (cursor != null) {
            val cursorCondition = LessOp(
                RecipesTable.createdAt,
                QueryParameter(cursor, RecipesTable.createdAt.columnType),
            )
            condition = condition and cursorCondition
        }

        if (!search.isNullOrBlank()) {
            val escaped = search.trim().lowercase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            condition = condition and (RecipesTable.name.lowerCase() like "%$escaped%")
        }

        if (!tag.isNullOrBlank()) {
            condition = condition and object : Op<Boolean>() {
                override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                    queryBuilder {
                        append(RecipesTable.tags)
                        append(" @> ARRAY[")
                        registerArgument(TextColumnType(), tag.trim())
                        append("]::text[]")
                    }
                }
            }
        }

        val query = RecipesTable.selectAll().where(condition)

        val rows = query
            .orderBy(RecipesTable.createdAt, SortOrder.DESC)
            .limit(limit + 1)
            .toList()

        val hasMore = rows.size > limit
        val pageRows = rows.take(limit)

        val items = pageRows.map { row ->
            val id = row[RecipesTable.id]
            Recipe(
                id = id,
                name = row[RecipesTable.name],
                description = row[RecipesTable.description],
                ingredients = loadIngredients(id),
                steps = row[RecipesTable.steps],
                tags = row[RecipesTable.tags],
            )
        }

        val nextCursor = if (hasMore) pageRows.last()[RecipesTable.createdAt].toEpochMilliseconds().toString() else null
        RecipePage(items = items, nextCursor = nextCursor)
    }

    override suspend fun findById(id: Uuid): Recipe? = suspendTransaction {
        val row = RecipesTable.selectAll()
            .where(RecipesTable.id eq id)
            .singleOrNull() ?: return@suspendTransaction null

        Recipe(
            id = row[RecipesTable.id],
            name = row[RecipesTable.name],
            description = row[RecipesTable.description],
            ingredients = loadIngredients(id),
            steps = row[RecipesTable.steps],
            tags = row[RecipesTable.tags],
        )
    }

    override suspend fun findTagsByUserId(userId: Uuid): List<String> = suspendTransaction {
        RecipesTable.selectAll()
            .where(RecipesTable.userId eq userId)
            .flatMap { it[RecipesTable.tags] }
            .distinct()
            .sorted()
    }

    override suspend fun create(userId: Uuid, data: RecipeData): Recipe = suspendTransaction {
        val row = RecipesTable.insertReturning {
            it[RecipesTable.userId] = userId
            it[RecipesTable.name] = data.name.trim()
            it[RecipesTable.description] = data.description?.trim()?.ifBlank { null }
            it[RecipesTable.steps] = data.steps.map(String::trim)
            it[RecipesTable.tags] = data.tags.map(String::trim)
        }.single()

        val recipeId = row[RecipesTable.id]
        insertIngredients(recipeId, data.ingredients)

        Recipe(
            id = recipeId,
            name = row[RecipesTable.name],
            description = row[RecipesTable.description],
            ingredients = loadIngredients(recipeId),
            steps = row[RecipesTable.steps],
            tags = row[RecipesTable.tags],
        )
    }

    override suspend fun update(id: Uuid, data: RecipeData): Unit = suspendTransaction {
        RecipesTable.update({ RecipesTable.id eq id }) {
            it[RecipesTable.name] = data.name.trim()
            it[RecipesTable.description] = data.description?.trim()?.ifBlank { null }
            it[RecipesTable.steps] = data.steps.map(String::trim)
            it[RecipesTable.tags] = data.tags.map(String::trim)
        }

        RecipeIngredientsTable.deleteWhere { RecipeIngredientsTable.recipeId eq id }
        insertIngredients(id, data.ingredients)
    }

    override suspend fun delete(id: Uuid): Unit = suspendTransaction {
        RecipesTable.deleteWhere { RecipesTable.id eq id }
    }

    override suspend fun isOwnedByUser(userId: Uuid, recipeId: Uuid): Boolean = suspendTransaction {
        RecipesTable.selectAll()
            .where { (RecipesTable.id eq recipeId) and (RecipesTable.userId eq userId) }
            .count() > 0
    }

    private fun loadIngredients(recipeId: Uuid): List<RecipeIngredient> {
        val joined = RecipeIngredientsTable
            .join(CatalogItemsTable, JoinType.LEFT, RecipeIngredientsTable.catalogItemId, CatalogItemsTable.id)
            .join(ItemCategoriesTable, JoinType.LEFT, CatalogItemsTable.categoryId, ItemCategoriesTable.id)

        return joined.selectAll()
            .where { RecipeIngredientsTable.recipeId eq recipeId }
            .map { row ->
                val catalogItemId = row[RecipeIngredientsTable.catalogItemId]
                val item: Item = if (catalogItemId != null) {
                    Item.Catalog(
                        id = catalogItemId,
                        name = row[CatalogItemsTable.name],
                        category = ItemCategory(
                            id = row[ItemCategoriesTable.id],
                            name = row[ItemCategoriesTable.name],
                        ),
                    )
                } else {
                    Item.FreeText(name = requireNotNull(row[RecipeIngredientsTable.freeTextName]))
                }
                RecipeIngredient(
                    item = item,
                    quantity = row[RecipeIngredientsTable.quantity],
                )
            }
    }

    private fun insertIngredients(recipeId: Uuid, ingredients: List<RecipeIngredient>) {
        ingredients.forEach { ingredient ->
            RecipeIngredientsTable.insert {
                it[RecipeIngredientsTable.recipeId] = recipeId
                it[RecipeIngredientsTable.quantity] = ingredient.quantity
                when (val item = ingredient.item) {
                    is Item.Catalog -> {
                        it[RecipeIngredientsTable.catalogItemId] = item.id
                        it[RecipeIngredientsTable.freeTextName] = null
                    }

                    is Item.FreeText -> {
                        it[RecipeIngredientsTable.catalogItemId] = null
                        it[RecipeIngredientsTable.freeTextName] = item.name
                    }
                }
            }
        }
    }
}

object RecipesTable : Table("recipes") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val name = text("name")
    val description = text("description").nullable()
    val steps = array("steps", TextColumnType())
    val tags = array("tags", TextColumnType())
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object RecipeIngredientsTable : Table("recipe_ingredients") {
    val id = uuid("id").autoGenerate()
    val recipeId = uuid("recipe_id").references(RecipesTable.id)
    val catalogItemId = uuid("catalog_item_id").references(CatalogItemsTable.id).nullable()
    val freeTextName = text("free_text_name").nullable()
    val quantity = float("quantity").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
