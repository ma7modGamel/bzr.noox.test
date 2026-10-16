import BzrCore
import DesignSystem
import SwiftUI
import UIKit

/// SCR-C02 — Figma «حسابي» 3002:4654 (DEC-076): the profile card (avatar, name, masked phone, «تعديل الملف الشخصي»),
/// three stat tiles, the provider-mode card, the menu and «تسجيل الخروج». `fieldValues`: name, phone, completed
/// orders, saved addresses, active orders. «طرق الدفع» is left out (ASM-05: no saved cards) and «تقييماتي» too (B-10: no endpoint).
public struct C02AccountView: View {
    let state: CustomerUIState
    let onOpen: (String) -> Void

    public init(state: CustomerUIState, onOpen: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onOpen = onOpen
    }

    private func value(_ index: Int) -> String { state.fieldValues.indices.contains(index) ? state.fieldValues[index] : "" }

    public var body: some View {
        CustomerScreen(title: bzrString("customer.account.title"), state: state) {
            profileCard
            if state.messageKey == "account.unverified" {
                InfoBanner(text: bzrString("account.unverified"))
            }
            HStack(alignment: .top, spacing: DesignSpace.m) {
                stat(.check, tint: DesignColors.primary600, well: DesignColors.statusPillBg, value: value(2), label: bzrString("account.stats.completed"))
                stat(.orders, tint: DesignColors.primary600, well: DesignColors.statusPillBg, value: value(4), label: bzrString("account.stats.active"))
                stat(.location, tint: DesignColors.slate400, well: DesignColors.surfaceContainer, value: value(3), label: bzrString("account.stats.addresses"))
            }
            if state.visibleActions.contains("open_provider_mode") { providerCard }
            menuRow(.info, label: bzrString("account.language"), chip: bzrString(AppLanguage.current == "ar" ? "language.ar" : "language.en")) {
                guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                UIApplication.shared.open(url)
            }
            VStack(spacing: 0) {
                let rows: [(BzrIconKey, String, String)] = [
                    (.account, "customer.account.personal", "SCR-C33"), (.location, "customer.account.addresses", "SCR-C14"),
                    (.bell, "notifications.title", "SCR-C32"), (.info, "customer.account.help", "SCR-C29"),
                    (.shield, "account.terms", "SCR-C34"),
                ]
                ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                    if index > 0 { Rectangle().fill(DesignColors.border).frame(height: DesignBorder.width) }
                    menuRow(
                        row.0, label: bzrString(row.1),
                        chip: row.2 == "SCR-C14" && !value(3).isEmpty
                            ? bzrPlural("account.addresses_count", count: Int(value(3)) ?? 0) : nil
                    ) { onOpen(row.2) }
                }
            }
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
            Button(action: { onOpen("SCR-C10") }) {
                HStack(spacing: DesignSpace.xs) {
                    BzrIcon(.logout, tint: DesignColors.danger, size: DesignSize.iconSmall)
                    BzrText(bzrString("customer.account.logout"), style: DesignType.tileTitle.colored(DesignColors.danger))
                }
                .frame(maxWidth: .infinity, minHeight: DesignSize.primaryButtonHeight)
                .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.dangerTint))
            }
            .buttonStyle(BzrPressStyle())
        }
    }

    private var profileCard: some View {
        let name = value(0)
        return VStack(spacing: DesignSpace.s) {
            // BACKEND-NOTES B-16: the app loads no remote photos, so the avatar shows the first letter.
            Circle().fill(DesignColors.statusPillBg)
                .frame(width: DesignSize.profileAvatar, height: DesignSize.profileAvatar)
                .overlay(BzrText(String(name.prefix(1)), style: DesignType.profileName.colored(DesignColors.primary600)))
                .overlay(Circle().stroke(DesignColors.surfaceContainer, lineWidth: DesignSize.controlStroke))
            HStack(spacing: DesignSpace.xs) {
                BzrText(name, style: DesignType.profileName)
                if state.messageKey == "account.verified" {
                    BzrIcon(.verified, label: bzrString("account.verified"), tint: DesignColors.primary600, size: DesignSize.iconSmall)
                }
            }
            if !value(1).isEmpty {
                BzrText(Self.maskPhone(value(1)), style: DesignType.secondary).environment(\.layoutDirection, .leftToRight)
            }
            Button(action: { onOpen("SCR-C33") }) {
                HStack(spacing: DesignSpace.xs) {
                    BzrIcon(.edit, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                    BzrText(bzrString("account.edit_profile"), style: DesignType.caption.colored(DesignColors.navy900))
                }
                .padding(.horizontal, DesignSpace.l)
                .frame(minHeight: DesignSize.chipHeight)
                .background(Capsule().fill(DesignColors.surfaceAlt2))
            }
            .buttonStyle(BzrPressStyle())
        }
        .padding(DesignSpace.l)
        .frame(maxWidth: .infinity)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
    }

    private func stat(_ icon: BzrIconKey, tint: Color, well: Color, value: String, label: String) -> some View {
        VStack(spacing: DesignSpace.xs) {
            BzrIcon(icon, tint: tint, size: DesignSize.iconSmall)
                .frame(width: DesignSize.orderWell, height: DesignSize.orderWell)
                .background(Circle().fill(well))
            BzrText(value.isEmpty ? "—" : value, style: DesignType.statValue, alignment: .center)
            BzrText(label, style: DesignType.hint, alignment: .center)
        }
        .padding(.horizontal, DesignSpace.s)
        .padding(.vertical, DesignSpace.m)
        .frame(maxWidth: .infinity)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
        .accessibilityElement(children: .combine)
    }

    private var providerCard: some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) {
            HStack(spacing: DesignSpace.s) {
                BzrIcon(.wrench, tint: DesignColors.primary600)
                BzrText(bzrString("account.provider.title"), style: DesignType.homeSection)
                Spacer(minLength: DesignSpace.s)
                BzrText(bzrString("menu.provider_mode"), style: DesignType.hint.colored(DesignColors.onPrimary))
                    .padding(.horizontal, DesignSpace.s)
                    .background(Capsule().fill(DesignColors.primary600))
            }
            BzrText(bzrString("account.provider.body"), style: DesignType.secondary)
            PrimaryButton(text: bzrString("account.provider.action"), icon: .arrowForward) { onOpen("SCR-P01") }
        }
        .padding(DesignSpace.l)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.primary50Info))
    }

    private func menuRow(_ icon: BzrIconKey, label: String, chip: String?, onClick: @escaping () -> Void) -> some View {
        Button(action: onClick) {
            HStack(spacing: DesignSpace.m) {
                BzrIcon(icon, tint: DesignColors.primary600)
                    .frame(width: DesignSize.orderWell, height: DesignSize.orderWell)
                    .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
                BzrText(label, style: DesignType.tileTitle)
                Spacer(minLength: DesignSpace.s)
                if let chip {
                    BzrText(chip, style: DesignType.hint)
                        .padding(.horizontal, DesignSpace.s)
                        .background(Capsule().fill(DesignColors.surfaceAlt2))
                }
                BzrIcon(.chevron, tint: DesignColors.slate400, size: DesignSize.iconSmall)
            }
            .padding(.horizontal, DesignSpace.l)
            .padding(.vertical, DesignSpace.m)
            .frame(minHeight: DesignSize.touchTargetMin)
            .contentShape(Rectangle())
        }
        .buttonStyle(BzrPressStyle())
    }

    /// "010 •••• 5678": the start and the last four digits only.
    private static func maskPhone(_ phone: String) -> String {
        phone.count < 7 ? phone : "\(phone.prefix(3)) •••• \(phone.suffix(4))"
    }
}
