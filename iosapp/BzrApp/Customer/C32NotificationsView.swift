import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C32 — Figma «الإشعارات» 3002:3608 (DEC-076): «اليوم» and «سابق» groups; each card has the type icon (from the
/// NTF code) with an unread dot, the title, body and relative time. Unread cards are white, read ones tinted.
public struct C32NotificationsView: View {
    let state: CustomerUIState
    let onOpen: (Int) -> Void
    let onOpenSettings: () -> Void
    let onBack: (() -> Void)?

    public init(
        state: CustomerUIState, onOpen: @escaping (Int) -> Void = { _ in },
        onOpenSettings: @escaping () -> Void = {}, onBack: (() -> Void)? = nil
    ) {
        self.state = state
        self.onOpen = onOpen
        self.onOpenSettings = onOpenSettings
        self.onBack = onBack
    }

    public var body: some View {
        CustomerScreen(title: bzrString("notifications.title"), state: state, onBack: onBack) {
            // DEC-058 — الإذن مرفوض على الجهاز: تنبيه ورابط الإعدادات، والقائمة تعمل كما هي.
            if state.notificationsDenied {
                InfoBanner(text: bzrString("notifications.permission.off"))
                LinkButton(text: bzrString("action.open_settings"), onClick: onOpenSettings)
            }
            if state.items.isEmpty {
                EmptyState(
                    title: bzrString("notifications.empty.title"),
                    body: bzrString("notifications.empty.body"))
            } else {
                let today = state.items.indices.filter { state.stepStates[safe: $0] == "today" }
                let earlier = state.items.indices.filter { state.stepStates[safe: $0] != "today" }
                ForEach([("notifications.today", today), ("notifications.earlier", earlier)], id: \.0) { key, rows in
                    if !rows.isEmpty {
                        HStack(spacing: DesignSpace.s) {
                            Circle().fill(key == "notifications.today" ? DesignColors.primary600 : DesignColors.slate300)
                                .frame(width: DesignSize.statusDot, height: DesignSize.statusDot)
                            BzrText(bzrString(key), style: DesignType.tileTitle)
                        }
                        ForEach(rows, id: \.self) { index in
                            Button(action: { onOpen(index) }) { card(index) }
                                .buttonStyle(BzrPressStyle())
                                .disabled(state.isBusy)
                        }
                    }
                }
            }
        }
    }

    private func card(_ index: Int) -> some View {
        let unread = state.itemStates[safe: index] == "unread"
        let time = age(state.orderAgeMinutes[safe: index] ?? -1) ?? state.fieldValues[safe: index] ?? ""
        return HStack(alignment: .top, spacing: DesignSpace.m) {
            BzrIcon(Self.icon(state.optionCodes[safe: index] ?? ""), tint: DesignColors.primary600)
                .frame(width: DesignSize.homeTileWell, height: DesignSize.homeTileWell)
                .background(Circle().fill(unread ? DesignColors.statusPillBg : DesignColors.surfaceContainer))
                .overlay(alignment: .topTrailing) {
                    if unread {
                        Circle().fill(DesignColors.primary400)
                            .frame(width: DesignSize.onlineDot, height: DesignSize.onlineDot)
                            .overlay(Circle().stroke(DesignColors.surface, lineWidth: DesignSize.controlStroke))
                    }
                }
            VStack(alignment: .leading, spacing: DesignSpace.xs) {
                HStack(alignment: .top, spacing: DesignSpace.s) {
                    BzrText(state.items[index], style: DesignType.tileTitle)
                    Spacer(minLength: 0)
                    if !time.isEmpty {
                        BzrText(time, style: unread ? DesignType.hint.colored(DesignColors.primary600) : DesignType.hint)
                    }
                }
                if let body = state.itemDetails[safe: index], !body.isEmpty {
                    BzrText(body, style: DesignType.secondary)
                }
            }
        }
        .padding(DesignSpace.l)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: unread ? DesignRadius.card : DesignRadius.menuIconBox)
                .fill(unread ? DesignColors.surface : DesignColors.surfaceAlt2)
        )
        .bzrShadow(unread ? DesignShadows.card : nil)
        .accessibilityElement(children: .combine)
        .accessibilityValue(unread ? bzrString("notifications.unread") : "")
    }

    private func age(_ minutes: Int) -> String? {
        switch minutes {
        case ..<0: return nil
        case ..<1: return bzrString("time.ago.now")
        case ..<60: return bzrPlural("time.ago.minutes", count: minutes)
        case ..<1440: return bzrPlural("time.ago.hours", count: minutes / 60)
        default: return bzrPlural("time.ago.days", count: minutes / 1440)
        }
    }

    /// NotificationCode → the Figma icon of that event (tag for offers, navigation on the way, receipt for money…).
    private static func icon(_ code: String) -> BzrIconKey {
        switch code {
        case "NTF-04", "NTF-21": .tag
        case "NTF-05", "NTF-16", "NTF-28", "NTF-29": .check
        case "NTF-07", "NTF-08": .navigation
        case "NTF-09": .location
        case "NTF-10", "NTF-12": .info
        case "NTF-11": .wrench
        case "NTF-13", "NTF-14", "NTF-15", "NTF-30": .receipt
        case "NTF-18": .star
        case "NTF-02", "NTF-03", "NTF-19", "NTF-20": .warning
        case "NTF-25": .messages
        default: .bell
        }
    }
}

extension Array {
    fileprivate subscript(safe index: Index) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
