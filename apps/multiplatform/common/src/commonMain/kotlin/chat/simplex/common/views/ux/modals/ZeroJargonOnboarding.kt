package chat.simplex.common.views.ux.modals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.simplex.common.ui.theme.isInDarkTheme

data class OnboardingFeature(
    val icon: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)

@Composable
fun ZeroJargonOnboarding(
    onContinue: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val isDark = isInDarkTheme()
    val bgGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF070B14)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFEDE9FE)))
    }

    val features = remember {
        listOf(
            OnboardingFeature(
                icon = "🛡️",
                title = "Zéro Identifiant, Zéro Numéro",
                subtitle = "Pas de numéro de téléphone, pas d'email, pas de compte centralisé. Vous êtes 100% anonyme par conception.",
                accentColor = Color(0xFF38BDF8)
            ),
            OnboardingFeature(
                icon = "🔒",
                title = "Chiffrement Post-Quantum",
                subtitle = "Vos messages et fichiers sont protégés par le protocole SimpleX avec double-ratchet et sécurité quantique.",
                accentColor = Color(0xFF10B981)
            ),
            OnboardingFeature(
                icon = "✨",
                title = "Fluidité & Expérience Moderne",
                subtitle = "Gestes swipe rapides, formes d'ondes vocales, profils multiples instantanés et messages éphémères en 1 tap.",
                accentColor = Color(0xFFF59E0B)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(16.dp))

                // App Brand Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDark) Color(0x33F59E0B) else Color(0x1FF59E0B),
                    modifier = Modifier.border(
                        1.dp,
                        if (isDark) Color(0x66F59E0B) else Color(0x33F59E0B),
                        RoundedCornerShape(20.dp)
                    )
                ) {
                    Text(
                        text = "Bienvenue sur SimpleUX",
                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "La messagerie privée,\nréinventée pour vous.",
                    style = MaterialTheme.typography.h1.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Toute la puissance cryptographique de SimpleX dans une interface intuitive, fluide et élégante.",
                    style = MaterialTheme.typography.body1.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(28.dp))

                // Feature Cards
                features.forEachIndexed { index, feature ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                                slideInVertically(initialOffsetY = { 40 * (index + 1) })
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDark) Color(0x801E293B) else Color(0xCCFFFFFF),
                            elevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .border(
                                    1.dp,
                                    if (isDark) Color(0x26FFFFFF) else Color(0x1F000000),
                                    RoundedCornerShape(18.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(feature.accentColor.copy(alpha = if (isDark) 0.2f else 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = feature.icon,
                                        fontSize = 22.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = feature.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = feature.subtitle,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isDark) Color(0xFFE2B755) else Color(0xFFD97706),
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "Commencer l'expérience SimpleUX",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (onClose != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onClose) {
                        Text(
                            text = "Fermer",
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
