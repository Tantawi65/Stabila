package com.stabila.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.res.stringResource
import com.stabila.app.R
import com.stabila.app.navigation.Screen
import com.stabila.core.ui.LocalAdaptiveParams

private data class NavItem(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector
)

@Composable
fun StabilaBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(Screen.Home.route, R.string.nav_home, Icons.Default.Home),
        NavItem(Screen.DailyTest.route, R.string.nav_daily_test, Icons.Default.MonitorHeart),
        NavItem(Screen.History.route, R.string.nav_history, Icons.Default.History),
        NavItem(Screen.Settings.route, R.string.nav_settings, Icons.Default.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val adaptive = LocalAdaptiveParams.current

    // Hide bottom bar on setup/detail screens if appropriate, or show across top destinations
    val topLevelRoutes = items.map { it.route }
    if (currentRoute !in topLevelRoutes) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(100.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(RoundedCornerShape(100.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val title = stringResource(item.titleRes)

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                    animationSpec = tween(250),
                    label = "nav_bg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(250),
                    label = "nav_fg"
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(backgroundColor)
                        .clickable {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    if (isSelected) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = (13 * adaptive.fontScale).sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
