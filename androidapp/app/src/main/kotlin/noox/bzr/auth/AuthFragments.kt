package noox.bzr.auth

import androidx.fragment.app.activityViewModels
import noox.bzr.screens.ScreenFragment

// SCR-C10..C13 Fragments (DEC-047), wired to AuthViewModel as CustomerAuthFlow wired the Compose screens.

abstract class AuthFragment(private val screen: String) : ScreenFragment<AuthUiState, AuthScreenView>() {
    protected val viewModel: AuthViewModel by activityViewModels()
    override val states get() = viewModel.stateFlow
    override fun accepts(state: AuthUiState) = authScreen(state.screen) == screen
    override fun create() = authScreenView(requireContext(), screen)
    override fun AuthScreenView.render(state: AuthUiState) = render(state)
    override fun AuthScreenView.bind() {
        actions = AuthActions(
            onFieldChange = viewModel::update,
            onSubmit = viewModel::submit,
            onCheck = viewModel::checkVerification,
            onResend = viewModel::resendVerification,
            onOpen = viewModel::open,
            onLogout = viewModel::logout,
        )
    }
}

class CustomerLoginFragment : AuthFragment("SCR-C10")

class CustomerRegisterFragment : AuthFragment("SCR-C11")

class CustomerEmailVerificationFragment : AuthFragment("SCR-C12")

class CustomerPasswordRecoveryFragment : AuthFragment("SCR-C13")

/** The auth screen shown for a state screen code; anything else is the login screen, as in CustomerAuthFlow. */
fun authScreen(screen: String): String = if (screen in setOf("SCR-C11", "SCR-C12", "SCR-C13")) screen else "SCR-C10"
