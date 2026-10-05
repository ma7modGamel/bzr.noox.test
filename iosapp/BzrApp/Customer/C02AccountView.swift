import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C02 (DEC-063): profile card, grouped menu, gradient provider-mode card, sign out.
public struct C02AccountView: View {
    let state: CustomerUIState
    let onOpen: (String) -> Void

    public init(state: CustomerUIState, onOpen: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onOpen = onOpen
    }

    public var body: some View {
        CustomerScreen(title: bzrString("customer.account.title"), state: state) {
            AccountCard {
                ProviderHeader(
                    name: state.fieldValues.first ?? "", rating: "",
                    services: (state.fieldValues.count > 1 && !state.fieldValues[1].isEmpty)
                        ? state.fieldValues[1] : nil,
                    size: .large,
                    verifiedLabel: state.messageKey == "account.verified" ? bzrString("account.verified") : nil)
            }
            if state.messageKey == "account.unverified" {
                InfoBanner(text: bzrString("account.unverified"))
            }
            AccountCard {
                VStack(spacing: 0) {
                    MenuRow(icon: .account, text: bzrString("customer.account.personal"), onClick: { onOpen("SCR-C33") })
                    Divider().overlay(DesignColors.border)
                    MenuRow(
                        icon: .location, text: bzrString("customer.account.addresses"), onClick: { onOpen("SCR-C14") })
                    Divider().overlay(DesignColors.border)
                    MenuRow(icon: .bell, text: bzrString("notifications.title"), onClick: { onOpen("SCR-C32") })
                    Divider().overlay(DesignColors.border)
                    MenuRow(icon: .info, text: bzrString("customer.account.help"), onClick: { onOpen("SCR-C29") })
                }
            }
            providerModeCard
            AccountCard {
                MenuRow(
                    icon: .logout, text: bzrString("customer.account.logout"), onClick: { onOpen("SCR-C10") },
                    danger: true)
            }
        }
    }

    private var providerModeCard: some View {
        Button {
            onOpen("SCR-P01")
        } label: {
            HStack(spacing: DesignSpace.m) {
                BzrIcon(.wrench, tint: DesignColors.onPrimary)
                    .frame(width: DesignSize.menuIconBox, height: DesignSize.menuIconBox)
                    .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.onHeroWell))
                VStack(alignment: .leading, spacing: DesignSpace.xxs) {
                    BzrText(bzrString("menu.provider_mode"), style: DesignType.button)
                    BzrText(
                        bzrString("customer.account.provider_mode.body"),
                        style: DesignType.caption.colored(DesignColors.onHeroBody))
                }
                Spacer(minLength: 0)
                BzrIcon(.chevron, tint: DesignColors.onPrimary)
            }
            .padding(DesignSpace.l)
            .background(RoundedRectangle(cornerRadius: DesignRadius.button).fill(DesignGradients.brand))
            .bzrShadow(DesignShadows.brand)
        }
        .buttonStyle(BzrPressStyle())
        .accessibilityElement(children: .combine)
    }
}

/// A white card that lifts off the tinted screen (DEC-063).
private struct AccountCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) { self.content = content() }

    var body: some View {
        content
            .padding(.horizontal, DesignSpace.l)
            .padding(.vertical, DesignSpace.xs)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
    }
}
