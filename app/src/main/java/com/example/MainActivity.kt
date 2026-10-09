package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ThemeSettingsDialog
import com.example.ui.components.FeedbackDialog
import com.example.ui.components.SupportDialog
import com.example.ui.tour.GuidedTourOverlay
import com.example.ui.tour.APP_GUIDED_TOUR_STEPS
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FiqihGuideScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.EditProfileDialog
import com.example.ui.screens.QadhaScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldDeep
import com.example.ui.viewmodel.FiqihViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FiqihViewModel = viewModel()
            val themePalette by viewModel.themePalette.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()

            MyApplicationTheme(
                palette = themePalette,
                themeMode = themeMode
            ) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: FiqihViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var showSplashScreen by remember { mutableStateOf(prefs.getBoolean("show_splash_intro", true)) }
    var showInitialProfileSetup by remember { mutableStateOf(false) }
    val hasCompletedTour = prefs.getBoolean("has_completed_guided_tour", false)
    var isGuidedTourActive by remember { mutableStateOf(false) }
    var currentTourStepIndex by remember { mutableIntStateOf(0) }
    val userProfile by viewModel.userProfile.collectAsState()

    // Screen 1: Splash Screen matching reference image
    if (showSplashScreen) {
        SplashScreen(
            onEnter = {
                showSplashScreen = false
                prefs.edit().putBoolean("show_splash_intro", false).apply()
            }
        )
        return
    }

    // Request POST_NOTIFICATIONS on Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val openTab = (context as? ComponentActivity)?.intent?.getIntExtra("OPEN_TAB", -1) ?: -1
        if (openTab >= 0) {
            viewModel.setSelectedTab(openTab)
        } else {
            viewModel.setSelectedTab(0) // Default ke tab Kalkulator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        EditProfileDialog(
            currentProfile = userProfile,
            isFirstSetup = false,
            onDismiss = { showProfileDialog = false },
            onSave = { name, bio, usePersonal, photoUri, avatarIndex, haidDays, suciDays, nifasDays ->
                viewModel.saveFullProfile(
                    name = name,
                    bio = bio,
                    usePersonalPhoto = usePersonal,
                    photoUri = photoUri,
                    avatarIndex = avatarIndex,
                    haidDays = haidDays,
                    suciDays = suciDays,
                    nifasDays = nifasDays
                )
                showProfileDialog = false
            }
        )
    }

    if (showInitialProfileSetup) {
        EditProfileDialog(
            currentProfile = userProfile,
            isFirstSetup = true,
            onDismiss = {
                prefs.edit().putBoolean("is_first_launch_profile", false).apply()
                showInitialProfileSetup = false
                if (!prefs.getBoolean("has_completed_guided_tour", false)) {
                    currentTourStepIndex = 0
                    isGuidedTourActive = true
                }
            },
            onSave = { name, bio, usePersonal, photoUri, avatarIndex, haidDays, suciDays, nifasDays ->
                viewModel.saveFullProfile(
                    name = name,
                    bio = bio,
                    usePersonalPhoto = usePersonal,
                    photoUri = photoUri,
                    avatarIndex = avatarIndex,
                    haidDays = haidDays,
                    suciDays = suciDays,
                    nifasDays = nifasDays
                )
                prefs.edit().putBoolean("is_first_launch_profile", false).apply()
                showInitialProfileSetup = false
                if (!prefs.getBoolean("has_completed_guided_tour", false)) {
                    currentTourStepIndex = 0
                    isGuidedTourActive = true
                }
            }
        )
    }

    if (showThemeDialog) {
        val currentPalette by viewModel.themePalette.collectAsState()
        val currentMode by viewModel.themeMode.collectAsState()
        ThemeSettingsDialog(
            currentPalette = currentPalette,
            currentMode = currentMode,
            onPaletteChange = { viewModel.setThemePalette(it) },
            onModeChange = { viewModel.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismissRequest = { showFeedbackDialog = false }
        )
    }

    if (showSupportDialog) {
        SupportDialog(
            onDismissRequest = { showSupportDialog = false }
        )
    }

    if (showSplashScreen) {
        SplashScreen(
            onEnter = {
                prefs.edit().putBoolean("show_splash_intro", false).apply()
                showSplashScreen = false
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFFAF7F2),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Kalkulator Fiqih"
                                    1 -> "Kalender Siklus"
                                    2 -> "Catatan Qadha Shalat"
                                    3 -> "Panduan Fiqih"
                                    4 -> "Profil & Riwayat"
                                    else -> "Kalkulator Fiqih"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = Color(0xFF1E211F)
                            )
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Haid, Istihadhah & Nifas"
                                    1 -> "Catatan Darah & Prediksi Suci"
                                    2 -> "Hutang Shalat & Pengganti"
                                    3 -> "Kaidah Ibadah Mazhab Syafi'i"
                                    4 -> "Riwayat Siklus & Adat Pribadi"
                                    else -> "Mazhab Syafi'i"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.2.sp
                                ),
                                color = EmeraldPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF1E211F),
                        actionIconContentColor = Color(0xFF454B46)
                    ),
                    actions = {
                        // Interactive Guided Tour trigger
                        IconButton(
                            onClick = {
                                currentTourStepIndex = 0
                                isGuidedTourActive = true
                            },
                            modifier = Modifier.testTag("action_help_sheet")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = "Tur Panduan Aplikasi",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // More menu (Theme, Feedback, Support)
                        Box {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu Lainnya",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Lihat Tampilan Pembuka (Splash)",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showSplashScreen = true
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Lightbulb,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Tur Panduan Aplikasi",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        currentTourStepIndex = 0
                                        isGuidedTourActive = true
                                    },
                                    modifier = Modifier.testTag("menu_start_guided_tour")
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Palette,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Tema & Warna",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showThemeDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_theme_settings")
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.RateReview,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Kritik & Saran",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showFeedbackDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_feedback")
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.FavoriteBorder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Dukungan & Partisipasi",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showSupportDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_support")
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(
                    color = Color(0xFFEBE5DC),
                    thickness = 1.dp
                )
            }
        },
        bottomBar = {
            ElegantBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> CalculatorScreen(viewModel = viewModel)
                1 -> CalendarScreen(viewModel = viewModel)
                2 -> QadhaScreen(viewModel = viewModel)
                3 -> FiqihGuideScreen()
                4 -> ProfileScreen(
                    viewModel = viewModel,
                    onStartGuidedTour = {
                        currentTourStepIndex = 0
                        isGuidedTourActive = true
                    }
                )
            }
        }
    }

    // Interactive Spotlight Guided Tour Overlay
    GuidedTourOverlay(
        isActive = isGuidedTourActive,
        currentStepIndex = currentTourStepIndex,
        currentTab = selectedTab,
        onNavigateToTab = { viewModel.setSelectedTab(it) },
        onNextStep = {
            if (currentTourStepIndex < APP_GUIDED_TOUR_STEPS.size - 1) {
                currentTourStepIndex++
            } else {
                prefs.edit().putBoolean("has_completed_guided_tour", true).apply()
                isGuidedTourActive = false
            }
        },
        onPreviousStep = {
            if (currentTourStepIndex > 0) {
                currentTourStepIndex--
            }
        },
        onCloseTour = {
            prefs.edit().putBoolean("has_completed_guided_tour", true).apply()
            isGuidedTourActive = false
        }
    )
}

private data class BottomNavItem(
    val index: Int,
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val testTag: String
)

@Composable
fun ElegantBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bottom_navigation_bar"),
        color = Color.White,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            HorizontalDivider(
                color = Color(0xFFEBE5DC),
                thickness = 1.dp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navItems = remember {
                    listOf(
                        BottomNavItem(
                            index = 0,
                            title = "Kalkulator",
                            activeIcon = Icons.Filled.Calculate,
                            inactiveIcon = Icons.Outlined.Calculate,
                            testTag = "tab_calculator"
                        ),
                        BottomNavItem(
                            index = 1,
                            title = "Kalender",
                            activeIcon = Icons.Filled.CalendarMonth,
                            inactiveIcon = Icons.Outlined.CalendarMonth,
                            testTag = "tab_calendar"
                        ),
                        BottomNavItem(
                            index = 2,
                            title = "Qadha",
                            activeIcon = Icons.Filled.FactCheck,
                            inactiveIcon = Icons.Outlined.FactCheck,
                            testTag = "tab_qadha"
                        ),
                        BottomNavItem(
                            index = 3,
                            title = "Panduan",
                            activeIcon = Icons.AutoMirrored.Filled.MenuBook,
                            inactiveIcon = Icons.AutoMirrored.Outlined.MenuBook,
                            testTag = "tab_guide"
                        ),
                        BottomNavItem(
                            index = 4,
                            title = "Profil",
                            activeIcon = Icons.Filled.Person,
                            inactiveIcon = Icons.Outlined.Person,
                            testTag = "tab_profile"
                        )
                    )
                }

                navItems.forEach { item ->
                    val isSelected = selectedTab == item.index

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected)
                            Color(0xFFE8F4EE)
                        else
                            Color.Transparent,
                        label = "pill_background_${item.index}"
                    )
                    val iconTextColor by animateColorAsState(
                        targetValue = if (isSelected)
                            com.example.ui.theme.EmeraldDeep
                        else
                            Color(0xFF8A908A),
                        label = "icon_text_color_${item.index}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onTabSelected(item.index) }
                            .padding(vertical = 4.dp)
                            .testTag(item.testTag),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Subtle pill-shaped indicator around the refined icon
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(pillBgColor)
                                    .padding(horizontal = 16.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                                    contentDescription = item.title,
                                    tint = iconTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    letterSpacing = 0.2.sp,
                                    fontSize = 11.sp
                                ),
                                color = iconTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Forced to read */ },
        title = { Text("Selamat Datang! 👋") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Aplikasi ini membantu Anda menghitung masa haid, nifas, dan istihadhah sesuai dengan fiqih Mazhab Syafi'i.")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Cara Penggunaan:", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("1. Pilih jenis perhitungan (Haid / Nifas) di tab Kalkulator.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("2. Masukkan tanggal dan waktu mulai keluar darah.")
                Spacer(modifier = Modifier.height(4.dp))
                Text("3. Masukkan tanggal dan waktu berhenti (atau tambah rentang waktu jika darah putus-nyambung).")
                Spacer(modifier = Modifier.height(4.dp))
                Text("4. Klik tombol 'Hitung Hasil' untuk melihat rincian hukum, kewajiban shalat, puasa, dan mandi wajib.")
                Spacer(modifier = Modifier.height(16.dp))
                Text("Anda juga bisa mencatat darah harian dan melihat prediksi siklus di tab 'Kalender', serta membaca rincian hukum di tab 'Panduan'.")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Mulai Menggunakan Aplikasi")
            }
        }
    )
}
