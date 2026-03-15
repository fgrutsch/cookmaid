package io.github.fgrutsch.ui.shopping

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
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
import io.github.fgrutsch.catalog.Item
import io.github.fgrutsch.shopping.ShoppingItem
import io.github.fgrutsch.ui.common.SwipeToDeleteItem
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::onEvent

    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showNewListDialog by remember { mutableStateOf(false) }
    var editingList by remember { mutableStateOf<Pair<Uuid, String>?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onEvent(ShoppingListEvent.LoadLists)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "List options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("New list") },
                            onClick = {
                                showMenu = false
                                showNewListDialog = true
                            },
                        )
                        val currentList = state.selectedList
                        if (currentList != null) {
                            DropdownMenuItem(
                                text = { Text("Rename list") },
                                onClick = {
                                    showMenu = false
                                    editingList = currentList.id to currentList.name
                                },
                            )
                            if (!currentList.default && state.lists.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("Delete list") },
                                    onClick = {
                                        showMenu = false
                                        onEvent(ShoppingListEvent.DeleteList(currentList.id))
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.lists.forEach { list ->
                    FilterChip(
                        selected = list.id == state.selectedListId,
                        onClick = { onEvent(ShoppingListEvent.SelectList(list.id)) },
                        label = { Text(list.name) },
                    )
                }
            }

            AddItemField(
                query = state.searchQuery,
                suggestions = state.suggestions,
                onQueryChange = { onEvent(ShoppingListEvent.UpdateSearchQuery(it)) },
                onAddFreeText = {
                    if (state.searchQuery.isNotBlank()) {
                        onEvent(ShoppingListEvent.AddItem(Item.FreeTextItem(name = state.searchQuery.trim())))
                    }
                },
                onAddCatalogItem = { onEvent(ShoppingListEvent.AddItem(it)) },
            )

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { onEvent(ShoppingListEvent.LoadLists) },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (!state.isLoading && state.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No items yet", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Type above to add items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        val grouped = state.uncheckedItems.groupBy { item ->
                            (item.item as? Item.CatalogItem)?.category?.name ?: "Uncategorized"
                        }.entries.sortedBy { entry -> entry.key }

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
                                SwipeToDeleteItem(
                                    onDelete = { onEvent(ShoppingListEvent.DeleteItem(shoppingItem.id)) },
                                ) {
                                    ShoppingItemRow(
                                        item = shoppingItem,
                                        onToggle = { onEvent(ShoppingListEvent.ToggleChecked(shoppingItem.id)) },
                                        onEdit = { editingItem = shoppingItem },
                                    )
                                }
                            }
                        }

                        if (state.checkedItems.isNotEmpty()) {
                            item(key = "checked-divider") {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            }
                            item(key = "checked-header") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Checked (${state.checkedItems.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                    IconButton(onClick = { onEvent(ShoppingListEvent.DeleteChecked) }) {
                                        Icon(
                                            Icons.Default.DeleteSweep,
                                            contentDescription = "Delete checked",
                                            tint = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            }
                            items(state.checkedItems, key = { it.id }) { item ->
                                SwipeToDeleteItem(
                                    onDelete = { onEvent(ShoppingListEvent.DeleteItem(item.id)) },
                                ) {
                                    ShoppingItemRow(
                                        item = item,
                                        onToggle = { onEvent(ShoppingListEvent.ToggleChecked(item.id)) },
                                        onEdit = { editingItem = item },
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
            title = "New List",
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
            title = "Rename List",
            initialName = listName,
            onDismiss = { editingList = null },
            onConfirm = { newName ->
                onEvent(ShoppingListEvent.RenameList(listId, newName))
                editingList = null
            },
        )
    }
}
