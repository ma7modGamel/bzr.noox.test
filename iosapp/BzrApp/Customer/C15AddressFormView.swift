import BzrCore
import CoreLocation
import DesignSystem
import SwiftUI

public struct C15AddressFormView: View {
    let state: CustomerUIState
    let editing: Bool
    let onAreaSelect: (Int) -> Void
    let onFieldChange: (String, String) -> Void
    let onDefaultChange: () -> Void
    let onLocationChange: (Double, Double) -> Void
    let onSave: () -> Void

    public init(
        state: CustomerUIState, editing: Bool = false,
        onAreaSelect: @escaping (Int) -> Void = { _ in },
        onFieldChange: @escaping (String, String) -> Void = { _, _ in },
        onLocationChange: @escaping (Double, Double) -> Void = { _, _ in },
        onDefaultChange: @escaping () -> Void = {},
        onSave: @escaping () -> Void = {}
    ) {
        self.state = state
        self.editing = editing
        self.onAreaSelect = onAreaSelect
        self.onFieldChange = onFieldChange
        self.onLocationChange = onLocationChange
        self.onDefaultChange = onDefaultChange
        self.onSave = onSave
    }

    private var values: [String] {
        state.fieldValues + Array(repeating: "", count: max(0, 6 - state.fieldValues.count))
    }

    private var areas: [String] {
        state.options
    }

    public var body: some View {
        CustomerScreen(
            title: bzrString(
                editing || state.isEditing ? "address.form.edit_title" : "address.form.add_title"),
            state: state
        ) {
            NativeMapView(
                selectedCoordinate: selectedCoordinate, selectable: true,
                onSelect: onLocationChange
            )
            .frame(height: DesignSize.mapCardHeight)
            .clipShape(RoundedRectangle(cornerRadius: DesignRadius.card))
            .accessibilityLabel(bzrString("address.map.title"))
            ScreenHeading(bzrString("address.area.title"))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DesignSpace.m) {
                    ForEach(Array(areas.enumerated()), id: \.offset) { index, area in
                        Button(
                            action: { onAreaSelect(index) },
                            label: {
                                SelectableChip(
                                    text: area,
                                    state: state.selectedOptionIndex == index
                                        || (state.selectedOptionIndex == -1 && index == 0)
                                        ? .selected : .unselected)
                            }
                        ).buttonStyle(BzrPressStyle())
                    }
                }
            }
            AddressField(
                label: "address.field.label", placeholder: "address.field.label_placeholder",
                value: values[0], errorKey: "address.validation.label_required", state: state,
                onValueChange: { onFieldChange("label", $0) })
            AddressField(
                label: "address.field.details", placeholder: "address.field.details_placeholder",
                value: values[1], errorKey: "address.validation.details_required", state: state,
                onValueChange: { onFieldChange("address_text", $0) })
            HStack(spacing: DesignSpace.m) {
                AddressField(
                    label: "address.field.building", placeholder: "address.field.building", value: values[2],
                    state: state, onValueChange: { onFieldChange("building", $0) })
                AddressField(
                    label: "address.field.floor", placeholder: "address.field.floor", value: values[3],
                    state: state, onValueChange: { onFieldChange("floor", $0) })
            }
            HStack(spacing: DesignSpace.m) {
                AddressField(
                    label: "address.field.apartment", placeholder: "address.field.apartment",
                    value: values[4], state: state, onValueChange: { onFieldChange("apartment", $0) })
                AddressField(
                    label: "address.field.landmark", placeholder: "address.field.landmark", value: values[5],
                    state: state, onValueChange: { onFieldChange("landmark", $0) })
            }
            Button(
                action: onDefaultChange,
                label: { CheckRow(text: bzrString("address.default"), checked: state.isDefault) }
            )
            .buttonStyle(BzrPressStyle())
            if state.messageKey != nil { InfoBanner(text: bzrString("address.validation.unserved_area")) }
            PrimaryButton(
                text: bzrString("common.save"),
                state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                onClick: onSave)
        }
    }

    private var selectedCoordinate: CLLocationCoordinate2D? {
        guard let latitude = state.mapLatitude, let longitude = state.mapLongitude else { return nil }
        return CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

private struct AddressField: View {
    let label: String
    let placeholder: String
    let value: String
    var errorKey: String?
    let state: CustomerUIState
    let onValueChange: (String) -> Void

    var body: some View {
        let hasError = errorKey.map(state.fieldErrors.contains) ?? false
        AppTextField(
            label: bzrString(label), placeholder: bzrString(placeholder), value: value,
            state: hasError ? .error : (value.isEmpty ? .empty : .filled),
            error: hasError ? errorKey.map(bzrString) : nil, onValueChange: onValueChange)
    }
}
