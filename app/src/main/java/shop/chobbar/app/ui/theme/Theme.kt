package shop.chobbar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shop.chobbar.app.di.AppContainer

object Brand {
    val Rose = Color(0xFFE11D48)
    val RoseDark = Color(0xFFBE123C)
    val RoseLight = Color(0xFFFB7185)
    val RoseSoft = Color(0xFFFFF1F3)
    val Ink = Color(0xFF1C1917)
    val Ink950 = Color(0xFF0C0A09)
    val Stone500 = Color(0xFF78716C)
    val Stone200 = Color(0xFFE7E2DD)
    val Paper = Color(0xFFFCFBFA)
    val Surface = Color(0xFFF5F3F1)
    val Success = Color(0xFF059669)
    val Warning = Color(0xFFD97706)
    val Amber = Color(0xFFFBBF24)
}

private val Light = lightColorScheme(
    primary = Brand.Rose, onPrimary = Color.White, primaryContainer = Brand.RoseSoft, onPrimaryContainer = Brand.RoseDark,
    secondary = Brand.Ink, onSecondary = Color.White, secondaryContainer = Brand.Surface, onSecondaryContainer = Brand.Ink,
    background = Brand.Paper, onBackground = Brand.Ink, surface = Color.White, onSurface = Brand.Ink,
    surfaceVariant = Brand.Surface, onSurfaceVariant = Brand.Stone500, surfaceContainer = Brand.Surface,
    surfaceContainerHigh = Color(0xFFEFEAE6), surfaceContainerLow = Color(0xFFF9F7F5),
    outline = Brand.Stone200, outlineVariant = Color(0xFFEEE9E4), error = Color(0xFFDC2626),
)

private val Dark = darkColorScheme(
    primary = Brand.RoseLight, onPrimary = Brand.Ink950, primaryContainer = Color(0xFF4C0519), onPrimaryContainer = Color(0xFFFECDD5),
    secondary = Color(0xFFF5F2EF), onSecondary = Brand.Ink, secondaryContainer = Color(0xFF221E1C), onSecondaryContainer = Color(0xFFF5F2EF),
    background = Color(0xFF0F0D0C), onBackground = Color(0xFFF5F2EF), surface = Color(0xFF181514), onSurface = Color(0xFFF5F2EF),
    surfaceVariant = Color(0xFF221E1C), onSurfaceVariant = Color(0xFFA8A29E), surfaceContainer = Color(0xFF1C1917),
    surfaceContainerHigh = Color(0xFF292524), surfaceContainerLow = Color(0xFF151211),
    outline = Color(0xFF2C2725), outlineVariant = Color(0xFF221E1C), error = Color(0xFFF87171),
)

private val Display = TextStyle(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)

private val ChobbarType = Typography(
    displaySmall = Display.copy(fontSize = 32.sp, lineHeight = 38.sp),
    headlineLarge = Display.copy(fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = Display.copy(fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = Display.copy(fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

private val ChobbarShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
)

val LocalLowEnd = staticCompositionLocalOf { false }
val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

@Composable
fun ChobbarTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = ChobbarType, shapes = ChobbarShapes, content = content)
}
