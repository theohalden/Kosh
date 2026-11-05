package dev.kosh.financetracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import dev.kosh.financetracker.R

/** Google Play Services' downloadable-fonts provider — no font binary bundled in
 * the APK; resolved and cached on-device the first time it's needed, falling back
 * to the system sans-serif if Play services fonts are ever unavailable. */
private val manropeProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val manrope = GoogleFont("Manrope")

private val ManropeFamily = FontFamily(
    Font(googleFont = manrope, fontProvider = manropeProvider, weight = FontWeight.Normal),
    Font(googleFont = manrope, fontProvider = manropeProvider, weight = FontWeight.Medium),
    Font(googleFont = manrope, fontProvider = manropeProvider, weight = FontWeight.SemiBold),
    Font(googleFont = manrope, fontProvider = manropeProvider, weight = FontWeight.Bold),
)

/**
 * Per the approved handoff: Home amount 48-56sp, screen titles 27-32sp, section
 * titles 17-19sp, transaction merchant 15-16sp, supporting info 12-14sp.
 */
val KoshTypography = Typography(
    // Home hero amount
    displayLarge = TextStyle(fontFamily = ManropeFamily, fontSize = 52.sp, lineHeight = 58.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = ManropeFamily, fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = ManropeFamily, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = ManropeFamily, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = ManropeFamily, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = ManropeFamily, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontFamily = ManropeFamily, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = ManropeFamily, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
)
