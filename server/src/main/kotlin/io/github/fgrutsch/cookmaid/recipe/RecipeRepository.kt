package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.catalog.CatalogItemsTable
import io.github.fgrutsch.cookmaid.common.SupportedLocale
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.catalog.ItemCategoriesTable
import io.github.fgrutsch.cookmaid.catalog.ItemCategory
import io.github.fgrutsch.cookmaid.catalog.resolveCategoryName
import io.github.fgrutsch.cookmaid.catalog.resolveItemName
import io.github.fgrutsch.cookmaid.user.UserId
import kotlin.time.Instant
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.LessOp
import org.jetbrains.exposed.v1.core.NeqOp
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.QueryParameter
import org.jetbrains.exposed.v1.core.Random
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
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

/**
 * Persistence layer for recipes and their ingredients.
 */
interface RecipeRepository {
    /**
     * Returns a cursor-paginated page of recipes for [userId], optionally filtered by [search] and [tag].
     *
     * @param userId the owner of the recipes.
     * @param cursor pagination cursor (creation timestamp) for fetching the next page.
     * @param limit maximum number of recipes to return.
     * @param search optional case-insensitive text filter on recipe name.
     * @param tag optional tag filter.
     * @param locale the language code for catalog item names.
     * @return a page of matching recipes with an optional next-page cursor.
     */
    suspend fun find(
        userId: UserId,
        cursor: Instant?,
        limit: Int,
        search: String?,
        tag: String?,
        locale: SupportedLocale,
    ): RecipePage

    /**
     * Returns a single recipe by [id], or null if it does not exist.
     *
     * @param id the recipe identifier.
     * @param locale the language code for catalog item names.
     * @return the matching recipe, or null if not found.
     */
    suspend fun findById(id: Uuid, locale: SupportedLocale): Recipe?

    /**
     * Returns all distinct tags across the user's recipes.
     *
     * @param userId the owner of the recipes.
     * @return a sorted list of unique tag strings.
     */
    suspend fun findTags(userId: UserId): List<String>

    /**
     * Persists a new recipe with its ingredients for [userId].
     *
     * @param userId the owner of the new recipe.
     * @param data the recipe content including ingredients.
     * @param locale the language code for catalog item names.
     * @return the persisted recipe.
     */
    suspend fun create(userId: UserId, data: RecipeData, locale: SupportedLocale): Recipe

    /**
     * Replaces the recipe data (including ingredients) for the given [id].
     *
     * @param id the recipe to update.
     * @param data the new recipe content.
     */
    suspend fun update(id: Uuid, data: RecipeData)

    /**
     * Deletes a recipe and its associated ingredients by [id].
     *
     * @param id the recipe to delete.
     */
    suspend fun delete(id: Uuid)

    /**
     * Returns a random recipe for [userId], optionally filtered by [tag].
     *
     * @param userId the owner of the recipes.
     * @param tag optional tag filter.
     * @param excludeId optional recipe ID to exclude (for avoiding repeats).
     * @param locale the language code for catalog item names.
     * @return a random recipe, or null if no recipes match.
     */
    suspend fun findRandom(userId: UserId, tag: String?, excludeId: Uuid?, locale: SupportedLocale): Recipe?

    /**
     * Returns true if [userId] owns the recipe identified by [recipeId].
     *
     * @param userId the user to check ownership for.
     * @param recipeId the recipe to check.
     * @return true if the user owns the recipe.
     */
    suspend fun isOwner(userId: UserId, recipeId: Uuid): Boolean
}

class PostgresRecipeRepository : RecipeRepository {

    override suspend fun find(
        userId: UserId,
        cursor: Instant?,
        limit: Int,
        search: String?,
        tag: String?,
        locale: SupportedLocale,
    ): RecipePage = suspendTransaction {
        var condition: Op<Boolean> = RecipesTable.userId eq userId.value

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

        val recipeIds = pageRows.map { it[RecipesTable.id] }
        val ingredientsByRecipe = if (recipeIds.isEmpty()) {
            emptyMap()
        } else {
            loadIngredientsBatch(recipeIds, locale)
        }

        val items = pageRows.map { row ->
            val id = row[RecipesTable.id]
            Recipe(
                id = id,
                name = row[RecipesTable.name],
                description = row[RecipesTable.description],
                ingredients = ingredientsByRecipe[id].orEmpty(),
                steps = row[RecipesTable.steps],
                tags = row[RecipesTable.tags],
                servings = row[RecipesTable.servings],
            )
        }

        val nextCursor = if (hasMore) pageRows.last()[RecipesTable.createdAt].toEpochMilliseconds().toString() else null
        RecipePage(items = items, nextCursor = nextCursor)
    }

    override suspend fun findById(id: Uuid, locale: SupportedLocale): Recipe? = suspendTransaction {
        val row = RecipesTable.selectAll()
            .where(RecipesTable.id eq id)
            .singleOrNull() ?: return@suspendTransaction null

        Recipe(
            id = row[RecipesTable.id],
            name = row[RecipesTable.name],
            description = row[RecipesTable.description],
            ingredients = loadIngredients(id, locale),
            steps = row[RecipesTable.steps],
            tags = row[RecipesTable.tags],
            servings = row[RecipesTable.servings],
        )
    }

    override suspend fun findTags(userId: UserId): List<String> = suspendTransaction {
        RecipesTable.selectAll()
            .where(RecipesTable.userId eq userId.value)
            .flatMap { it[RecipesTable.tags] }
            .distinct()
            .sorted()
    }

    override suspend fun create(
        userId: UserId,
        data: RecipeData,
        locale: SupportedLocale,
    ): Recipe = suspendTransaction {
        val row = RecipesTable.insertReturning {
            it[RecipesTable.userId] = userId.value
            it[RecipesTable.name] = data.name.trim()
            it[RecipesTable.description] = data.description?.trim()?.ifBlank { null }
            it[RecipesTable.steps] = data.steps.map(String::trim)
            it[RecipesTable.tags] = data.tags.map(String::trim)
            it[RecipesTable.servings] = data.servings
        }.single()

        val recipeId = row[RecipesTable.id]
        insertIngredients(recipeId, data.ingredients)

        Recipe(
            id = recipeId,
            name = row[RecipesTable.name],
            description = row[RecipesTable.description],
            ingredients = data.ingredients,
            steps = row[RecipesTable.steps],
            tags = row[RecipesTable.tags],
            servings = row[RecipesTable.servings],
        )
    }

    override suspend fun update(id: Uuid, data: RecipeData): Unit = suspendTransaction {
        RecipesTable.update({ RecipesTable.id eq id }) {
            it[RecipesTable.name] = data.name.trim()
            it[RecipesTable.description] = data.description?.trim()?.ifBlank { null }
            it[RecipesTable.steps] = data.steps.map(String::trim)
            it[RecipesTable.tags] = data.tags.map(String::trim)
            it[RecipesTable.servings] = data.servings
        }

        RecipeIngredientsTable.deleteWhere { RecipeIngredientsTable.recipeId eq id }
        insertIngredients(id, data.ingredients)
    }

    override suspend fun delete(id: Uuid): Unit = suspendTransaction {
        RecipesTable.deleteWhere { RecipesTable.id eq id }
    }

    override suspend fun isOwner(userId: UserId, recipeId: Uuid): Boolean = suspendTransaction {
        RecipesTable.selectAll()
            .where { (RecipesTable.id eq recipeId) and (RecipesTable.userId eq userId.value) }
            .count() > 0
    }

    override suspend fun findRandom(
        userId: UserId,
        tag: String?,
        excludeId: Uuid?,
        locale: SupportedLocale,
    ): Recipe? = suspendTransaction {
        fun queryRandom(withExclusion: Boolean): Recipe? {
            var condition: Op<Boolean> = RecipesTable.userId eq userId.value

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

            if (withExclusion && excludeId != null) {
                condition = condition and NeqOp(
                    RecipesTable.id,
                    QueryParameter(excludeId, RecipesTable.id.columnType),
                )
            }

            val row = RecipesTable.selectAll()
                .where(condition)
                .orderBy(Random())
                .limit(1)
                .singleOrNull() ?: return null

            val id = row[RecipesTable.id]
            return Recipe(
                id = id,
                name = row[RecipesTable.name],
                description = row[RecipesTable.description],
                ingredients = loadIngredients(id, locale),
                steps = row[RecipesTable.steps],
                tags = row[RecipesTable.tags],
                servings = row[RecipesTable.servings],
            )
        }

        queryRandom(withExclusion = true)
            ?: if (excludeId != null) queryRandom(withExclusion = false) else null
    }

    private fun loadIngredients(recipeId: Uuid, locale: SupportedLocale): List<RecipeIngredient> {
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
                        name = resolveItemName(row, locale),
                        category = ItemCategory(
                            id = row[ItemCategoriesTable.id],
                            name = resolveCategoryName(row, locale),
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

    private fun loadIngredientsBatch(
        recipeIds: List<Uuid>,
        locale: SupportedLocale,
    ): Map<Uuid, List<RecipeIngredient>> {
        val joined = RecipeIngredientsTable
            .join(CatalogItemsTable, JoinType.LEFT, RecipeIngredientsTable.catalogItemId, CatalogItemsTable.id)
            .join(ItemCategoriesTable, JoinType.LEFT, CatalogItemsTable.categoryId, ItemCategoriesTable.id)

        return joined.selectAll()
            .where { RecipeIngredientsTable.recipeId inList recipeIds }
            .groupBy { it[RecipeIngredientsTable.recipeId] }
            .mapValues { (_, rows) ->
                rows.map { row ->
                    val catalogItemId = row[RecipeIngredientsTable.catalogItemId]
                    val item: Item = if (catalogItemId != null) {
                        Item.Catalog(
                            id = catalogItemId,
                            name = resolveItemName(row, locale),
                            category = ItemCategory(
                                id = row[ItemCategoriesTable.id],
                                name = resolveCategoryName(row, locale),
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
    val servings = integer("servings").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object RecipeIngredientsTable : Table("recipe_ingredients") {
    val id = uuid("id").autoGenerate()
    val recipeId = uuid("recipe_id").references(RecipesTable.id)
    val catalogItemId = uuid("catalog_item_id").references(CatalogItemsTable.id).nullable()
    val freeTextName = text("free_text_name").nullable()
    val quantity = text("quantity").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
