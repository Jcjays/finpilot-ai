package com.finpilot.android.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Material 3 defaults with heavier display styles so headline amounts read clearly. */
val FinPilotTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Tabular figures keep columns of amounts aligned; applied by money components. */
val MoneyTextStyle: TextStyle = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    fontFeatureSettings = "tnum",
)
