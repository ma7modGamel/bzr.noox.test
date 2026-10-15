import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C18 — Figma «الرسائل» 3002:2924 (DEC-076): the bell in the top bar, a local search, «الكل» and «طلبات نشطة»
/// chips, one card per conversation, the support entry, then «أمانك وحقوقك أولوية». `items`: provider names;
/// `itemDetails`: "category • status"; `itemStates`: conversation status; `options`: last messages; `fieldValues`: times;
/// `fieldErrors`: "verified"; `optionCodes`: "mine" when the last message is the customer's. Unread counts and availability come from the API; opening a chat marks only its fetched incoming messages read.
public struct C18MessagesView: View {
    let state: CustomerUIState
    let onSelect: (Int) -> Void
    let onOpen: (String) -> Void
    @State private var query = ""
    @State private var activeOnly = false
    @State private var unreadOnly = false

    public init(state: CustomerUIState, onSelect: @escaping (Int) -> Void = { _ in }, onOpen: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onSelect = onSelect
        self.onOpen = onOpen
    }

    private func value(_ values: [String], _ index: Int) -> String { values.indices.contains(index) ? values[index] : "" }

    private var shown: [Int] {
        let trimmed = query.trimmingCharacters(in: .whitespaces)
        return state.items.indices.filter { index in
            (!activeOnly || value(state.itemStates, index) == "OPEN")
                && (!unreadOnly || unread(index) > 0)
                && (trimmed.isEmpty
                    || [state.items, state.itemDetails, state.options].contains {
                        value($0, index).localizedCaseInsensitiveContains(trimmed)
                    })
        }
    }

    public var body: some View {
        CustomerScreen(
            title: bzrString("messages.title"), state: state,
            topAction: (.bell, bzrString("notifications.title"), { onOpen("SCR-C32") })
        ) {
            if state.phase == .empty {
                EmptyState(title: bzrString("messages.empty.title"), body: bzrString("messages.empty.body"))
            } else {
                SearchField(placeholder: bzrString("messages.search"), value: query) { query = $0 }
                HStack(spacing: DesignSpace.s) {
                    chip(bzrString("messages.filter.all"), count: state.items.count, selected: !activeOnly && !unreadOnly) {
                        activeOnly = false; unreadOnly = false
                    }
                    chip(bzrString("messages.filter.active"), count: nil, selected: activeOnly) {
                        activeOnly = true; unreadOnly = false
                    }
                    chip(bzrString("messages.filter.unread"), count: state.unreadCounts.reduce(0, +), selected: unreadOnly) {
                        unreadOnly = true; activeOnly = false
                    }
                    Spacer(minLength: 0)
                }
                if shown.isEmpty {
                    BzrText(bzrString("messages.no_results"), style: DesignType.secondary, alignment: .center)
                        .frame(maxWidth: .infinity)
                }
                ForEach(shown, id: \.self) { index in
                    let message = value(state.options, index)
                    Button(action: { onSelect(index) }) {
                        card(
                            avatar: AnyView(initialAvatar(String(state.items[index].prefix(1)), available: available(index))), name: state.items[index],
                            verified: value(state.fieldErrors, index) == "verified", badge: unread(index) > 0 ? BzrFormat.number(unread(index)) : nil,
                            meta: value(state.itemDetails, index) + (available(index) ? " · " + bzrString("provider.available_now") : ""),
                            message: message.isEmpty ? bzrString("messages.no_messages") : message,
                            time: value(state.fieldValues, index),
                            tick: value(state.optionCodes, index) == "mine" && !message.isEmpty)
                    }
                    .buttonStyle(BzrPressStyle())
                }
                if query.isEmpty && !activeOnly && !unreadOnly {
                    // Figma's official support conversation opens the help screen (C29); it is not an API conversation.
                    Button(action: { onOpen("SCR-C29") }) {
                        card(
                            avatar: AnyView(
                                Circle().fill(DesignColors.surfaceContainer)
                                    .overlay(BzrIcon(.shield, tint: DesignColors.primary600))),
                            name: bzrString("messages.support.name"), verified: false,
                            badge: bzrString("messages.support.badge"),
                            meta: bzrString("messages.support.subtitle"),
                            message: bzrString("messages.support.body"), time: "", tick: false)
                    }
                    .buttonStyle(BzrPressStyle())
                }
                safetyNote
            }
        }
    }

    private func unread(_ index: Int) -> Int { state.unreadCounts.indices.contains(index) ? state.unreadCounts[index] : 0 }
    private func available(_ index: Int) -> Bool { state.providersAvailable.indices.contains(index) && state.providersAvailable[index] }

    private func initialAvatar(_ initial: String, available: Bool) -> some View {
        Circle().fill(DesignColors.surfaceContainer)
            .overlay(BzrText(initial, style: DesignType.tileTitle.colored(DesignColors.primary600)))
            .overlay(alignment: .bottomLeading) {
                if available {
                    Circle().fill(DesignColors.success)
                        .frame(width: DesignSize.onlineDot, height: DesignSize.onlineDot)
                        .overlay(Circle().stroke(DesignColors.surface, lineWidth: DesignSize.controlStroke))
                }
            }
    }

    private func card(
        avatar: AnyView, name: String, verified: Bool, badge: String?, meta: String, message: String, time: String, tick: Bool
    ) -> some View {
        HStack(alignment: .top, spacing: DesignSpace.m) {
            avatar.frame(width: DesignSize.offerAvatar, height: DesignSize.offerAvatar)
            VStack(alignment: .leading, spacing: DesignSpace.xs) {
                HStack(spacing: DesignSpace.xs) {
                    BzrText(name, style: DesignType.offerName, lineLimit: 1)
                    if verified { BzrIcon(.verified, tint: DesignColors.primary600, size: DesignSize.iconSmall) }
                    if let badge {
                        BzrText(badge, style: DesignType.statusPill)
                            .padding(.horizontal, DesignSpace.s)
                            .background(Capsule().fill(DesignColors.statusPillBg))
                    }
                    Spacer(minLength: DesignSpace.s)
                    if !time.isEmpty { BzrText(time, style: DesignType.hint) }
                }
                if !meta.isEmpty { BzrText(meta, style: DesignType.hint, lineLimit: 1) }
                HStack(spacing: DesignSpace.xs) {
                    BzrText(message, style: DesignType.secondary, lineLimit: 1)
                    Spacer(minLength: 0)
                    if tick { BzrIcon(.check, tint: DesignColors.slate300, size: DesignSize.iconSmall) }
                }
            }
        }
        .padding(DesignSpace.l)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
        .accessibilityElement(children: .combine)
    }

    private func chip(_ label: String, count: Int?, selected: Bool, onClick: @escaping () -> Void) -> some View {
        Button(action: onClick) {
            HStack(spacing: DesignSpace.xs) {
                BzrText(label, style: DesignType.tileTitle.colored(selected ? DesignColors.onPrimary : DesignColors.navy900))
                if let count {
                    BzrText(BzrFormat.number(count), style: DesignType.hint.colored(selected ? DesignColors.navy900 : DesignColors.onPrimary))
                        .frame(minWidth: DesignSize.icon, minHeight: DesignSize.icon)
                        .background(Circle().fill(selected ? DesignColors.surfaceContainer : DesignColors.primary600))
                }
            }
            .padding(.horizontal, DesignSpace.m)
            .frame(minHeight: DesignSize.chipHeight)
            .background(Capsule().fill(selected ? DesignColors.primary600 : DesignColors.surfaceAlt2))
        }
        .buttonStyle(BzrPressStyle())
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private var safetyNote: some View {
        HStack(spacing: DesignSpace.m) {
            BzrIcon(.shield, tint: DesignColors.onPrimary)
                .frame(width: DesignSize.orderWell, height: DesignSize.orderWell)
                .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.primary600))
            VStack(alignment: .leading, spacing: 0) {
                BzrText(bzrString("messages.safety.title"), style: DesignType.tileTitle)
                BzrText(bzrString("messages.safety.body"), style: DesignType.hint)
            }
            Spacer(minLength: 0)
        }
        .padding(DesignSpace.l)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.primary50Info))
    }
}
