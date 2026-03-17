package net.flipper.bridge.connection.screens.dashboard.account

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import net.flipper.bridge.connection.feature.link.model.LinkedAccountInfo
import net.flipper.bridge.connection.screens.dashboard.common.DashboardInfoRow
import net.flipper.bridge.connection.screens.dashboard.common.DashboardScreenLayout
import net.flipper.bridge.connection.screens.dashboard.common.DashboardSectionCard
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal

@Composable
fun AccountDashboardContent(
    onBack: () -> Unit,
    authState: BUSYLibUserPrincipal,
    isLoggingIn: Boolean,
    loginError: String?,
    onLogin: (email: String, password: String) -> Unit,
    onLogout: () -> Unit,
    linkedAccountStatus: LinkedAccountInfo?,
    onDeleteLinkedAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardScreenLayout(
        modifier = modifier,
        title = "Account",
        onBack = onBack
    ) {
        AuthSectionContent(
            authState = authState,
            isLoggingIn = isLoggingIn,
            loginError = loginError,
            onLogin = onLogin,
            onLogout = onLogout
        )

        DashboardSectionCard(
            title = "Linked Device Account",
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            DashboardInfoRow(label = "Linked account", value = linkedAccountStatus.toUiText())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDeleteLinkedAccount,
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = MaterialTheme.colors.surface
                )
            ) {
                Text("Delete linked account")
            }
        }
    }
}

@Composable
private fun AuthSectionContent(
    authState: BUSYLibUserPrincipal,
    isLoggingIn: Boolean,
    loginError: String?,
    onLogin: (email: String, password: String) -> Unit,
    onLogout: () -> Unit
) {
    when (authState) {
        is BUSYLibUserPrincipal.Loading -> {
            DashboardSectionCard(
                title = "Authentication",
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                CircularProgressIndicator()
            }
        }

        is BUSYLibUserPrincipal.Empty -> {
            LoginSection(
                isLoggingIn = isLoggingIn,
                loginError = loginError,
                onLogin = onLogin
            )
        }

        is BUSYLibUserPrincipal.Token -> {
            LoggedInSection(
                userId = authState.userId.toString(),
                onLogout = onLogout
            )
        }
    }
}

@Composable
private fun LoginSection(
    isLoggingIn: Boolean,
    loginError: String?,
    onLogin: (email: String, password: String) -> Unit
) {
    DashboardSectionCard(
        title = "Login",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (loginError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = loginError,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onLogin(email, password) },
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoggingIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    color = MaterialTheme.colors.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Login")
            }
        }
    }
}

@Composable
private fun LoggedInSection(
    userId: String,
    onLogout: () -> Unit
) {
    DashboardSectionCard(
        title = "Authentication",
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        DashboardInfoRow(label = "User ID", value = userId)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = MaterialTheme.colors.surface
            )
        ) {
            Text("Logout")
        }
    }
}

private fun LinkedAccountInfo?.toUiText(): String {
    return when (this) {
        null -> "Unavailable"
        LinkedAccountInfo.NotLinked -> "Not linked"
        LinkedAccountInfo.Error -> "Error"
        LinkedAccountInfo.Disconnected -> "Disconnected"
        is LinkedAccountInfo.Linked.SameUser -> "Linked (same user: $linkedMail)"
        is LinkedAccountInfo.Linked.DifferentUser -> "Linked (different user: $linkedMail)"
        is LinkedAccountInfo.Linked.MissingBusyCloud -> "Linked (missing BusyCloud: $linkedMail)"
    }
}
