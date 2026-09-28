package kr.chriskim.hbrowser.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Primary, onPrimary = OnPrimary, primaryContainer = PrimaryContainer,
    secondary = Secondary, secondaryContainer = SecondaryContainer, tertiary = Tertiary,
    background = Background, surface = Surface, surfaceVariant = SurfaceVariant,
    onSurface = OnSurface, outline = Outline
)
private val Dark = darkColorScheme(
    primary = PrimaryDark, onPrimary = BackgroundDark,
    background = BackgroundDark, surface = SurfaceDark
)

@Composable
fun HBrowserTheme(dark: Boolean = isSystemInDarkTheme(), dynamic: Boolean = true, content: @Composable () -> Unit) {
    val cs = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = cs, typography = Typography(), content = content)
}
