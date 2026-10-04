import BzrCore
import DesignSystem
import SwiftUI

public struct C02AccountView: View {
    let state: CustomerUIState
    let onOpen: (String) -> Void

    public init(state: CustomerUIState, onOpen: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onOpen = onOpen
    }

    public var body: some View {
        CustomerScreen(title: bzrString("customer.account.title"), state: state) {
            ProviderHeader(
                name: state.fieldValues.first ?? "", rating: "",
                services: (state.fieldValues.count > 1 && !state.fieldValues[1].isEmpty) ? state.fieldValues[1] : nil,
                size: .large,
                verifiedLabel: state.messageKey == "account.verified" ? bzrString("account.verified") : nil)
            if state.messageKey == "account.unverified" {
                InfoBanner(text: bzrString("account.unverified"))
            }
            MenuRow(
                icon: .account, text: bzrString("customer.account.personal"), onClick: { onOpen("SCR-C33") }
            )
            MenuRow(
                icon: .location, text: bzrString("customer.account.addresses"),
                onClick: { onOpen("SCR-C14") })
            MenuRow(
                icon: .messages, text: bzrString("notifications.title"), onClick: { onOpen("SCR-C32") })
            MenuRow(icon: .info, text: bzrString("customer.account.help"), onClick: { onOpen("SCR-C29") })
            MenuRow(icon: .account, text: bzrString("menu.provider_mode"), onClick: { onOpen("SCR-P01") })
            MenuRow(
                icon: .close, text: bzrString("customer.account.logout"), onClick: { onOpen("SCR-C10") })
        }
    }
}
