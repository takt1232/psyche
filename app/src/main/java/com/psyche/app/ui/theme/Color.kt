package com.psyche.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The fixed Budget Pass palette. Dynamic colour is deliberately disabled. */
data class PsychePalette(
    val isDark: Boolean,
    val ground: Color,
    val onGround: Color,
    val muted: Color,
    val faint: Color,
    val boardRow: Color,
    val boardLine: Color,
    val pass: Color,
    val passInk: Color,
    val passMuted: Color,
    val passLine: Color,
    val alert: Color,
    val accentText: Color,
    val onAlert: Color,
    val danger: Color,
    val dangerOnPass: Color,
    val cautionOnPass: Color,
    val panel: Color,
    val onPanel: Color,
    val panelLine: Color,
    val navContainer: Color,
    val navInactive: Color,
)

private val Ink = Color(0xFF0B0D10)
private val Slate = Color(0xFF1C2127)
private val Steel = Color(0xFF6B7178)
private val Panel = Color(0xFFE6E8EB)
private val Surface = Color(0xFFFFFFFF)
private val Alert = Color(0xFFFFD400)
private val DangerDark = Color(0xFFB3261E)
private val DangerLight = Color(0xFFFFB4AB)

val DarkPalette = PsychePalette(
    isDark = true,
    ground = Ink,
    onGround = Color(0xFFFFFFFF),
    muted = Color(0xFF9BA1A8),
    faint = Color(0xFF7C848C),
    boardRow = Color(0xFF15191E),
    boardLine = Color(0xFF2A2F35),
    pass = Surface,
    passInk = Ink,
    passMuted = Color(0xFF5C636B),
    passLine = Color(0xFFDDE0E3),
    alert = Alert,
    accentText = Alert,
    onAlert = Ink,
    danger = DangerLight,
    dangerOnPass = DangerDark,
    cautionOnPass = Color(0xFF7A5B00),
    panel = Slate,
    onPanel = Color(0xFFFFFFFF),
    panelLine = Color(0xFF2A2F35),
    navContainer = Ink,
    navInactive = Color(0xFF8A9199),
)

val LightPalette = PsychePalette(
    isDark = false,
    ground = Panel,
    onGround = Ink,
    muted = Color(0xFF4F565E),
    faint = Steel,
    boardRow = Surface,
    boardLine = Color(0xFFDDE0E3),
    pass = Surface,
    passInk = Ink,
    passMuted = Color(0xFF5C636B),
    passLine = Color(0xFFDDE0E3),
    alert = Alert,
    accentText = Color(0xFF7A5B00),
    onAlert = Ink,
    danger = DangerDark,
    dangerOnPass = DangerDark,
    cautionOnPass = Color(0xFF7A5B00),
    panel = Color(0xFFF4F5F6),
    onPanel = Ink,
    panelLine = Color(0xFFD5D8DC),
    navContainer = Surface,
    navInactive = Steel,
)

val LocalPsychePalette = staticCompositionLocalOf { DarkPalette }

val DarkColorScheme = darkColorScheme(
    primary = Alert,
    onPrimary = Ink,
    primaryContainer = Color(0xFF3B3512),
    onPrimaryContainer = Color(0xFFFFE66B),
    secondary = Color(0xFFC4C8CD),
    onSecondary = Ink,
    secondaryContainer = Color(0xFF2A2F35),
    onSecondaryContainer = Color(0xFFE6E8EB),
    tertiary = Color(0xFF9BCFCF),
    onTertiary = Ink,
    background = Ink,
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121519),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF262C33),
    onSurfaceVariant = Color(0xFFB5BBC2),
    outline = Color(0xFF4A5158),
    outlineVariant = Color(0xFF2A2F35),
    error = DangerLight,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Alert,
    onPrimaryContainer = Ink,
    secondary = Color(0xFF5C636B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE0E3),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF316B6B),
    onTertiary = Color(0xFFFFFFFF),
    background = Panel,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1F2F4),
    onSurfaceVariant = Color(0xFF4F565E),
    outline = Color(0xFF8D939A),
    outlineVariant = Color(0xFFD5D8DC),
    error = DangerDark,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)
