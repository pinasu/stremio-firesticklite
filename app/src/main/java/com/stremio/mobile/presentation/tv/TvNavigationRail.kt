package com.stremio.mobile.presentation.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stremio.mobile.core.theme.AccentPurple
import com.stremio.mobile.core.theme.MutedText
import com.stremio.mobile.presentation.navigation.AppView

/** Width reserved on the left of the board for the TV navigation rail. */
val TvRailWidth = 96.dp

/** Number of poster columns used by the Discover / Library / Search grids on TV. */
const val TvGridColumns = 6

/**
 * Left navigation rail used on TV instead of the phone bottom bar. It is always one D-pad "left"
 * press away from the content, which is how navigation works on Fire TV / Android TV launchers.
 * It exposes the same destinations as [com.stremio.mobile.presentation.components.StremioBottomBar]
 * plus a Search shortcut.
 */
@Composable
fun TvNavigationRail(
    selectedView: AppView,
    onSelect: (AppView) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(TvRailWidth)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xF20B0A14), Color(0xCC0B0A14)),
                ),
            )
            .padding(vertical = 24.dp)
            .focusGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        TvRailItem(
            icon = Icons.Outlined.Search,
            label = "Search",
            selected = false,
            onClick = onOpenSearch,
        )
        AppView.entries.forEach { view ->
            TvRailItem(
                icon = view.icon,
                label = view.label,
                selected = view == selectedView,
                onClick = { onSelect(view) },
            )
        }
    }
}

@Composable
private fun TvRailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val contentColor = if (selected) Color.White else MutedText
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(shape)
            .background(if (selected) AccentPurple.copy(alpha = 0.38f) else Color.Transparent)
            // Focus highlight comes from TvFocusIndication (LocalIndication on TV).
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color.White else contentColor,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
