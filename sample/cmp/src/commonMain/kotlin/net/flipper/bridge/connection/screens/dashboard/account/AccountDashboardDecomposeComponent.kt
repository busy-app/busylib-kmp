package net.flipper.bridge.connection.screens.dashboard.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import net.flipper.bridge.connection.screens.decompose.DecomposeOnBackParameter
import net.flipper.bridge.connection.screens.decompose.ScreenDecomposeComponent

class AccountDashboardDecomposeComponent(
    componentContext: ComponentContext,
    private val onBack: DecomposeOnBackParameter,
    private val viewModelFactory: () -> AccountDashboardViewModel
) : ScreenDecomposeComponent(componentContext) {
    private val viewModel = instanceKeeper.getOrCreate {
        viewModelFactory()
    }

    @Composable
    override fun Render(modifier: Modifier) {
        val linkedAccountStatus by viewModel.linkedAccountStatusFlow.collectAsState()
        val authState by viewModel.authState.collectAsState()
        val loginError by viewModel.loginError.collectAsState()
        val isLoggingIn by viewModel.isLoggingIn.collectAsState()

        AccountDashboardContent(
            modifier = modifier,
            onBack = onBack::invoke,
            authState = authState,
            isLoggingIn = isLoggingIn,
            loginError = loginError,
            onLogin = viewModel::login,
            onLogout = viewModel::logout,
            linkedAccountStatus = linkedAccountStatus,
            onDeleteLinkedAccount = viewModel::deleteLinkedAccount
        )
    }
}
