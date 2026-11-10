package com.ipvio.stream.mobile

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Wide tablets (and wide windows): list and content side by side. Follows the window, so rotating or resizing switches live. */
@Composable
internal fun twoPane(): Boolean = MobileLogic.twoPane(LocalConfiguration.current.screenWidthDp)

/** Width of the list pane next to the content. */
@Composable
internal fun listPaneWidth(): Dp = MobileLogic.listPaneDp(LocalConfiguration.current.screenWidthDp).dp
