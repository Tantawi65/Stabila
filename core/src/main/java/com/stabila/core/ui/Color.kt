package com.stabila.core.ui

import androidx.compose.ui.graphics.Color

// ─── Background / Surface ────────────────────────────────────────────────────
// Warm zinc blacks — the same palette used by Linear, Vercel, Raycast.
val Zinc950  = Color(0xFF09090B)   // Dark app background
val Zinc900  = Color(0xFF18181B)   // Dark card / surface
val Zinc800  = Color(0xFF27272A)   // Dark elevated surface
val Zinc700  = Color(0xFF3F3F46)   // Dark borders
val Zinc600  = Color(0xFF52525B)   
val Zinc500  = Color(0xFF71717A)   // Muted / caption text
val Zinc400  = Color(0xFFA1A1AA)   // Dark secondary text
val Zinc300  = Color(0xFFD4D4D8)
val Zinc200  = Color(0xFFE4E4E7)   // Light borders / elevated
val Zinc100  = Color(0xFFF4F4F5)   // Light surface
val Zinc50   = Color(0xFFFAFAFA)   // Light background / Dark primary text

// ─── Primary Accent — Stabila Sky Blue ───────────────────────────────────────
// Brand identity color. Communicates calm, precision, medical trust.
val Indigo900 = Color(0xFF0C4A6E)  // Subtle tinted backgrounds / chips
val Indigo500 = Color(0xFF0EA5E9)  // Buttons, links, highlighted elements
val Indigo400 = Color(0xFF38BDF8)  // Hover / active states, scores
val Indigo200 = Color(0xFFBAE6FD)  // Light tint (suggestion chips)

// ─── Secondary Accent — Stabila Violet ───────────────────────────────────────
// Used for AI/classification results and high tremor warning states.
val Violet900 = Color(0xFF2E1065)  // Subtle violet backgrounds
val Violet500 = Color(0xFF8B5CF6)  // AI results, warning accent
val Violet400 = Color(0xFFA78BFA)  // Hover/active violet

// ─── Semantic Colours ────────────────────────────────────────────────────────
val Emerald500 = Color(0xFF22C55E)  // Stable / good tremor reading
val Amber500   = Color(0xFFF59E0B)  // Elevated tremor day
val Amber400   = Color(0xFFFBBF24)  // Amber lighter variant
val Red500     = Color(0xFFEF4444)  // Severe / error state

// Semantic variants with reduced opacity for backgrounds
val Emerald900 = Color(0xFF14532D)  // Success chip / badge background
val Amber900   = Color(0xFF78350F)  // Warning chip background
val Red900     = Color(0xFF7F1D1D)  // Error chip background

