package io.github.fgrutsch.cookmaid.ui.shopping

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.common_uncategorized
import cookmaid.composeapp.generated.resources.ic_delete_sweep
import cookmaid.composeapp.generated.resources.ic_more_vert
import cookmaid.composeapp.generated.resources.shopping_checked_count
import cookmaid.composeapp.generated.resources.shopping_delete_checked
import cookmaid.composeapp.generated.resources.shopping_delete_list
import cookmaid.composeapp.generated.resources.shopping_empty_subtitle
import cookmaid.composeapp.generated.resources.shopping_empty_title
import cookmaid.composeapp.generated.resources.shopping_list_options
import cookmaid.composeapp.generated.resources.shopping_new_list
import cookmaid.composeapp.generated.resources.shopping_rename_list
import cookmaid.composeapp.generated.resources.shopping_title
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import io.github.fgrutsch.cookmaid.ui.common.SwipeItem
import io.github.fgrutsch.cookmaid.ui.common.resolve
import org.jetbrains.compose.resources.painterResource
import kotlin.uuid.Uuid

/**
 * Shopping list screen with categorized items, search, add, check/uncheck,
 * and swipe-to-delete/edit.
 *
 * @param viewModel the shopping list view model.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun ShoppingListScreen(viewModel: ShoppingListViewModel) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::onEvent

    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showNewListDialog by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf<Pair<Uuid, String>?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        onEvent(ShoppingListEvent.LoadLists)
        viewModel.effects.collect { effect ->
            when (effect) {
                is ShoppingListEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ShoppingListTopBar(
                showMenu = showMenu,
                selectedList = state.selectedList,
                listCount = state.lists.size,
                onShowMenu = { showMenu = true },
                onDismissMenu = { showMenu = false },
                onNewList = {
                    showMenu = false
                    showNewListDialog = true
                },
                onRenameList = { id, name ->
                    showMenu = false
                    editingList = id to name
                },
                onDeleteList = { id ->
                    showMenu = false
                    onEvent(ShoppingListEvent.DeleteList(id))
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            ListSelectorChips(
                lists = state.lists,
                selectedListId = state.selectedListId,
                onSelectList = { onEvent(ShoppingListEvent.SelectList(it)) },
            )

            AddItemField(
                query = state.searchQuery,
                suggestions = state.suggestions,
                onQueryChange = { onEvent(ShoppingListEvent.UpdateSearchQuery(it)) },
                onAddFreeText = {
                    if (state.searchQuery.isNotBlank()) {
                        onEvent(ShoppingListEvent.AddItemByName(state.searchQuery))
                    }
                },
                onAddCatalogItem = { onEvent(ShoppingListEvent.AddItem(it)) },
            )

            ShoppingItemList(
                state = state,
                onRefresh = { onEvent(ShoppingListEvent.Refresh) },
                onToggleChecked = { onEvent(ShoppingListEvent.ToggleChecked(it)) },
                onDeleteItem = { onEvent(ShoppingListEvent.DeleteItem(it)) },
                onEditItem = { editingItem = it },
                onDeleteChecked = { onEvent(ShoppingListEvent.DeleteChecked) },
            )
        }
    }

    editingItem?.let { item ->
        EditItemDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                onEvent(ShoppingListEvent.UpdateItem(updated))
                editingItem = null
            },
        )
    }

    if (showNewListDialog) {
        ListNameDialog(
            title = Res.string.shopping_new_list.resolve(),
            initialName = "",
            onDismiss = { showNewListDialog = false },
            onConfirm = { name ->
                onEvent(ShoppingListEvent.CreateList(name))
                showNewListDialog = false
            },
        )
    }

    editingList?.let { (listId, listName) ->
        ListNameDialog(
            title = Res.string.shopping_rename_list.resolve(),
            initialName = listName,
            onDismiss = { editingList = null },
            onConfirm = { newName ->
                onEvent(ShoppingListEvent.RenameList(listId, newName))
                editingList = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingListTopBar(
    showMenu: Boolean,
    selectedList: ShoppingList?,
    listCount: Int,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onNewList: () -> Unit,
    onRenameList: (Uuid, String) -> Unit,
    onDeleteList: (Uuid) -> Unit,
) {
    TopAppBar(
        title = { Text(Res.string.shopping_title.resolve()) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        actions = {
            IconButton(onClick = onShowMenu) {
                Icon(
                    painterResource(Res.drawable.ic_more_vert),
                    contentDescription = Res.string.shopping_list_options.resolve(),
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissMenu,
            ) {
                DropdownMenuItem(text = { Text(Res.string.shopping_new_list.resolve()) }, onClick = onNewList)
                if (selectedList != null) {
                    DropdownMenuItem(
                        text = { Text(Res.string.shopping_rename_list.resolve()) },
                        onClick = { onRenameList(selectedList.id, selectedList.name) },
                    )
                    if (!selectedList.default && listCount > 1) {
                        DropdownMenuItem(
                            text = { Text(Res.string.shopping_delete_list.resolve()) },
                            onClick = { onDeleteList(selectedList.id) },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ListSelectorChips(
    lists: List<ShoppingList>,
    selectedListId: Uuid?,
    onSelectList: (Uuid?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        lists.forEach { list ->
            FilterChip(
                selected = list.id == selectedListId,
                onClick = { onSelectList(list.id) },
                label = { Text(list.name) },
            )
        }
    }
}

@Composable
private fun ShoppingItemList(
    state: ShoppingListState,
    onRefresh: () -> Unit,
    onToggleChecked: (Uuid) -> Unit,
    onDeleteItem: (Uuid) -> Unit,
    onEditItem: (ShoppingItem) -> Unit,
    onDeleteChecked: () -> Unit,
) {
    val uncategorizedLabel = Res.string.common_uncategorized.resolve()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (!state.isLoading && state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Res.string.shopping_empty_title.resolve(), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        Res.string.shopping_empty_subtitle.resolve(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                uncheckedItemsSection(
                    uncheckedItems = state.uncheckedItems,
                    uncategorizedLabel = uncategorizedLabel,
                    onToggleChecked = onToggleChecked,
                    onDeleteItem = onDeleteItem,
                    onEditItem = onEditItem,
                )
                checkedItemsSection(state.checkedItems, onToggleChecked, onDeleteItem, onEditItem, onDeleteChecked)
            }
        }
    }
}

private fun LazyListScope.uncheckedItemsSection(
    uncheckedItems: List<ShoppingItem>,
    uncategorizedLabel: String,
    onToggleChecked: (Uuid) -> Unit,
    onDeleteItem: (Uuid) -> Unit,
    onEditItem: (ShoppingItem) -> Unit,
) {
    val grouped = uncheckedItems.groupBy { item ->
        (item.item as? Item.Catalog)?.category?.name ?: uncategorizedLabel
    }.entries.sortedBy { it.key }
    grouped.forEach { (categoryName, categoryItems) ->
        item(key = "header-$categoryName") {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            )
        }
        items(categoryItems, key = { it.id }) { shoppingItem ->
            SwipeItem(onDelete = { onDeleteItem(shoppingItem.id) }, onEdit = { onEditItem(shoppingItem) }) {
                ShoppingItemRow(item = shoppingItem, onToggle = { onToggleChecked(shoppingItem.id) })
            }
        }
    }
}

private fun LazyListScope.checkedItemsSection(
    checkedItems: List<ShoppingItem>,
    onToggleChecked: (Uuid) -> Unit,
    onDeleteItem: (Uuid) -> Unit,
    onEditItem: (ShoppingItem) -> Unit,
    onDeleteChecked: () -> Unit,
) {
    if (checkedItems.isEmpty()) return
    item(key = "checked-divider") {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
    item(key = "checked-header") {
        CheckedSectionHeader(checkedCount = checkedItems.size, onDeleteChecked = onDeleteChecked)
    }
    items(checkedItems, key = { it.id }) { item ->
        SwipeItem(onDelete = { onDeleteItem(item.id) }, onEdit = { onEditItem(item) }) {
            ShoppingItemRow(item = item, onToggle = { onToggleChecked(item.id) })
        }
    }
}

@Composable
private fun CheckedSectionHeader(checkedCount: Int, onDeleteChecked: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = Res.string.shopping_checked_count.resolve(checkedCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
        )
        IconButton(onClick = onDeleteChecked) {
            Icon(
                painterResource(Res.drawable.ic_delete_sweep),
                contentDescription = Res.string.shopping_delete_checked.resolve(),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
