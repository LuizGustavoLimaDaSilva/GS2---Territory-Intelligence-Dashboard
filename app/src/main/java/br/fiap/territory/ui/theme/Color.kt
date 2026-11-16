package br.fiap.territory.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Todos os pares texto/fundo abaixo têm contraste >= 4,5:1 (verificado com a fórmula WCAG). */
val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E8C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E9F8),
    onPrimaryContainer = Color(0xFF0B3350),
    secondary = Color(0xFF3F5566),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF7A4E00),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF17212B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17212B),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFF4A5866),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1F4F8),
    surfaceContainerHigh = Color(0xFFEBEFF4),
    surfaceContainerHighest = Color(0xFFE8EDF3),
    outline = Color(0xFF6B7885),
    outlineVariant = Color(0xFFC9D2DB),
    error = Color(0xFF8A1C14),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCE4E1),
    onErrorContainer = Color(0xFF8A1C14),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF9CCBF0),
    onPrimary = Color(0xFF0C2D45),
    primaryContainer = Color(0xFF184A6E),
    onPrimaryContainer = Color(0xFFD6E9F8),
    secondary = Color(0xFFB4C8D8),
    onSecondary = Color(0xFF1D3342),
    tertiary = Color(0xFFFFC970),
    onTertiary = Color(0xFF3D2800),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE2E8EE),
    surface = Color(0xFF1A2026),
    onSurface = Color(0xFFE2E8EE),
    surfaceVariant = Color(0xFF28313A),
    onSurfaceVariant = Color(0xFFB4BFCA),
    surfaceContainerLowest = Color(0xFF0E1216),
    surfaceContainerLow = Color(0xFF161C21),
    surfaceContainer = Color(0xFF1A2026),
    surfaceContainerHigh = Color(0xFF222A31),
    surfaceContainerHighest = Color(0xFF28313A),
    outline = Color(0xFF8A97A4),
    outlineVariant = Color(0xFF3A4551),
    error = Color(0xFFFFB7AE),
    onError = Color(0xFF4F1A16),
    errorContainer = Color(0xFF4F1A16),
    onErrorContainer = Color(0xFFFFB7AE),
)

/** Par de cores semântico: fundo do contêiner e cor do conteúdo (texto e ícone). */
@Immutable
class StatusPalette(val container: Color, val content: Color)

/** Cores semânticas do domínio: normal, informativo, atenção e crítico. Nunca comunicam sozinhas (sempre com ícone e rótulo). */
@Immutable
class StatusColors(
    val normal: StatusPalette,
    val info: StatusPalette,
    val attention: StatusPalette,
    val critical: StatusPalette,
)

val LightStatusColors = StatusColors(
    normal = StatusPalette(Color(0xFFE4F3E8), Color(0xFF0B5A2B)),
    info = StatusPalette(Color(0xFFE1EFFA), Color(0xFF0D4A73)),
    attention = StatusPalette(Color(0xFFFFF0D1), Color(0xFF6E4300)),
    critical = StatusPalette(Color(0xFFFCE4E1), Color(0xFF8A1C14)),
)

val DarkStatusColors = StatusColors(
    normal = StatusPalette(Color(0xFF143626), Color(0xFF9BE3B7)),
    info = StatusPalette(Color(0xFF123349), Color(0xFFA8D8F7)),
    attention = StatusPalette(Color(0xFF3D2C0A), Color(0xFFFFD592)),
    critical = StatusPalette(Color(0xFF4F1A16), Color(0xFFFFB7AE)),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
