import BzrCore
import DesignSystem
import SwiftUI

public struct C02AccountView: View {
    private let accountPhone = bzrString("customer.account.phone")
    let state: CustomerUIState
    let onOpen: (String) -> Void

    public init(state: CustomerUIState, onOpen: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onOpen = onOpen
    }

    public var body: some View {
        CustomerScreen(title: bzrString("customer.account.title"), state: state) {
            ProviderHeader(name: bzrString("drawer.name"), rating: "", services: accountPhone, size: .large, verifiedLabel: bzrString(state.messageKey ?? "account.unverified"))
            if state.messageKey == "account.unverified" { InfoBanner(text: bzrString("account.unverified")) }
            MenuRow(icon: .account, text: bzrString("customer.account.personal"), onClick: { onOpen("SCR-C33") })
            MenuRow(icon: .location, text: bzrString("customer.account.addresses"), onClick: { onOpen("SCR-C14") })
            MenuRow(icon: .orders, text: bzrString("customer.account.payments"))
            MenuRow(icon: .messages, text: bzrString("notifications.title"), onClick: { onOpen("SCR-C32") })
            MenuRow(icon: .info, text: bzrString("customer.account.help"), onClick: { onOpen("SCR-C29") })
            MenuRow(icon: .close, text: bzrString("customer.account.logout"), onClick: { onOpen("SCR-C10") })
        }
    }
}
