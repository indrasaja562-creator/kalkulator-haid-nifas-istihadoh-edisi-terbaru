package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fiqih.FiqihGuideData
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.FiqihHaidColor
import com.example.ui.theme.FiqihSuciColor
import com.example.ui.theme.GoldTertiary

data class FiqihGuideModuleCategory(
    val id: String,
    val indexNumber: String,
    val title: String,
    val subtitle: String,
    val sourceBook: String,
    val itemCount: Int,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun FiqihGuideScreen(
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<FiqihGuideModuleCategory?>(null) }

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    AnimatedContent(
        targetState = selectedCategory,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "FiqihGuideTransition",
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2))
    ) { category ->
        if (category != null) {
            FiqihGuideDetailScreen(
                category = category,
                onBackClick = { selectedCategory = null }
            )
        } else {
            FiqihGuideHomeScreen(
                onSelectCategory = { selectedCategory = it }
            )
        }
    }
}

/**
 * Halaman Utama Panduan Fiqih Digital: Premium Islamic Feminine Library
 */
@Composable
fun FiqihGuideHomeScreen(
    onSelectCategory: (FiqihGuideModuleCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterId by remember { mutableStateOf("ALL") }

    val guideModules = remember {
        listOf(
            FiqihGuideModuleCategory(
                id = "istihadhah",
                indexNumber = "01",
                title = "7 Golongan Istihadhah Haid",
                subtitle = "Memahami klasifikasi mustahadhah, penentuan masa haid, dan konsekuensi ibadahnya",
                sourceBook = "Uyunul Masa'il Linnisa' · Tuhfatun Niswah",
                itemCount = FiqihGuideData.haidGuideList.size,
                icon = Icons.Outlined.Bloodtype,
                color = EmeraldPrimary
            ),
            FiqihGuideModuleCategory(
                id = "nifas",
                indexNumber = "02",
                title = "Fiqih Nifas & 5 Golongan Pasca Salin",
                subtitle = "Batas maksimal 60 hari, jeda suci pemisah 15 hari, dan hukum ibadah persalinan",
                sourceBook = "Uyunul Masa'il Linnisa' · Tuhfatun Niswah",
                itemCount = FiqihGuideData.nifasGuideList.size,
                icon = Icons.Outlined.ChildCare,
                color = EmeraldPrimary
            ),
            FiqihGuideModuleCategory(
                id = "niat",
                indexNumber = "03",
                title = "Niat Mandi Bersuci & Puasa",
                subtitle = "Lafadz Arab mu'tamad, teks transliterasi latin, terjemahan, serta rukun mandi wajib",
                sourceBook = "Fathul Qarib · Kasyifatus Saja · Al-Bajuri",
                itemCount = FiqihGuideData.niatList.size,
                icon = Icons.Outlined.WaterDrop,
                color = EmeraldPrimary
            ),
            FiqihGuideModuleCategory(
                id = "larangan",
                indexNumber = "04",
                title = "11 Larangan Hadats Besar",
                subtitle = "Hal-hal yang diharamkan saat haid dan nifas beserta landasan dalil syariat",
                sourceBook = "Al-Qur'an · Sunnah Nabawiyyah · Fiqih Syafi'i",
                itemCount = FiqihGuideData.prohibitions.size,
                icon = Icons.Outlined.Mosque,
                color = EmeraldPrimary
            ),
            FiqihGuideModuleCategory(
                id = "qadha",
                indexNumber = "05",
                title = "Kaidah Qadha Shalat & Puasa",
                subtitle = "Aturan wajib shalat saat darah keluar di awal waktu atau suci di akhir waktu",
                sourceBook = "Fathul Mu'in · Nihayatul Muhtaj",
                itemCount = FiqihGuideData.qadhaRulesList.size,
                icon = Icons.Outlined.AccessTime,
                color = EmeraldPrimary
            )
        )
    }

    val filteredModules = remember(searchQuery, selectedFilterId) {
        guideModules.filter { module ->
            val matchesFilter = when (selectedFilterId) {
                "ALL" -> true
                else -> module.id == selectedFilterId
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                val q = searchQuery.trim().lowercase()
                module.title.lowercase().contains(q) ||
                        module.subtitle.lowercase().contains(q) ||
                        module.sourceBook.lowercase().contains(q)
            }
            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2))
            .testTag("fiqih_guide_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Editorial Header (No large banner)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("guide_hero_banner"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Kaidah Fiqih Thaharah",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color(0xFF1E211F)
                )
                Text(
                    text = "Kajian komprehensif hukum bersuci, hadats & ibadah berlandaskan dalil mu'tamad Mazhab Syafi'i",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    ),
                    color = Color(0xFF6B726C)
                )
            }
        }

        // 2. Search Field: Elegant Ivory Input Box
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Cari pembahasan fiqih, dalil, niat...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = Color(0xFF8A908A)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hapus",
                                tint = Color(0xFF6B726C),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    "ALL" to "Semua",
                    "istihadhah" to "Istihadhah",
                    "nifas" to "Nifas",
                    "niat" to "Niat Mandi",
                    "larangan" to "Larangan",
                    "qadha" to "Qadha"
                )
                items(filters) { (id, label) ->
                    val isSelected = selectedFilterId == id
                    Surface(
                        modifier = Modifier.clickable { selectedFilterId = id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) EmeraldPrimary else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) GoldTertiary.copy(alpha = 0.6f) else Color(0xFFEBE5DC)
                        ),
                        shadowElevation = if (isSelected) 1.dp else 0.dp
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = if (isSelected) Color.White else Color(0xFF454B46),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 4. Guide Module Cards
        if (filteredModules.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEBE5DC))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = Color(0xFF8A908A),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak Ditemukan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E211F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tidak ada materi panduan yang cocok dengan pencarian \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B726C),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredModules, key = { it.id }) { module ->
                FiqihGuideCard(
                    module = module,
                    onClick = { onSelectCategory(module) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}

/**
 * Modern Islamic Feminine Card for Guide Category
 */
@Composable
fun FiqihGuideCard(
    module: FiqihGuideModuleCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("guide_card_${module.id}"),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gold Editorial Number Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF9F3E5))
                    .border(1.dp, GoldTertiary.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = module.indexNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = EmeraldDeep
                )
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Color(0xFF1E211F)
                )

                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    ),
                    color = Color(0xFF6B726C)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F4EE)
                    ) {
                        Text(
                            text = "${module.itemCount} Kajian",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                        )
                    }

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB0A99F)
                    )

                    Text(
                        text = module.sourceBook,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF6B726C),
                        maxLines = 1
                    )
                }
            }

            // Arrow forward circle
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFAF7F2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Buka",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Halaman Rincian Modul Pembahasan Fiqih: Luxury Islamic Reading Mode
 */
@Composable
fun FiqihGuideDetailScreen(
    category: FiqihGuideModuleCategory,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2))
    ) {
        // Minimal Elegant Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            border = BorderStroke(0.5.dp, Color(0xFFEBE5DC))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFAF7F2))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = EmeraldDeep,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1E211F),
                        maxLines = 1
                    )
                    Text(
                        text = category.sourceBook,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF6B726C),
                        maxLines = 1
                    )
                }
            }
        }

        // Reading Content List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (category.id) {
                "istihadhah" -> {
                    itemsIndexed(FiqihGuideData.haidGuideList) { idx, item ->
                        EditorialFiqihDetailCard(
                            number = String.format("%02d", idx + 1),
                            title = item.categoryName,
                            reference = item.kitabReference,
                            definition = item.definition,
                            legalStatus = item.hukumHaidDanIstihadhah,
                            consequence = item.kewajibanShalatDanQadha
                        )
                    }
                }
                "nifas" -> {
                    itemsIndexed(FiqihGuideData.nifasGuideList) { idx, item ->
                        EditorialFiqihDetailCard(
                            number = String.format("%02d", idx + 1),
                            title = item.categoryName,
                            reference = item.kitabReference,
                            definition = item.definition,
                            legalStatus = item.hukumNifasDanIstihadhah,
                            consequence = item.kewajibanShalatDanQadha
                        )
                    }
                }
                "niat" -> {
                    itemsIndexed(FiqihGuideData.niatList) { idx, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NIAT ${String.format("%02d", idx + 1)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = GoldTertiary
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFAF7F2),
                                        border = BorderStroke(0.5.dp, Color(0xFFEBE5DC))
                                    ) {
                                        Text(
                                            text = item.rujukanKitab,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                            color = Color(0xFF6B726C),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color(0xFF1E211F)
                                )

                                // Arabic text in soft warm ivory container
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFCF9F5),
                                    border = BorderStroke(1.dp, GoldTertiary.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = item.arabicText,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 20.sp,
                                                lineHeight = 36.sp,
                                                fontWeight = FontWeight.Bold,
                                                textDirection = TextDirection.Rtl
                                            ),
                                            color = EmeraldDeep,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = item.latinText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = EmeraldPrimary
                                )

                                Text(
                                    text = "Artinya: \"${item.translation}\"",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = Color(0xFF1E211F)
                                )

                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        lineHeight = 17.sp
                                    ),
                                    color = Color(0xFF6B726C)
                                )
                            }
                        }
                    }
                }
                "larangan" -> {
                    itemsIndexed(FiqihGuideData.prohibitions) { idx, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF9ECEE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = String.format("%02d", item.number),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = FiqihHaidColor
                                        )
                                    }
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color(0xFF1E211F)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF9ECEE)
                                ) {
                                    Text(
                                        text = "HUKUM: ${item.legalStatus.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            letterSpacing = 0.6.sp
                                        ),
                                        color = FiqihHaidColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = item.explanation,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = Color(0xFF454B46)
                                )

                                Text(
                                    text = "Dalil: ${item.reference}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF6B726C)
                                )
                            }
                        }
                    }
                }
                "qadha" -> {
                    itemsIndexed(FiqihGuideData.qadhaRulesList) { idx, item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE8F4EE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = String.format("%02d", idx + 1),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = EmeraldPrimary
                                        )
                                    }
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        color = Color(0xFF1E211F)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE8F4EE)
                                ) {
                                    Text(
                                        text = item.ruleType.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            letterSpacing = 0.6.sp
                                        ),
                                        color = EmeraldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = item.explanation,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp
                                    ),
                                    color = Color(0xFF454B46)
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFAF7F2),
                                    border = BorderStroke(0.5.dp, Color(0xFFEBE5DC))
                                ) {
                                    Text(
                                        text = item.example,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            lineHeight = 17.sp
                                        ),
                                        color = Color(0xFF555B55),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }

                                Text(
                                    text = "Rujukan: ${item.referenceKitab}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF6B726C)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

/**
 * Digital reading card for Fiqih categories (7 Golongan Istihadhah & Nifas)
 */
@Composable
fun EditorialFiqihDetailCard(
    number: String,
    title: String,
    reference: String,
    definition: String,
    legalStatus: String,
    consequence: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEBE5DC)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF9F3E5))
                            .border(1.dp, GoldTertiary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = number,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = EmeraldDeep
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1E211F)
                    )
                }
            }

            // Reference Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFAF7F2),
                border = BorderStroke(0.5.dp, Color(0xFFEBE5DC))
            ) {
                Text(
                    text = "Kitab: $reference",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color(0xFF6B726C),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // Definition
            Text(
                text = definition,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                ),
                color = Color(0xFF3E433F)
            )

            HorizontalDivider(
                color = Color(0xFFEBE5DC).copy(alpha = 0.6f),
                thickness = 0.8.dp
            )

            // Status Hukum Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF9ECEE),
                border = BorderStroke(0.5.dp, FiqihHaidColor.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "STATUS HUKUM FIQIH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = FiqihHaidColor
                    )
                    Text(
                        text = legalStatus,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        ),
                        color = Color(0xFF3B151C)
                    )
                }
            }

            // Kewajiban Ibadah Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8F4EE),
                border = BorderStroke(0.5.dp, EmeraldPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "KEWAJIBAN IBADAH & SHALAT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = EmeraldPrimary
                    )
                    Text(
                        text = consequence,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        ),
                        color = Color(0xFF0D2D24)
                    )
                }
            }
        }
    }
}
