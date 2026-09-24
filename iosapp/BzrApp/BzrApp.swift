import BzrCore
import DesignSystem
import SwiftUI

@main
struct BzrApp: App {
    @State private var authViewModel: AuthViewModel
    @State private var customerViewModel: CustomerViewModel?
    private let session: AppAuthSessionStore
    private let apiBaseURL: URL

    init() {
        let configuredValue = Bundle.main.object(forInfoDictionaryKey: "BZRApiBaseURL") as? String
        let configured = configuredValue.flatMap { $0.isEmpty ? nil : $0 } ?? "http://localhost:8000/api/v1/"
        guard let url = URL(string: configured) else { fatalError("Invalid BZRApiBaseURL") }
        let session = AppAuthSessionStore()
        self.session = session
        apiBaseURL = url
        _authViewModel = State(initialValue: AuthViewModel(api: LiveAuthAPI(baseURL: url), session: session))
        _customerViewModel = State(
            initialValue: session.token.map {
                CustomerViewModel(
                    api: LiveCustomerAPI(baseURL: url, token: $0, appMode: "CUSTOMER"),
                    currencyLabel: bzrString("common.currency.egp"))
            })
    }

    var body: some Scene {
        WindowGroup {
            if let customerViewModel {
                CustomerJourneyRoot(viewModel: customerViewModel, onLogout: logout)
            } else {
                CustomerAuthFlow(viewModel: authViewModel) {
                    guard let token = session.token else { return }
                    customerViewModel = makeCustomerViewModel(token)
                }
            }
        }
    }

    private func makeCustomerViewModel(_ token: String) -> CustomerViewModel {
        CustomerViewModel(
            api: LiveCustomerAPI(baseURL: apiBaseURL, token: token, appMode: "CUSTOMER"),
            currencyLabel: bzrString("common.currency.egp"))
    }

    private func logout() {
        session.clear()
        customerViewModel = nil
    }
}
