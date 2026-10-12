package com.example.model

data class AvatarTemplate(
    val id: Int,
    val initial: String,
    val name: String,
    val backgroundColors: List<Long>,
    val accentColor: Long,
    val subtitle: String
)

val PRESET_AVATAR_TEMPLATES = listOf(
    AvatarTemplate(
        id = 0,
        initial = "A",
        name = "Aisyah",
        backgroundColors = listOf(0xFF2E7D32L, 0xFF1B5E20L),
        accentColor = 0xFFD4AF37L,
        subtitle = "Emerald Khadijah"
    ),
    AvatarTemplate(
        id = 1,
        initial = "F",
        name = "Fatimah",
        backgroundColors = listOf(0xFF880E4FL, 0xFF4A148CL),
        accentColor = 0xFFE91E63L,
        subtitle = "Rose Zahra"
    ),
    AvatarTemplate(
        id = 2,
        initial = "M",
        name = "Maryam",
        backgroundColors = listOf(0xFF00695CL, 0xFF004D40L),
        accentColor = 0xFF00B0FFL,
        subtitle = "Ocean Thahirah"
    ),
    AvatarTemplate(
        id = 3,
        initial = "K",
        name = "Khadijah",
        backgroundColors = listOf(0xFFB78103L, 0xFF795548L),
        accentColor = 0xFFFFD700L,
        subtitle = "Gold Kubra"
    )
)
