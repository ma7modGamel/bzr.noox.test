import BzrCore
import DesignSystem
import SwiftUI

@main
struct BzrApp: App {
    @UIApplicationDelegateAdaptor(BzrAppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    @State private var authViewModel: AuthViewModel
    @State private var customerViewModel: CustomerViewModel?
    @State private var providerViewModel: ProviderViewModel?
    /// A provider link opens the provider root on its target instead of the application status (DEC-058).
    @State private var providerLinkTarget: DeepLinkTarget?
    @State private var push = PushCenter.shared
    private let session: AppAuthSessionStore
    private let apiBaseURL: URL

    init() {
        let configuredValue = Bundle.main.object(forInfoDictionaryKey: "BZRApiBaseURL") as? String
        let configured =
            configuredValue.flatMap { $0.isEmpty ? nil : $0 }
                ?? "https://dg.dnbscy.com/api/v1/"
        guard let url = URL(string: configured) else { fatalError("Invalid BZRApiBaseURL") }
        let session = AppAuthSessionStore()
        self.session = session
        apiBaseURL = url
        _authViewModel = State(
            initialValue: AuthViewModel(api: LiveAuthAPI(baseURL: url), session: session))
        _customerViewModel = State(
            initialValue: session.token.map {
                CustomerViewModel(
                    api: LiveCustomerAPI(baseURL: url, token: $0, appMode: "CUSTOMER"),
                    currencyLabel: bzrString("common.currency.egp"))
            })
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if let providerViewModel {
                    ProviderJourneyRoot(
                        viewModel: providerViewModel,
                        onClose: { self.providerViewModel = nil },
                        onOpenSupport: openCustomerSupport
                    )
                    .task {
                        if let target = providerLinkTarget {
                            providerLinkTarget = nil
                            await providerViewModel.openDeepLink(
                                target, notificationsDenied: !push.notificationsAllowed)
                        } else {
                            await providerViewModel.loadApplication()
                        }
                    }
                } else if let customerViewModel {
                    CustomerJourneyRoot(
                        viewModel: customerViewModel, onLogout: logout,
                        onOpenProvider: openProvider
                    )
                    .task { signedIn(customerViewModel) }
                } else {
                    CustomerAuthFlow(viewModel: authViewModel) {
                        guard let token = session.token else { return }
                        customerViewModel = makeCustomerViewModel(token)
                    }
                }
            }
            // 17 §الروابط العميقة — `bremo://` and Universal Links for email links.
            .onOpenURL { open(link: $0.absoluteString) }
            .onChange(of: push.pendingLink) { link in
                guard let link, customerViewModel != nil else { return }
                push.pendingLink = nil
                open(link: link)
            }
            .onChange(of: scenePhase) { phase in
                if phase == .active { push.refreshAuthorization() }
            }
        }
    }

    private func makeCustomerViewModel(_ token: String) -> CustomerViewModel {
        CustomerViewModel(
            api: LiveCustomerAPI(baseURL: apiBaseURL, token: token, appMode: "CUSTOMER"),
            currencyLabel: bzrString("common.currency.egp"))
    }

    private func makeProviderViewModel(_ token: String) -> ProviderViewModel {
        let viewModel = ProviderViewModel(
            api: LiveProviderAPI(baseURL: apiBaseURL, token: token),
            currencyLabel: bzrString("common.currency.egp"))
        viewModel.onDeepLink = { open(link: $0) }
        return viewModel
    }

    /// DEC-058: hooks, device registration, the one-time permission prompt, and any link that waited for sign-in.
    private func signedIn(_ viewModel: CustomerViewModel) {
        viewModel.onDeepLink = { open(link: $0) }
        viewModel.notificationsAllowed = { PushCenter.shared.notificationsAllowed }
        push.onToken = { token in Task { await viewModel.registerPushDevice(token: token) } }
        if let token = push.token { Task { await viewModel.registerPushDevice(token: token) } }
        push.requestPermissionOnce()
        push.refreshAuthorization()
        if let link = push.pendingLink {
            push.pendingLink = nil
            open(link: link)
        }
    }

    /// The router decides the mode; a provider link needs a provider profile (17 §الروابط العميقة).
    private func open(link: String) {
        guard let token = session.token, let customerViewModel else {
            push.pendingLink = link
            return
        }
        let resolver = providerViewModel ?? makeProviderViewModel(token)
        Task {
            let target = DeepLinkRouter.route(link, providerStatus: await resolver.providerStatus())
            if target.mode == DeepLinkTarget.provider {
                if let providerViewModel {
                    await providerViewModel.openDeepLink(
                        target, notificationsDenied: !push.notificationsAllowed)
                } else {
                    providerLinkTarget = target
                    providerViewModel = resolver
                }
            } else {
                providerViewModel = nil
                await customerViewModel.openDeepLink(target)
            }
        }
    }

    /// AC-NTF-07 — the device token and the Sanctum token go before the local session.
    private func logout() {
        let viewModel = customerViewModel
        let deviceToken = push.token
        Task { await viewModel?.signOut(deviceToken: deviceToken) }
        session.clear()
        customerViewModel = nil
        providerViewModel = nil
    }

    private func openProvider() {
        guard let token = session.token else { return }
        providerViewModel = makeProviderViewModel(token)
    }

    private func openCustomerSupport() {
        providerViewModel = nil
        Task { await customerViewModel?.loadHelp() }
    }
}
