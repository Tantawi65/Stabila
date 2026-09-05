package com.stabila.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.stabila.app.navigation.Screen

// Colors matching the React design for nav bar
private val BgCard = Color(0xFFFFFFFF)
private val BorderColor = Color(0xFFE5E7EB)
private val PrimaryColor = Color(0xFF2E4B6B)
private val PrimaryLight = PrimaryColor.copy(alpha = 0.12f)
private val MutedForeground = Color(0xFF6B7280)

private data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun StabilaBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(Screen.Home.route, "Home", Icons.Default.Home),
        NavItem(Screen.Camera.route, "Camera", Icons.Default.CameraAlt),
        NavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Ensure we only show on the main top-level routes
    val topLevelRoutes = listOf(Screen.Home.route, Screen.DailyTest.route, Screen.History.route, Screen.Settings.route)
    if (currentRoute !in topLevelRoutes && currentRoute != Screen.Camera.route) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(BgCard)
            .drawBehind {
                drawLine(
                    color = BorderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            // In the React design, if we are not on Camera or Settings exactly, Home should appear active if it's the root.
            // But since Stabila uses DailyTest/History in Home, we'll keep Home active if on those.
            val isActive = isSelected || (item.route == Screen.Home.route && currentRoute in listOf(Screen.DailyTest.route, Screen.History.route))
            
            val iconColor = if (isActive) PrimaryColor else MutedForeground

            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Background pill for active item
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(width = 56.dp, height = 36.dp)
                            .background(PrimaryLight, RoundedCornerShape(18.dp))
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = iconColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
