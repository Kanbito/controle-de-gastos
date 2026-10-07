package br.com.felipejesus.controlegastos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta derivada da logo (dourado sobre preto).
val Ouro = Color(0xFFD4AF37)
val OuroClaro = Color(0xFFF1D78A)
val OuroEscuro = Color(0xFF8A6A12)
val PretoMarca = Color(0xFF0B0B0B)

private val Escuro = darkColorScheme(
    primary = Ouro,
    onPrimary = Color(0xFF231A00),
    primaryContainer = Color(0xFF4A3B0C),
    onPrimaryContainer = Color(0xFFF5E2A8),
    secondary = Color(0xFFCDBB8C),
    onSecondary = Color(0xFF2B2205),
    secondaryContainer = Color(0xFF3A321C),
    onSecondaryContainer = Color(0xFFEADFC0),
    tertiary = Color(0xFF9FD3B0),
    onTertiary = Color(0xFF00381F),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFEDE6D6),
    surface = Color(0xFF0D0D0D),
    onSurface = Color(0xFFEDE6D6),
    surfaceVariant = Color(0xFF2A2721),
    onSurfaceVariant = Color(0xFFCFC6B4),
    surfaceContainerLowest = Color(0xFF080808),
    surfaceContainerLow = Color(0xFF151412),
    surfaceContainer = Color(0xFF1A1917),
    surfaceContainerHigh = Color(0xFF23211E),
    surfaceContainerHighest = Color(0xFF2D2B27),
    outline = Color(0xFF989080),
    outlineVariant = Color(0xFF4B463C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val Claro = lightColorScheme(
    primary = Color(0xFF7A5C00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE08A),
    onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFF6A5D3F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E1BB),
    onSecondaryContainer = Color(0xFF231B04),
    tertiary = Color(0xFF2E6A45),
    onTertiary = Color.White,
    background = Color(0xFFFFFBF4),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFFFFBF4),
    onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFEDE1CF),
    onSurfaceVariant = Color(0xFF4D4639),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF3E6),
    surfaceContainer = Color(0xFFF6EEDF),
    surfaceContainerHigh = Color(0xFFF0E7D8),
    surfaceContainerHighest = Color(0xFFEAE1D2),
    outline = Color(0xFF7F7667),
    outlineVariant = Color(0xFFD0C5B4),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

/** Cores semânticas de finanças, ajustadas para contraste em cada tema. */
@Immutable
data class CoresFinanceiras(
    val receita: Color,
    val despesa: Color,
    val neutro: Color,
)

val LocalCoresFinanceiras = staticCompositionLocalOf {
    CoresFinanceiras(receita = Color(0xFF2E7D32), despesa = Color(0xFFC62828), neutro = Color.Gray)
}

private val TipografiaBase = Typography()
private val Tipografia = TipografiaBase.copy(
    displaySmall = TipografiaBase.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = TipografiaBase.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = TipografiaBase.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = TipografiaBase.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = TipografiaBase.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

/** Estilo para valores monetários: números tabulares alinham melhor em listas. */
val EstiloValor = TextStyle(
    fontFamily = FontFamily.Default,
    fontFeatureSettings = "tnum",
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
)

@Composable
fun ControleGastosTheme(escuro: Boolean, conteudo: @Composable () -> Unit) {
    val cores = if (escuro) {
        CoresFinanceiras(receita = Color(0xFF7BD88F), despesa = Color(0xFFFF8A80), neutro = Color(0xFFCFC6B4))
    } else {
        CoresFinanceiras(receita = Color(0xFF1B6E2E), despesa = Color(0xFFB3261E), neutro = Color(0xFF4D4639))
    }
    CompositionLocalProvider(LocalCoresFinanceiras provides cores) {
        MaterialTheme(
            colorScheme = if (escuro) Escuro else Claro,
            typography = Tipografia,
            content = conteudo,
        )
    }
}
