package com.psyche.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.psyche.app.R

private fun weightAxis(fontRes: Int, weight: FontWeight, axisWeight: Int): Font =
    Font(
        resId = fontRes,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(axisWeight)),
    )

val PublicSans = FontFamily(
    weightAxis(R.font.public_sans, FontWeight.Normal, 400),
    weightAxis(R.font.public_sans, FontWeight.Medium, 500),
    weightAxis(R.font.public_sans, FontWeight.SemiBold, 600),
    weightAxis(R.font.public_sans, FontWeight.Bold, 700),
)

val RobotoMono = FontFamily(
    weightAxis(R.font.roboto_mono, FontWeight.Normal, 400),
    weightAxis(R.font.roboto_mono, FontWeight.Medium, 500),
    weightAxis(R.font.roboto_mono, FontWeight.Bold, 700),
)

val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_regular, FontWeight.Normal),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

val PsycheTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PublicSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    ),
)

/** Shared display and data styles that are not part of Material's type scale. */
object PassType {
    val amount = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 58.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.2).sp,
        fontFeatureSettings = "tnum",
    )
    val monoLabel = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Medium,
        fontSize = 8.sp,
        lineHeight = 11.sp,
        letterSpacing = 0.9.sp,
    )
    val monoMicro = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Normal,
        fontSize = 7.sp,
        lineHeight = 10.sp,
        letterSpacing = 0.7.sp,
    )
    val monoData = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.1.sp,
        fontFeatureSettings = "tnum",
    )
    val monoDataSmall = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 13.sp,
        fontFeatureSettings = "tnum",
    )
    val brand = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 24.sp,
        letterSpacing = 1.7.sp,
    )
    val passAmount = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 58.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.2).sp,
        fontFeatureSettings = "tnum",
    )
    val sectionTitle = TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.8.sp,
    )
}
