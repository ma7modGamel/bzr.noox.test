package noox.bzr.gallery

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import noox.bzr.auth.AndroidAuthSessionStore
import noox.bzr.auth.AuthViewModel
import noox.bzr.auth.UrlConnectionAuthApi
import noox.bzr.auth.authScreen
import noox.bzr.customer.CustomerRoutes
import noox.bzr.customer.CustomerViewModel
import noox.bzr.customer.UrlConnectionCustomerApi
import noox.bzr.design.R as DesignR

/**
 * The single activity (DEC-047). Screens are Fragments in nav_graph; which one is shown follows the ViewModel
 * state, as the Compose setContent switched between CustomerAuthFlow and CustomerJourneyFlow.
 */
class MainActivity : AppCompatActivity() {
    private val session by lazy { AndroidAuthSessionStore(getSharedPreferences("auth", MODE_PRIVATE)) }
    private val authViewModel: AuthViewModel by viewModels()
    private val customerViewModel: CustomerViewModel by viewModels()
    private lateinit var authenticated: MutableStateFlow<Boolean>
    private var customerFlowActive = false

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                AuthViewModel::class.java -> AuthViewModel(UrlConnectionAuthApi(BuildConfig.API_BASE_URL), session)
                CustomerViewModel::class.java -> CustomerViewModel(
                    UrlConnectionCustomerApi(BuildConfig.API_BASE_URL),
                    session,
                    getString(DesignR.string.common_currency_egp),
                )
                else -> error("Unknown ViewModel: $modelClass")
            } as T
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // targetSdk 35 draws edge to edge: keep the screens clear of the status and navigation bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        authenticated = MutableStateFlow(session.token != null)
        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController
        navController.setGraph(
            navController.navInflater.inflate(R.navigation.nav_graph).apply { setStartDestination(destination()) },
            null,
        )

        // CustomerAuthFlow's LaunchedEffect(state.route), while the auth screens are shown.
        lifecycleScope.launch {
            combine(showsCustomer(), authViewModel.stateFlow) { customer, state -> if (customer) null else state.route }
                .distinctUntilChanged()
                .collect { route ->
                    when (route) {
                        "SCR-C12" -> authViewModel.open("SCR-C12")
                        "SCR-C10" -> authViewModel.open("SCR-C10")
                        "SCR-C01" -> authenticated.value = true
                    }
                }
        }
        // CustomerJourneyFlow's LaunchedEffect(Unit): load home whenever the customer screens start showing.
        lifecycleScope.launch {
            showsCustomer().distinctUntilChanged().collect { customer ->
                if (customer && !customerFlowActive) customerViewModel.loadHome()
                customerFlowActive = customer
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(showsCustomer(), authViewModel.stateFlow, customerViewModel.stateFlow) { _, _, _ -> destination() }
                    .distinctUntilChanged()
                    .collect { navController.show(it) }
            }
        }
    }

    private fun showsCustomer() = combine(authenticated, customerViewModel.requiresAuthenticationFlow) { signedIn, required -> signedIn && !required }

    private fun destination(): Int =
        if (authenticated.value && !customerViewModel.requiresAuthentication) {
            CustomerRoutes.destination(customerViewModel.state)
        } else {
            when (authScreen(authViewModel.state.screen)) {
                "SCR-C11" -> R.id.scr_c11
                "SCR-C12" -> R.id.scr_c12
                "SCR-C13" -> R.id.scr_c13
                else -> R.id.scr_c10
            }
        }

    /** One screen at a time, as the Compose flow replaced its content: the back stack never grows. */
    private fun NavController.show(destination: Int) {
        if (currentDestination?.id == destination) return
        navigate(destination, null, navOptions {
            popUpTo(graph.id) { inclusive = true }
            launchSingleTop = true
        })
    }
}
