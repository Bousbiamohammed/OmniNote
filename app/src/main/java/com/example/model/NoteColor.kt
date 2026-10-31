package com.example.model

import androidx.compose.ui.graphics.Color

data class NoteColor(
    val hex: String,
    val name: String,
    val darkSurface: Color,
    val lightSurface: Color,
    val accent: Color
)

object NoteColors {
    val PALETTE = listOf(
        NoteColor(
            hex = "#202124",
            name = "Default",
            darkSurface = Color(0xFF1E1F2A),
            lightSurface = Color(0xFFF1F5F9),
            accent = Color(0xFF64748B)
        ),
        NoteColor(
            hex = "#2E384D",
            name = "Indigo",
            darkSurface = Color(0xFF1E2638),
            lightSurface = Color(0xFFE0E7FF),
            accent = Color(0xFF6366F1)
        ),
        NoteColor(
            hex = "#1E3A34",
            name = "Mint",
            darkSurface = Color(0xFF152A26),
            lightSurface = Color(0xFFD1FAE5),
            accent = Color(0xFF10B981)
        ),
        NoteColor(
            hex = "#3D2B1F",
            name = "Amber",
            darkSurface = Color(0xFF2E2017),
            lightSurface = Color(0xFFFEF3C7),
            accent = Color(0xFFF59E0B)
        ),
        NoteColor(
            hex = "#372948",
            name = "Lavender",
            darkSurface = Color(0xFF271B36),
            lightSurface = Color(0xFFEDE9FE),
            accent = Color(0xFF8B5CF6)
        ),
        NoteColor(
            hex = "#3E2328",
            name = "Coral",
            darkSurface = Color(0xFF2B171B),
            lightSurface = Color(0xFFFFE4E6),
            accent = Color(0xFFF43F5E)
        ),
        NoteColor(
            hex = "#1E3344",
            name = "Sky",
            darkSurface = Color(0xFF142431),
            lightSurface = Color(0xFFE0F2FE),
            accent = Color(0xFF0284C7)
        ),
        NoteColor(
            hex = "#2D3748",
            name = "Slate",
            darkSurface = Color(0xFF1F2633),
            lightSurface = Color(0xFFE2E8F0),
            accent = Color(0xFF94A3B8)
        ),
        NoteColor(
            hex = "#293826",
            name = "Forest",
            darkSurface = Color(0xFF1B2619),
            lightSurface = Color(0xFFDCFCE7),
            accent = Color(0xFF22C55E)
        ),
        NoteColor(
            hex = "#38271E",
            name = "Clay",
            darkSurface = Color(0xFF261912),
            lightSurface = Color(0xFFFFEDD5),
            accent = Color(0xFFEA580C)
        )
    )

    fun getColorForHex(hex: String, isDark: Boolean): Color {
        val found = PALETTE.find { it.hex.equals(hex, ignoreCase = true) }
        return if (found != null) {
            if (isDark) found.darkSurface else found.lightSurface
        } else {
            if (isDark) Color(0xFF1E1F2A) else Color(0xFFF8FAFC)
        }
    }

    fun getAccentForHex(hex: String): Color {
        return PALETTE.find { it.hex.equals(hex, ignoreCase = true) }?.accent ?: Color(0xFF38BDF8)
    }
}
