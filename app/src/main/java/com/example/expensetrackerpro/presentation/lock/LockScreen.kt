package com.example.expensetrackerpro.presentation.lock

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.withResumed
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.presentation.main.uistate.MainEvent

@Composable
fun LockScreen(
    isPromptRequested: Boolean,
    message: LockMessage?,
    onEvent: (MainEvent) -> Unit
) {
    val activity = LocalActivity.current as? FragmentActivity
    val authenticator = rememberBiometricAuthenticator { onEvent(MainEvent.AuthFinished(it)) }
    val promptTitle = stringResource(R.string.lock_prompt_title)
    val promptSubtitle = stringResource(R.string.lock_prompt_subtitle)

    LaunchedEffect(isPromptRequested, authenticator) {
        if (!isPromptRequested || activity == null || authenticator == null) return@LaunchedEffect
        // The prompt can only open over a resumed activity
        activity.lifecycle.withResumed {
            onEvent(MainEvent.PromptLaunched)
            authenticator.authenticate(promptTitle, promptSubtitle)
        }
    }

    LockContent(message = message, onUnlock = { onEvent(MainEvent.UnlockClicked) })
}

@Composable
fun LockContent(
    message: LockMessage?,
    onUnlock: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_splash_logo),
                contentDescription = null,
                modifier = Modifier.size(160.dp)
            )
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            if (message != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(message.textRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onUnlock) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.lock_unlock))
            }
        }
    }
}

@StringRes
private fun LockMessage.textRes(): Int = when (this) {
    LockMessage.CANCELED -> R.string.lock_message_canceled
    LockMessage.LOCKOUT -> R.string.lock_message_lockout
    LockMessage.LOCKOUT_PERMANENT -> R.string.lock_message_lockout_permanent
    LockMessage.ERROR -> R.string.lock_message_error
}

@PreviewLightDark
@Composable
private fun LockContentPreview() {
    ExpenseTrackerProTheme {
        LockContent(message = null, onUnlock = {})
    }
}

@PreviewLightDark
@Composable
private fun LockContentLockoutPreview() {
    ExpenseTrackerProTheme {
        LockContent(message = LockMessage.LOCKOUT, onUnlock = {})
    }
}
