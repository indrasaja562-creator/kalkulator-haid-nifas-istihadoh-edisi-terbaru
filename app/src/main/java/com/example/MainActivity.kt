package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.FiqihGuideScreen
import com.example.ui.screens.PrayerAndQadhaScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FiqihViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
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
    var showOnboarding by remember { mutableStateOf(prefs.getBoolean("is_first_launch", true)) }

    var showMenu by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    if (showOnboarding) {
        OnboardingDialog(onDismiss = {
            prefs.edit().putBoolean("is_first_launch", false).apply()
            showOnboarding = false
        })
    }

    if (showFeedbackDialog) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text("Kritik dan Saran") },
            text = {
                Column {
                    Text("Bantu kami berkembang! Sampaikan kritik dan saran melalui:")
                    Spacer(Modifier.height(16.dp))
                    Text("Email:", style = MaterialTheme.typography.bodySmall)
                    Text("indrasaja562@gmail.com", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(onClick = {
                    uriHandler.openUri("https://www.instagram.com/iniindra27_?stkn=MTlocm12d2ZxNmFlMw==")
                    showFeedbackDialog = false
                }) {
                    Text("Instagram Saya")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Dukungan") },
            text = {
                Column {
                    Text("Dukung pengembangan aplikasi ini!")
                    Spacer(Modifier.height(16.dp))
                    Text("Buat neraktir kopi yang bikin aplikasi ☕", fontStyle = FontStyle.Italic)
                    Spacer(Modifier.height(8.dp))
                    Text("DANA:", style = MaterialTheme.typography.bodySmall)
                    Text("082315744568", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(onClick = { showSupportDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "Kalkulator Haid & Nifas"
                            1 -> "Jadwal Shalat & Qadha"
                            2 -> "Panduan Fiqih Syafi'i"
                            3 -> "Profil & Riwayat Siklus"
                            else -> "Kalkulator Fiqih"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.setTab(2) },
                        modifier = Modifier.testTag("open_guide_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Kitab Rujukan",
                            tint = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu Lainnya", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Kritik dan Saran") },
                                onClick = {
                                    showMenu = false
                                    showFeedbackDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Dukungan") },
                                onClick = {
                                    showMenu = false
                                    showSupportDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_navigation_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Calculate else Icons.Outlined.Calculate,
                            contentDescription = "Kalkulator"
                        )
                    },
                    label = { Text("Kalkulator") },
                    modifier = Modifier.testTag("tab_calculator")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Alarm else Icons.Outlined.Alarm,
                            contentDescription = "Pengingat & Qadha"
                        )
                    },
                    label = { Text("Pengingat") },
                    modifier = Modifier.testTag("tab_reminder")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                            contentDescription = "Panduan & Rujukan"
                        )
                    },
                    label = { Text("Panduan") },
                    modifier = Modifier.testTag("tab_guide")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profil & Riwayat"
                        )
                    },
                    label = { Text("Profil") },
                    modifier = Modifier.testTag("tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> CalculatorScreen(viewModel = viewModel)
                1 -> PrayerAndQadhaScreen(viewModel = viewModel)
                2 -> FiqihGuideScreen()
                3 -> ProfileScreen(viewModel = viewModel)
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
                Text("Anda juga bisa membaca rincian hukum di tab 'Panduan' dan mencatat shalat qadha di tab 'Pengingat'.")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Mulai Menggunakan Aplikasi")
            }
        }
    )
}
