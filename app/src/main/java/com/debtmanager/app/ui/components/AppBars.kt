@file:OptIn(ExperimentalMaterial3Api::class)

package com.debtmanager.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.debtmanager.app.R
import com.debtmanager.app.ui.navigation.BottomNavItem
import com.debtmanager.app.ui.navigation.Screen
import com.debtmanager.app.ui.navigation.bottomNavItems
import com.debtmanager.app.util.ItemIcons
import com.debtmanager.app.viewmodel.MainViewModel

@Composable
fun MainTopBar(
    navItem: BottomNavItem,
    viewModel: MainViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val userName by viewModel.userName.collectAsState()
    val userIcon by viewModel.userIcon.collectAsState()

    Surface(
        modifier = modifier.fillMaxWidth().shadow(4.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                navItem.selectedIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            navItem.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "سلام $userName",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TopBarAction(
                        onClick = { navController.navigate(Screen.Profile.route) },
                        content = {
                            if (userIcon == "avatar_brand") {
                                Image(
                                    painter = painterResource(R.drawable.brand_logo),
                                    contentDescription = "پروفایل",
                                    modifier = Modifier.size(28.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(ItemIcons.resolve(userIcon), "پروفایل", tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                    )
                    TopBarAction(onClick = { navController.navigate(Screen.History.route) }) {
                        Icon(Icons.Default.History, "تاریخچه", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    TopBarAction(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, "تنظیمات", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarAction(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.15f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun SecondaryTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun EnhancedBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .shadow(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.screen.route
                val iconColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "navIcon"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(item.screen.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AppIcon3D(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        size = if (selected) 34 else 30,
                        tint = iconColor,
                        contentDescription = item.label
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        maxLines = 1,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}


