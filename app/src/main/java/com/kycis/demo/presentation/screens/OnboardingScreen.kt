package com.kycis.demo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    onStartKyc: () -> Unit,
    onExploreAllDemos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val heroGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
            Color.Transparent,
        ),
    )

    val features = remember {
        listOf(
            OnboardingFeature(
                title = "In-app AI onboarding guide",
                subtitle = "Guides users through complex KYC steps in real time.",
                points = listOf(
                    "Context-aware prompts reduce confusion and form errors.",
                    "Step support for PAN, Aadhaar, OTP, and selfie capture.",
                ),
                icon = Icons.Default.SmartToy,
            ),
            OnboardingFeature(
                title = "Drop-off recovery",
                subtitle = "Re-engages users who leave midway during onboarding.",
                points = listOf(
                    "Automated follow-ups bring users back into flow.",
                    "Users resume from where they stopped, not from scratch.",
                ),
                icon = Icons.Default.Sync,
            ),
            OnboardingFeature(
                title = "Voice calling AI agent",
                subtitle = "Handles onboarding calls with human-like voice support.",
                points = listOf(
                    "Language switch and call transfer when needed.",
                    "Useful for low-literacy or high-friction onboarding cases.",
                ),
                icon = Icons.Default.SupportAgent,
            ),
            OnboardingFeature(
                title = "Campaign analytics and insights",
                subtitle = "Tracks outcomes to optimize onboarding conversion.",
                points = listOf(
                    "Call status, summaries, and conversion signal tracking.",
                    "Use performance insights to improve journey completion.",
                ),
                icon = Icons.Default.Insights,
            ),
            OnboardingFeature(
                title = "Multi-channel engagement",
                subtitle = "Works across channels to keep users progressing.",
                points = listOf(
                    "Supports calling-led engagement and channel orchestration.",
                    "Designed for BFSI/Fintech onboarding and support.",
                ),
                icon = Icons.Default.Language,
            ),
        )
    }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentFeature = features[currentIndex]
    val isLast = currentIndex == features.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(heroGradient)
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KYCIS Voice Onboarding",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    text = "AI onboarding for faster KYC completion",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = "Feature-by-feature onboarding inspired by revrag.ai patterns.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrustChip("SOC-ready", Icons.Default.Security)
                    TrustChip("Live AI assist", Icons.Default.Bolt)
                    TrustChip("Step-by-step", Icons.Default.CheckCircle)
                }
            }
        }

        Text(
            text = "What this SDK helps you do",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )

        FeatureScreenCard(
            stepLabel = "Feature ${currentIndex + 1} of ${features.size}",
            feature = currentFeature,
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Quick integration snippet",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "AI.init(apiKey, userId)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Then call AI.setKycStep(...) per screen and let Voice FAB handle real-time assistance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { if (currentIndex > 0) currentIndex -= 1 },
                modifier = Modifier.weight(1f),
                enabled = currentIndex > 0,
            ) {
                Text("Previous")
            }
            Button(
                onClick = { if (!isLast) currentIndex += 1 else onStartKyc() },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isLast) "Start Full KYC Demo" else "Next Feature")
            }
        }

        OutlinedButton(
            onClick = onExploreAllDemos,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Explore All Demo Activities")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FeatureScreenCard(
    stepLabel: String,
    feature: OnboardingFeature,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = feature.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            feature.points.forEach { point ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class OnboardingFeature(
    val title: String,
    val subtitle: String,
    val points: List<String>,
    val icon: ImageVector,
)
