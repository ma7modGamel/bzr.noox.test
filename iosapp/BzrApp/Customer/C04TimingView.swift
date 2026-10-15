import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C04, step 3 of 5 (DEC-071): the service address and when the technician comes.
public struct C04TimingView: View {
    let state: CustomerUIState
    let onOpen: (String) -> Void
    let onMaterialSelect: (Int) -> Void
    let onTimingSelect: (Int) -> Void

    public init(
        state: CustomerUIState, onOpen: @escaping (String) -> Void = { _ in },
        onTimingSelect: @escaping (Int) -> Void = { _ in },
        onMaterialSelect: @escaping (Int) -> Void = { _ in }
    ) {
        self.state = state
        self.onOpen = onOpen
        self.onTimingSelect = onTimingSelect
        self.onMaterialSelect = onMaterialSelect
    }

    private var address: String { state.fieldValues.first ?? "" }

    public var body: some View {
        CustomerScreen(title: bzrString("request.timing.title"), state: state, bottomBar: AnyView(primaryAction)) {
            RequestProgress(current: 3, total: state.requestSteps)
            HStack {
                heading(.location, bzrString("request.address.title"))
                Spacer(minLength: DesignSpace.s)
                LinkButton(text: bzrString("request.address.change"), onClick: { onOpen("SCR-C14") })
            }
            MapCard(
                title: address.isEmpty ? bzrString("request.address.required") : address,
                onMyLocation: { onOpen("SCR-C15") })
            HStack(spacing: DesignSpace.s) {
                if !address.isEmpty {
                    Button(action: { onOpen("SCR-C14") }) {
                        SelectableChip(text: address.components(separatedBy: " · ").first ?? address, state: .selected)
                    }
                    .buttonStyle(BzrPressStyle())
                }
                Button(action: { onOpen("SCR-C15") }) {
                    SelectableChip(text: bzrString("request.address.new"), state: .unselected)
                }
                .buttonStyle(BzrPressStyle())
                Spacer(minLength: 0)
            }
            LinkButton(text: bzrString("request.address.current_location"), onClick: { onOpen("SCR-C15") })
            if let message = state.messageKey { InfoBanner(text: bzrString(message)) }
            heading(.clock, bzrString("request.timing.question"))
            ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                let code = state.optionCodes.indices.contains(index) ? state.optionCodes[index] : (index == 0 ? "NOW" : "SCHEDULED")
                Button(
                    action: { code == "NOW" ? onTimingSelect(index) : onOpen("SCR-C16") },
                    label: {
                        RadioCard(
                            title: label, body: body(for: code), selected: index == state.selectedOptionIndex,
                            icon: code == "NOW" ? .bolt : .calendar,
                            badge: code == "NOW" ? bzrString(state.nowAvailable ? "request.timing.fastest" : "request.timing.unavailable.badge") : "")
                    }
                )
                .buttonStyle(BzrPressStyle())
                .disabled(code == "NOW" && !state.nowAvailable)
                .opacity(code == "NOW" && !state.nowAvailable ? 0.55 : 1)
            }
            if state.messageKey == "request.timing.check_failed" {
                LinkButton(text: bzrString("common.retry"), onClick: { onOpen("SCR-C04") })
            }
            if state.showMaterials { MaterialOptions(state: state, onSelect: onMaterialSelect) }
            InfoBanner(text: bzrString(state.showMaterials ? "request.timing.staff.note" : "request.timing.note"))
        }
    }

    private func heading(_ icon: BzrIconKey, _ title: String) -> some View {
        HStack(spacing: DesignSpace.xs) {
            BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.icon)
            ScreenHeading(title)
        }
    }

    /// The chosen slot, or a prompt — never a sample date (6ب).
    private func body(for code: String) -> String {
        if code == "NOW" {
            if state.messageKey == "request.timing.checking" { return bzrString("request.timing.checking") }
            return bzrString(state.nowAvailable ? "request.timing.now.eta" : "request.timing.now_unavailable")
        }
        let label = state.fieldValues.count > 2 ? state.fieldValues[2] : ""
        return label.isEmpty ? bzrString("request.timing.body.SCHEDULED") : label
    }

    private var primaryAction: some View {
        PrimaryButton(
            text: bzrString(state.showMaterials ? "request.step4.action" : "request.step3.action"), state: state.canContinue ? .normal : .disabled, icon: .arrowForward,
            onClick: { onOpen(state.showMaterials ? "SCR-C05" : "SCR-C38") })
    }
}
