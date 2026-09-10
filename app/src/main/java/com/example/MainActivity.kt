package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FirstPlanOnboardingDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OutdoorDayScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NutriCardBorder
import com.example.ui.theme.NutriDark
import com.example.ui.theme.NutriTextMuted
import com.example.ui.theme.NutriTextPrimary
import com.example.ui.theme.NutriTextSecondary
import com.example.ui.viewmodel.NutritionViewModel

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: NutritionViewModel = viewModel()
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: NutritionViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val allPlans by viewModel.allPlans.collectAsState()

    val navItems = listOf(
        NavItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
        NavItem("Piano", Icons.Filled.Assignment, Icons.Outlined.Assignment, "tab_piano"),
        NavItem("Giornata fuori", Icons.Filled.Flight, Icons.Outlined.Flight, "tab_giornata_fuori"),
        NavItem("Impostazioni", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_impostazioni")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.8.dp, color = NutriCardBorder),
                containerColor = Color.White,
                tonalElevation = 2.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = currentTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(index) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NutriDark,
                            selectedTextColor = NutriDark,
                            unselectedIconColor = NutriTextMuted,
                            unselectedTextColor = NutriTextSecondary,
                            indicatorColor = Color(0xFFF3F4F6)
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToPlan = { viewModel.selectTab(1) }
                )
                1 -> PlanScreen(viewModel = viewModel)
                2 -> OutdoorDayScreen(viewModel = viewModel)
                3 -> SettingsScreen(viewModel = viewModel)
            }

            // Onboarding popup on first startup if no plans exist yet
            if (allPlans.isEmpty()) {
                FirstPlanOnboardingDialog(
                    onConfirm = { name, cal, prot, c, f, meals ->
                        viewModel.createPlan(
                            name = name,
                            calories = cal,
                            protein = prot,
                            carbs = c,
                            fat = f,
                            mealsCount = meals,
                            makeActive = true
                        )
                    }
                )
            }
        }
    }
}

