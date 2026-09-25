package noox.bzr.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import noox.bzr.auth.AndroidAuthSessionStore
import noox.bzr.auth.AuthViewModel
import noox.bzr.auth.CustomerAuthFlow
import noox.bzr.auth.UrlConnectionAuthApi
import noox.bzr.customer.CustomerJourneyFlow
import noox.bzr.customer.CustomerViewModel
import noox.bzr.customer.UrlConnectionCustomerApi
import noox.bzr.design.R as DesignR

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = AndroidAuthSessionStore(getSharedPreferences("auth", MODE_PRIVATE))
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(
                UrlConnectionAuthApi(BuildConfig.API_BASE_URL),
                session,
            ) as T
        })[AuthViewModel::class.java]
        val customerViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CustomerViewModel(
                UrlConnectionCustomerApi(BuildConfig.API_BASE_URL),
                session,
                getString(DesignR.string.common_currency_egp),
            ) as T
        })[CustomerViewModel::class.java]
        var authenticated by mutableStateOf(session.token != null)
        setContent {
            if (authenticated && !customerViewModel.requiresAuthentication) {
                CustomerJourneyFlow(customerViewModel)
            } else {
                CustomerAuthFlow(viewModel) { authenticated = true }
            }
        }
    }
}
