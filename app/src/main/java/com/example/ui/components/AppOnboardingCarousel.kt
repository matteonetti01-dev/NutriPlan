package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ApexBlack
import com.example.ui.theme.ApexBorder
import com.example.ui.theme.ApexCyanAccent
import com.example.ui.theme.ApexDarkSurface
import com.example.ui.theme.ApexDarkSurfaceHighlight
import com.example.ui.theme.ApexNeonLime
import com.example.ui.theme.ApexTextMuted
import com.example.ui.theme.ApexTextPrimary
import com.example.ui.theme.ApexTextSecondary
import kotlinx.coroutines.launch

data class OnboardingSlideData(
    val badge: String,
    val title: String,
    val subtitle: String,
    val primaryIcon: ImageVector,
    val iconAccentColor: Color,
    val bulletPoints: List<Pair<ImageVector, String>>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppOnboardingCarouselDialog(
    isReviewMode: Boolean = false,
    onDismiss: () -> Unit = {},
    onFinished: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (isReviewMode) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = isReviewMode,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        AppOnboardingContent(
            isReviewMode = isReviewMode,
            onDismiss = onDismiss,
            onFinished = onFinished
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppOnboardingContent(
    isReviewMode: Boolean = false,
    onDismiss: () -> Unit = {},
    onFinished: () -> Unit
) {
    val slides = listOf(
        OnboardingSlideData(
            badge = "BENVENUTO IN APEX",
            title = "Nutrizione Scientifica & Atwater",
            subtitle = "Pianifica e monitora la tua alimentazione con formule ufficiali e precisione assoluta.",
            primaryIcon = Icons.Default.AutoAwesome,
            iconAccentColor = ApexNeonLime,
            bulletPoints = listOf(
                Pair(Icons.Default.Scale, "Formule ufficiali Atwater: 4 kcal/g proteine e carbo, 9 kcal/g grassi"),
                Pair(Icons.Default.Check, "Scelta flessibile tra grammi (g) o percentuali (%) con bilanciamento automatico"),
                Pair(Icons.Default.AutoAwesome, "Avvisi di discrepanza per calcoli sempre corretti e certificati")
            )
        ),
        OnboardingSlideData(
            badge = "PIANI SU MISURA",
            title = "Piani Personalizzati & Pasti",
            subtitle = "Configura il tuo fabbisogno e crea infinite alternative per ogni pasto della giornata.",
            primaryIcon = Icons.AutoMirrored.Filled.Assignment,
            iconAccentColor = ApexCyanAccent,
            bulletPoints = listOf(
                Pair(Icons.Default.Restaurant, "Suddivisione pasti da 3 a 6 volte al giorno in base alle tue abitudini"),
                Pair(Icons.Default.Check, "Alternative flessibili per ogni pasto con calcoli nutrizionali esatti"),
                Pair(Icons.Default.Scale, "Verifica costante del raggiungimento degli obiettivi del piano")
            )
        ),
        OnboardingSlideData(
            badge = "CHEF AI & VOCE",
            title = "Chat AI con Dettatura a Voce",
            subtitle = "Trova subito cosa cucinare con gli ingredienti che hai in casa: puoi scrivere o parlare a voce.",
            primaryIcon = Icons.Default.Mic,
            iconAccentColor = Color(0xFFA78BFA),
            bulletPoints = listOf(
                Pair(Icons.Default.Mic, "Dettatura a voce: tocca il microfono per parlare invece di digitare"),
                Pair(Icons.AutoMirrored.Filled.Chat, "Proposte su misura in base a quello che hai realmente in frigo o dispensa"),
                Pair(Icons.Default.Check, "Copia direttamente le ricette consigliate nei tuoi pasti con un tap")
            )
        ),
        OnboardingSlideData(
            badge = "GIORNATA LIBERA",
            title = "Stima Pasti Fuori con la Chat AI",
            subtitle = "Sei al ristorante, al bar o in viaggio? Usa la modalità Chat per stimare le calorie al volo.",
            primaryIcon = Icons.Default.Flight,
            iconAccentColor = Color(0xFF38BDF8),
            bulletPoints = listOf(
                Pair(Icons.AutoMirrored.Filled.Chat, "Descrivi cosa stai mangiando: l'AI ti farà qualche domanda di precisione"),
                Pair(Icons.Default.Scale, "Stima affidabile di calorie, macronutrienti e ingredienti calcolati"),
                Pair(Icons.Default.Check, "Aggiungi con un solo tocco alla tua 'Giornata fuori' e prosegui la giornata")
            )
        )
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == slides.size - 1

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(ApexBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("onboarding_carousel"),
        color = ApexBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar (Fixed Header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF142918))
                        .border(1.dp, Color(0xFF22542B), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "APEX // INTRODUZIONE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ApexNeonLime,
                        letterSpacing = 0.5.sp
                    )
                }

                if (isReviewMode) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_onboarding_review")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = ApexTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else if (!isLastPage) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(slides.size - 1)
                            }
                        },
                        modifier = Modifier.testTag("skip_onboarding_button")
                    ) {
                        Text(
                            text = "Salta",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ApexTextMuted
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Carousel Slides (Flexible area taking all available space)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val slide = slides[pageIndex]
                OnboardingSlideContent(slide = slide)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fixed Bottom Control Section (Always visible, perfectly positioned)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicator dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(slides.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 22.dp else 7.dp,
                            label = "indicator_width"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(width)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) ApexNeonLime else ApexDarkSurfaceHighlight
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Buttons Row
                if (isLastPage) {
                    Button(
                        onClick = onFinished,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("enter_app_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApexNeonLime,
                            contentColor = ApexBlack
                        )
                    ) {
                        Text(
                            text = if (isReviewMode) "Ho Capito ✓ Chiudi" else "Inizia e Crea il Tuo Piano",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ApexBlack
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = ApexBlack,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ApexTextSecondary
                                ),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Indietro",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("next_onboarding_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ApexNeonLime,
                                contentColor = ApexBlack
                            )
                        ) {
                            Text(
                                text = "Avanti",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ApexBlack
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = ApexBlack,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlideContent(slide: OnboardingSlideData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Compact Glowing Icon Frame
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            slide.iconAccentColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ApexDarkSurfaceHighlight)
                    .border(1.2.dp, slide.iconAccentColor.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = slide.primaryIcon,
                    contentDescription = null,
                    tint = slide.iconAccentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Slide Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(slide.iconAccentColor.copy(alpha = 0.12f))
                .border(1.dp, slide.iconAccentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = slide.badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = slide.iconAccentColor,
                letterSpacing = 0.6.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = slide.title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            color = ApexTextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle
        Text(
            text = slide.subtitle,
            fontSize = 12.sp,
            color = ApexTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Bullet Points Container (Clean and Compact)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ApexBorder, RoundedCornerShape(12.dp)),
            color = ApexDarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slide.bulletPoints.forEach { (icon, text) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ApexDarkSurfaceHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = slide.iconAccentColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = text,
                            fontSize = 11.5.sp,
                            color = ApexTextPrimary,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
