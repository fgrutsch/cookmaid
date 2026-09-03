package io.github.fgrutsch.cookmaid.ui.common

import androidx.compose.ui.unit.dp

/**
 * Horizontal inset for a screen's list content.
 *
 * Shared because the three list screens sit next to each other in the navigation bar, so any
 * difference shows up as the content shifting sideways when you switch tabs. They had drifted to
 * 12dp on two screens and 16dp elsewhere.
 */
val LIST_HORIZONTAL_PADDING = 16.dp
