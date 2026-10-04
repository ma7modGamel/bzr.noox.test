package noox.bzr.gallery

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
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
import noox.bzr.auth.RetrofitAuthApi
import noox.bzr.auth.authScreen
import noox.bzr.customer.CustomerRoutes
import noox.bzr.customer.CustomerViewModel
import noox.bzr.customer.UrlConnectionCustomerApi
import noox.bzr.design.R as DesignR
import noox.bzr.design.views.BottomNavView
import noox.bzr.links.AndroidPushDevice
import noox.bzr.links.BremoMessagingService
import noox.bzr.links.DeepLinkRouter
import noox.bzr.links.DeepLinkTarget
import noox.bzr.links.NotificationChannels
import noox.bzr.provider.ProviderRoutes
import noox.bzr.provider.ProviderViewModel
import noox.bzr.provider.UrlConnectionProviderApi

/**
 * The single activity (DEC-047). Screens are Fragments in nav_graph; which one is shown follows the ViewModel
 * state, as the Compose setContent switched between CustomerAuthFlow and CustomerJourneyFlow.
 */
class MainActivity : AppCompatActivity() {
    private val session by lazy { AndroidAuthSessionStore(getSharedPreferences("auth", MODE_PRIVATE)) }
    private val authViewModel: AuthViewModel by viewModels()
    private val customerViewModel: CustomerViewModel by viewModels()
    private val providerViewModel: ProviderViewModel by viewModels()
    private lateinit var authenticated: MutableStateFlow<Boolean>
    private var customerFlowActive = false
    private val push by lazy { AndroidPushDevice(applicationContext) }
    /** A notification or link that arrived before sign-in, or before the first screen (17 §الروابط العميقة). */
    private var pendingLink: String? = null
    private val bottomNav by lazy { findViewById<BottomNavView>(R.id.bottom_nav) }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
                AuthViewModel::class.java -> AuthViewModel(RetrofitAuthApi(BuildConfig.API_BASE_URL), session)
                CustomerViewModel::class.java -> CustomerViewModel(
                    UrlConnectionCustomerApi(BuildConfig.API_BASE_URL),
                    session,
                    getString(DesignR.string.common_currency_egp),
                    push,
                )
                ProviderViewModel::class.java -> ProviderViewModel(
                    UrlConnectionProviderApi(BuildConfig.API_BASE_URL),
                    session,
                    getString(DesignR.string.common_currency_egp),
                )
                else -> error("Unknown ViewModel: $modelClass")
            } as T
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        // targetSdk 35 draws edge to edge: keep the screens clear of the status and navigation bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setUpBottomNav()
        authenticated = MutableStateFlow(session.token != null)
        NotificationChannels.create(this)
        customerViewModel.onDeepLink = ::openLink
        providerViewModel.onDeepLink = ::openLink
        BremoMessagingService.onTokenRefreshed = { customerViewModel.registerPushDevice(it) }
        if (savedInstanceState == null) pendingLink = linkFrom(intent)
        // System back follows the same history as the top bar; on customer home it leaves the app (6ب).
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val handled = when {
                    providerViewModel.flowActive.value -> providerViewModel.goBack().let { true }
                    authenticated.value && !customerViewModel.requiresAuthentication -> customerViewModel.goBack()
                    else -> false
                }
                if (!handled) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
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
                if (customer && !customerFlowActive) {
                    val link = pendingLink
                    pendingLink = null
                    if (link == null) customerViewModel.loadHome() else openLink(link)
                    onSignedIn()
                }
                customerFlowActive = customer
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    showsCustomer(), authViewModel.stateFlow, customerViewModel.stateFlow,
                    providerViewModel.flowActive, providerViewModel.stateFlow,
                ) { _, _, _, _, _ -> destination() }
                    .distinctUntilChanged()
                    .collect {
                        navController.show(it)
                        showTab(it)
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val link = linkFrom(intent) ?: return
        if (customerFlowActive) openLink(link) else pendingLink = link
    }

    /** DEC-062: the bottom navigation under the customer tab screens; the selection animates between them. */
    private fun setUpBottomNav() {
        bottomNav.items = listOf(
            DesignR.drawable.ic_home to getString(DesignR.string.nav_home),
            DesignR.drawable.ic_orders to getString(DesignR.string.nav_orders),
            DesignR.drawable.ic_chat to getString(DesignR.string.nav_messages),
            DesignR.drawable.ic_account to getString(DesignR.string.nav_account),
        )
        bottomNav.onSelect = { index ->
            if (index != CUSTOMER_TABS.indexOf(destination())) {
                bottomNav.selectedIndex = index
                when (index) {
                    0 -> customerViewModel.loadHome()
                    1 -> customerViewModel.loadOrders()
                    2 -> customerViewModel.loadConversations()
                    3 -> customerViewModel.loadAccountSummary()
                }
            }
        }
    }

    private fun showTab(destination: Int) {
        val tab = CUSTOMER_TABS.indexOf(destination)
        bottomNav.isVisible = tab >= 0
        if (tab >= 0 && bottomNav.selectedIndex != tab) bottomNav.selectedIndex = tab
    }

    /** Push extras carry `deep_link`; `bremo://` and App Links arrive as the intent data. */
    private fun linkFrom(intent: Intent?): String? =
        intent?.getStringExtra(AndroidPushDevice.DEEP_LINK)?.takeIf(String::isNotBlank) ?: intent?.dataString

    /** 17 §الروابط العميقة: the router decides the mode; a provider link needs a provider profile. */
    private fun openLink(link: String) {
        providerViewModel.resolveProviderStatus { status ->
            val target = DeepLinkRouter.route(link, status)
            if (target.mode == DeepLinkTarget.PROVIDER) {
                providerViewModel.openDeepLink(target, notificationsDenied = !push.notificationsAllowed())
            } else {
                providerViewModel.closeFlow()
                customerViewModel.openDeepLink(target)
            }
        }
    }

    /** DEC-058: register this device, and ask for the notification permission once. */
    private fun onSignedIn() {
        runCatching {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                push.store(token)
                customerViewModel.registerPushDevice(token)
            }
        }
        if (push.shouldAskPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            push.markPermissionAsked()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showsCustomer() = combine(authenticated, customerViewModel.requiresAuthenticationFlow) { signedIn, required -> signedIn && !required }

    private fun destination(): Int =
        if (providerViewModel.flowActive.value) {
            ProviderRoutes.destination(providerViewModel.stateFlow.value)
        } else if (authenticated.value && !customerViewModel.requiresAuthentication) {
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

    private companion object {
        /** C01, C25, C18, C02: the bottom navigation order (43 §3). */
        val CUSTOMER_TABS = listOf(R.id.scr_c01, R.id.scr_c25, R.id.scr_c18, R.id.scr_c02)
    }
}
