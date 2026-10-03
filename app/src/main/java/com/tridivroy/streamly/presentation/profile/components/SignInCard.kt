package com.tridivroy.streamly.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tridivroy.streamly.R
import com.tridivroy.streamly.core.theme.LogoSizeTopBar
import com.tridivroy.streamly.core.theme.SageMint
import com.tridivroy.streamly.core.theme.StreamlyIcons
import com.tridivroy.streamly.core.theme.StreamlyLogoTile
import com.tridivroy.streamly.core.theme.StreamlyShape
import com.tridivroy.streamly.core.theme.StreamlyType
import com.tridivroy.streamly.core.theme.TextMuted
import com.tridivroy.streamly.domain.model.SignInMethod

/**
 * The signed-out state: one card carrying the pitch, the two sign-in buttons and what signing in
 * actually gets you. Lit from the top-left by the same mint glow the splash uses, so the card reads
 * as the screen's single focal point rather than a form.
 */
@Composable
fun SignInCard(
    pendingMethod: SignInMethod?,
    onSignIn: (SignInMethod) -> Unit,
    modifier: Modifier = Modifier,
    /** Shown only where skipping sign-in is an option -- onboarding, not the Profile screen. */
    onContinueAsGuest: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(StreamlyShape.Card)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .background(
                Brush.radialGradient(
                    colors = listOf(SageMint.copy(alpha = 0.13f), Color.Transparent),
                    radius = 520f,
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, StreamlyShape.Card)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StreamlyLogoTile(modifier = Modifier.size(LogoSizeTopBar + 10.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.profile_signin_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.profile_signin_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SignInButton(
                label = stringResource(R.string.profile_signin_google),
                method = SignInMethod.Google,
                pendingMethod = pendingMethod,
                primary = true,
                onClick = onSignIn,
            )
            SignInButton(
                label = stringResource(R.string.profile_signin_email),
                method = SignInMethod.Email,
                pendingMethod = pendingMethod,
                primary = false,
                icon = StreamlyIcons.Mail,
                onClick = onSignIn,
            )
            if (onContinueAsGuest != null) {
                GuestButton(
                    pending = pendingMethod == SignInMethod.Guest,
                    enabled = pendingMethod == null,
                    onClick = onContinueAsGuest,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.profile_benefits_title),
                style = StreamlyType.Eyebrow,
                color = TextMuted,
            )
            Benefit(StreamlyIcons.CloudOff, stringResource(R.string.profile_benefit_offline))
            Benefit(StreamlyIcons.Spark, stringResource(R.string.profile_benefit_sync))
            Benefit(StreamlyIcons.Shield, stringResource(R.string.profile_benefit_noads))
        }

        Text(
            text = stringResource(R.string.profile_signin_disclaimer),
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SignInButton(
    label: String,
    method: SignInMethod,
    pendingMethod: SignInMethod?,
    primary: Boolean,
    onClick: (SignInMethod) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val isPending = pendingMethod == method
    val anyPending = pendingMethod != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT)
            .clip(StreamlyShape.ActionButton)
            .background(
                if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .border(
                width = 1.dp,
                color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = StreamlyShape.ActionButton,
            )
            .clickable(enabled = !anyPending) { onClick(method) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        val content = if (primary) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

        if (isPending) {
            CircularProgressIndicator(
                color = content,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else if (method == SignInMethod.Google) {
            GoogleMark()
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
        )
    }
}

/** Text-only, so it reads as the way past the card rather than a third account option. */
@Composable
private fun GuestButton(
    pending: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT)
            .clip(StreamlyShape.ActionButton)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (pending) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = stringResource(R.string.profile_signin_guest),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

/**
 * A neutral monogram, not Google's logo.
 *
 * The real mark is a four-colour asset with its own brand guidelines, and approximating it by hand
 * would ship something that looks official but is not. This stands in until Google Sign-In is
 * actually wired up, at which point the official drawable from the SDK replaces it.
 */
@Composable
private fun GoogleMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "G",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Benefit(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(StreamlyShape.asymmetricPill(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val BUTTON_HEIGHT = 48.dp
