package com.stabila.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Stabila's Material3 dark color scheme.
 * Strictly zinc-based neutrals + indigo primary.
 *
 * Dark mode only for v1. Light mode can be added later by defining
 * a lightColorScheme() and toggling based on system setting.
 */
private val DarkColorScheme = darkColorScheme(
    // ── Core ──────────────────────────────────────────────
    primary            = Indigo500,
    onPrimary          = Zinc50,
    primaryContainer   = Indigo900,
    onPrimaryContainer = Indigo200,

    // ── Secondary (used for chips/tags) ───────────────────
    secondary          = Zinc700,
    onSecondary        = Zinc50,
    secondaryContainer = Zinc800,
    onSecondaryContainer = Zinc400,

    // ── Backgrounds ───────────────────────────────────────
    background         = Zinc950,
    onBackground       = Zinc50,
    surface            = Zinc900,
    onSurface          = Zinc50,
    surfaceVariant     = Zinc800,
    onSurfaceVariant   = Zinc400,

    // ── Borders ────────────────────────────────────────────
    outline            = Zinc700,
    outlineVariant     = Zinc800,

    // ── Semantic ───────────────────────────────────────────
    error              = Red500,
    onError            = Zinc50,
    errorContainer     = Red900,
    onErrorContainer   = Zinc50,
)

private val LightColorScheme = lightColorScheme(
    // ── Core ──────────────────────────────────────────────
    primary            = Indigo500,
    onPrimary          = Zinc50,
    primaryContainer   = Indigo200,
    onPrimaryContainer = Indigo900,

    // ── Secondary (used for chips/tags) ───────────────────
    secondary          = Zinc200,
    onSecondary        = Zinc900,
    secondaryContainer = Zinc100,
    onSecondaryContainer = Zinc600,

    // ── Backgrounds ───────────────────────────────────────
    background         = Zinc50,
    onBackground       = Zinc950,
    surface            = Zinc100,
    onSurface          = Zinc900,
    surfaceVariant     = Zinc200,
    onSurfaceVariant   = Zinc700,

    // ── Borders ────────────────────────────────────────────
    outline            = Zinc300,
    outlineVariant     = Zinc200,

    // ── Semantic ───────────────────────────────────────────
    error              = Red500,
    onError            = Zinc50,
    errorContainer     = Red500.copy(alpha = 0.1f),
    onErrorContainer   = Red900,
)

/**
 * Root composable. Wrap every screen with this.
 */
@Composable
fun StabilaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = StabilaTypography,
        shapes      = StabilaShapes,
        content     = content
    )
}
