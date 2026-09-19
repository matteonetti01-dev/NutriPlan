package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ApexNeonLime
import com.example.ui.theme.ApexTextPrimary

@Composable
fun ApexHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Logo + Text "APEX // AI"
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stylized neon lime triangular chevron logo
            Canvas(modifier = Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    lineTo(w * 0.15f, h * 0.9f)
                    lineTo(w * 0.45f, h * 0.72f)
                    lineTo(w * 0.5f, h * 0.45f)
                    lineTo(w * 0.55f, h * 0.72f)
                    lineTo(w * 0.85f, h * 0.9f)
                    close()
                }
                drawPath(
                    path = path,
                    color = ApexNeonLime,
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = "APEX",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = ApexTextPrimary,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "//",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = ApexNeonLime
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "AI",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = ApexTextPrimary,
                letterSpacing = 0.5.sp
            )
        }

        // Top-right Cyber Slanted Speed Dashes "///"
        Canvas(modifier = Modifier.size(width = 34.dp, height = 16.dp)) {
            val dashWidth = 3.5.dp.toPx()
            val slant = 6.dp.toPx()
            val spacing = 9.dp.toPx()

            for (i in 0..2) {
                val startX = i * spacing + slant
                drawLine(
                    color = ApexNeonLime,
                    start = Offset(startX, 1f),
                    end = Offset(startX - slant, size.height - 1f),
                    strokeWidth = dashWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
