import BzrCore
import DesignSystem
import SwiftUI
import PhotosUI
import UIKit

/// SCR-C24 — Figma «قيّم الخدمة» 3002:4475 (DEC-076): «تم إنهاء الزيارة بنجاح», the technician card and question, the
/// overall stars (they set the three API dimensions), «تقييم التفاصيل», the quick tags, «ملاحظات إضافية» and the dock.
/// `fieldValues`: comment, provider name and job title; `items`: chosen tag indexes.
/// Optional after-repair photos are uploaded and attached to the review.
public struct C24RatingView: View {
    let state: CustomerUIState
    let onRate: (Int, Int) -> Void
    let onOverall: (Int) -> Void
    let onTag: (Int, String) -> Void
    let onCommentChange: (String) -> Void
    let onSubmit: () -> Void
    let onPhotoSelected: (CustomerMediaUpload) -> Void
    let onRemovePhoto: () -> Void
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var pickingPhoto = false

    public init(
        state: CustomerUIState,
        onRate: @escaping (Int, Int) -> Void = { _, _ in },
        onOverall: @escaping (Int) -> Void = { _ in },
        onTag: @escaping (Int, String) -> Void = { _, _ in },
        onCommentChange: @escaping (String) -> Void = { _ in },
        onSubmit: @escaping () -> Void = {},
        onPhotoSelected: @escaping (CustomerMediaUpload) -> Void = { _ in },
        onRemovePhoto: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onRate = onRate
        self.onOverall = onOverall
        self.onTag = onTag
        self.onCommentChange = onCommentChange
        self.onSubmit = onSubmit
        self.onPhotoSelected = onPhotoSelected
        self.onRemovePhoto = onRemovePhoto
    }

    private static let dimensions = ["rating.quality", "rating.punctuality", "rating.conduct"]

    private func value(_ index: Int) -> String { state.fieldValues.indices.contains(index) ? state.fieldValues[index] : "" }
    private func rating(_ index: Int) -> Int { state.ratings.indices.contains(index) ? state.ratings[index] : 0 }
    private var name: String { value(1) }

    public var body: some View {
        CustomerScreen(title: bzrString("rating.title"), state: state, bottomBar: AnyView(dock)) {
            HStack(spacing: DesignSpace.xs) {
                BzrIcon(.verified, tint: DesignColors.success, size: DesignSize.badgeIcon)
                BzrText(bzrString("rating.visit_done"), style: DesignType.statusPill.colored(DesignColors.success))
            }
            .padding(.horizontal, DesignSpace.m)
            .padding(.vertical, DesignSpace.xs)
            .background(RoundedRectangle(cornerRadius: DesignRadius.badge).fill(DesignColors.successTint))
            .frame(maxWidth: .infinity)
            providerCard
            if state.visibleActions.contains("rate") {
                card {
                    SecondaryButton(text: bzrString("rating.photos.add"), enabled: !state.isBusy && state.photoCount < 5, onClick: { pickingPhoto = true })
                        .photosPicker(isPresented: $pickingPhoto, selection: $selectedPhoto, matching: .images)
                        .onChange(of: selectedPhoto) { _, item in
                            guard let item else { return }
                            Task {
                                guard let originalData = try? await item.loadTransferable(type: Data.self),
                                    let data = UIImage(data: originalData)?.jpegData(compressionQuality: 0.9)
                                else { return }
                                onPhotoSelected(CustomerMediaUpload(fileName: "review-photo.jpg", mimeType: "image/jpeg", data: data))
                            }
                        }
                    if state.photoCount > 0 {
                        BzrText(String(format: bzrString("rating.photos.count"), String(state.photoCount)), style: DesignType.hint)
                        DangerTextButton(text: bzrString("common.delete"), onClick: onRemovePhoto).disabled(state.isBusy)
                    }
                }
            }
            if state.messageKey == "media.upload_error" { WarningBox(text: bzrString("media.upload_error")) }
            card(alignment: .center) {
                // Figma's overall row: the shared value of the three dimensions, or empty while they differ.
                let all = Set((0..<3).map(rating))
                stars(
                    selected: all.count == 1 ? all.first ?? 0 : 0, size: DesignSize.ratingStarLarge,
                    cell: DesignSize.touchTargetMin + DesignSpace.s, label: bzrString("rating.overall_hint"), onPick: onOverall)
                BzrText(bzrString("rating.overall_hint"), style: DesignType.hint, alignment: .center)
            }
            card {
                HStack(spacing: DesignSpace.s) {
                    BzrIcon(.tune, tint: DesignColors.primary600)
                    BzrText(bzrString("rating.details"), style: DesignType.homeSection)
                }
                ForEach(Array(Self.dimensions.enumerated()), id: \.offset) { index, key in
                    if index > 0 { Rectangle().fill(DesignColors.border).frame(height: DesignBorder.width) }
                    HStack {
                        BzrText(bzrString(key), style: DesignType.secondary)
                        Spacer(minLength: DesignSpace.s)
                        stars(
                            selected: rating(index), size: DesignSize.ratingStar, cell: DesignSize.touchTargetMin * 3 / 4,
                            label: bzrString(key)
                        ) { onRate(index, $0) }
                    }
                }
            }
            card {
                HStack {
                    BzrText(bzrString("rating.tags"), style: DesignType.homeSection)
                    Spacer(minLength: DesignSpace.s)
                    BzrText(bzrString("rating.optional"), style: DesignType.hint)
                }
                TagFlow(spacing: DesignSpace.s) {
                    ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                        let code = state.optionCodes.indices.contains(index) ? state.optionCodes[index] : ""
                        tagChip(label, icon: .star, selected: state.items.contains(code)) {
                            onTag(index, code)
                        }
                    }
                }
            }
            card {
                BzrText(bzrString("rating.notes"), style: DesignType.homeSection)
                TextAreaField(
                    label: "", placeholder: bzrString("rating.notes"),
                    value: value(0), state: commentError == nil ? .filled : .error,
                    error: commentError, onValueChange: onCommentChange)
            }
            if let message = state.messageKey {
                if message == "rating.success" {
                    InfoBanner(text: bzrString(message))
                } else {
                    WarningBox(text: bzrString(message))
                }
            }
        }
    }

    private var commentError: String? {
        state.fieldErrors.contains("rating.validation.comment_max") ? bzrString("rating.validation.comment_max") : nil
    }

    private var providerCard: some View {
        card(alignment: .center) {
            // BACKEND-NOTES B-16: the app loads no remote photos, so the avatar shows the first letter.
            ZStack(alignment: .bottom) {
                Circle().fill(DesignColors.surfaceContainer)
                    .frame(width: DesignSize.avatarLarge, height: DesignSize.avatarLarge)
                    .overlay(BzrText(String(name.prefix(1)), style: DesignType.profileName.colored(DesignColors.primary600)))
                if state.providerVerified {
                    BzrIcon(.check, tint: DesignColors.onPrimary, size: DesignSize.iconSmall)
                        .frame(width: DesignSize.verifiedBadge, height: DesignSize.verifiedBadge)
                        .background(Circle().fill(DesignColors.primary400))
                        .overlay(Circle().stroke(DesignColors.surface, lineWidth: DesignSize.controlStroke))
                }
            }
            if !name.isEmpty { BzrText(name, style: DesignType.offerName, alignment: .center) }
            if !value(2).isEmpty {
                HStack(spacing: DesignSpace.xs) {
                    BzrIcon(.wrench, tint: DesignColors.primary600, size: DesignSize.iconXs)
                    BzrText(value(2), style: DesignType.hint)
                }
                .padding(.horizontal, DesignSpace.s)
                .padding(.vertical, DesignSpace.xs)
                .background(Capsule().fill(DesignColors.surfaceAlt2))
            }
            BzrText(
                name.isEmpty
                    ? bzrString("rating.heading")
                    : BzrFormatter.fill(bzrString("rating.question"), values: [("name", name.components(separatedBy: " ").first ?? name)]),
                style: DesignType.screenTitle, alignment: .center)
        }
    }

    /// Five stars; each one is a 48-high touch target around the Figma glyph.
    private func stars(selected: Int, size: CGFloat, cell: CGFloat, label: String, onPick: @escaping (Int) -> Void) -> some View {
        HStack(spacing: 0) {
            ForEach(1...5, id: \.self) { star in
                Button(action: { onPick(star) }) {
                    BzrIcon(
                        star <= selected ? .starFilled : .star, label: "\(label) \(star)",
                        tint: star <= selected ? DesignColors.star : DesignColors.starEmpty, size: size
                    )
                    .frame(width: max(size, cell), height: DesignSize.touchTargetMin)
                    .contentShape(Rectangle())
                }
                .buttonStyle(BzrPressStyle())
            }
        }
    }

    private func tagChip(_ label: String, icon: BzrIconKey, selected: Bool, onClick: @escaping () -> Void) -> some View {
        Button(action: onClick) {
            HStack(spacing: DesignSpace.xs) {
                BzrIcon(selected ? .check : icon, tint: selected ? DesignColors.chipSelectedText : DesignColors.chipText, size: DesignSize.iconSmall)
                BzrText(label, style: DesignType.chip.colored(selected ? DesignColors.chipSelectedText : DesignColors.chipText))
            }
            .padding(.horizontal, DesignSpace.m)
            .frame(minHeight: DesignSize.chipHeight)
            .background(Capsule().fill(selected ? DesignColors.chipSelectedBg : DesignColors.chipBg))
            .overlay(Capsule().strokeBorder(selected ? DesignColors.chipSelectedText : DesignColors.border, lineWidth: DesignBorder.width))
            .frame(minHeight: DesignSize.touchTargetMin)
            .contentShape(Rectangle())
        }
        .buttonStyle(BzrPressStyle())
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private func card<Content: View>(
        alignment: HorizontalAlignment = .leading, @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: alignment, spacing: DesignSpace.s) { content() }
            .padding(DesignSpace.l)
            .frame(maxWidth: .infinity, alignment: alignment == .center ? .center : .leading)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
    }

    private var dock: some View {
        VStack(spacing: DesignSpace.xs) {
            if state.visibleActions.contains("rate") {
                PrimaryButton(
                    text: bzrString("rating.submit"),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled), icon: .send,
                    onClick: onSubmit
                )
                // Figma shows no validation text; VoiceOver hears why «إرسال التقييم» is disabled.
                .accessibilityHint(
                    state.fieldErrors.contains("rating.validation.required") ? bzrString("rating.validation.required") : "")
                BzrText(bzrString("rating.after_submit"), style: DesignType.hint, alignment: .center)
            }
        }
    }
}

/// Wraps the quick tags onto new lines from the leading edge (SwiftUI mirrors it for RTL).
private struct TagFlow: Layout {
    let spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        return CGSize(width: proposal.width ?? 0, height: rows(subviews, width: width).last.map { $0.y + $0.height } ?? 0)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        for row in rows(subviews, width: bounds.width) {
            for (index, x) in row.items {
                subviews[index].place(at: CGPoint(x: bounds.minX + x, y: bounds.minY + row.y), proposal: .unspecified)
            }
        }
    }

    private func rows(_ subviews: Subviews, width: CGFloat) -> [(items: [(Int, CGFloat)], y: CGFloat, height: CGFloat)] {
        var rows: [(items: [(Int, CGFloat)], y: CGFloat, height: CGFloat)] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var line: CGFloat = 0
        var items: [(Int, CGFloat)] = []
        for (index, subview) in subviews.enumerated() {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > width {
                rows.append((items, y, line))
                y += line + spacing
                x = 0
                line = 0
                items = []
            }
            items.append((index, x))
            x += size.width + spacing
            line = max(line, size.height)
        }
        if !items.isEmpty { rows.append((items, y, line)) }
        return rows
    }
}
