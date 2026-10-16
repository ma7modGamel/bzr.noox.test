import BzrCore
import DesignSystem
import SwiftUI
import UIKit

/// SCR-C07 — Figma «Primo App» technician profile 3002:873 (DEC-073): profile card with three real stats, about, specialties,
/// portfolio, verification and reviews, then a dock with the offer price, chat and «اختر …». `fieldValues`: name,
/// rating, completed, experience, about, offer amount, review total, category; `itemStates`: portfolio captions;
/// `items`: name|stars|comment|date.
public struct C07ProviderView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    let loadImage: (String) async -> Data?
    @State private var aboutExpanded = false

    private static let reviewFields = 4
    private static let aboutLines = 4
    private static let portfolioTiles = 3
    private static let maxStars = 5

    public init(state: CustomerUIState, loadImage: @escaping (String) async -> Data? = { _ in nil }, onAction: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onAction = onAction
        self.loadImage = loadImage
    }

    private func value(_ index: Int) -> String { state.fieldValues.indices.contains(index) ? state.fieldValues[index] : "" }
    private var firstName: String { value(0).components(separatedBy: " ").first ?? value(0) }

    public var body: some View {
        CustomerScreen(title: bzrString("provider.profile.title"), state: state, bottomBar: AnyView(dock)) {
            profileCard
            if state.phase == .empty { InfoBanner(text: bzrString("provider.unavailable.title")) }
            VStack(alignment: .leading, spacing: DesignSpace.xl) {
                if !value(4).trimmingCharacters(in: .whitespaces).isEmpty { about }
                if !state.options.isEmpty { specialties }
                if !state.itemStates.isEmpty { portfolio }
                // STATIC (DEC-074, BACKEND-NOTES B-12): the API only says whether the technician is verified.
                if state.providerVerified { verification }
                if !state.items.isEmpty { reviews }
                if !state.reviewTagSummary.isEmpty {
                    section(bzrString("provider.review_tags")) {
                        ForEach(state.reviewTagSummary, id: \.self) { BzrText($0, style: DesignType.secondary) }
                    }
                }
            }
            ActionButtons(actions: state.visibleActions.filter { $0 != "accept_offer" && $0 != "chat" }, onAction: onAction)
        }
    }

    private var profileCard: some View {
        VStack(spacing: DesignSpace.s) {
            ProfileRemoteImage(url: state.providerAvatarURL, label: value(0), loadImage: loadImage) {
                BzrText(String(value(0).trimmingCharacters(in: .whitespaces).prefix(1)), style: DesignType.profileName.colored(DesignColors.primary600))
            }
            .clipShape(Circle())
            .frame(width: DesignSize.profileAvatar, height: DesignSize.profileAvatar)
            .background(Circle().fill(DesignColors.surfaceContainer))
            .overlay(alignment: .bottomLeading) {
                if state.providerVerified {
                    BzrIcon(.check, tint: DesignColors.onPrimary, size: DesignSize.iconSmall)
                        .frame(width: DesignSize.verifiedBadge, height: DesignSize.verifiedBadge)
                        .background(Circle().fill(DesignColors.verifiedText))
                        .overlay(Circle().strokeBorder(DesignColors.surface, lineWidth: DesignSize.controlStroke))
                }
            }
            .accessibilityHidden(true)
            BzrText(value(0), style: DesignType.profileName)
            HStack(spacing: DesignSpace.s) {
                if state.providerVerified && !value(7).isEmpty {
                    HStack(spacing: DesignSpace.xs) {
                        BzrIcon(.verified, tint: DesignColors.verifiedText, size: DesignSize.badgeIcon)
                        BzrText(
                            BzrFormatter.fill(bzrString("provider.verified_title"), values: [("category", value(7))]),
                            style: DesignType.statusPill.colored(DesignColors.verifiedText))
                    }
                    .padding(.horizontal, DesignSpace.s)
                    .background(RoundedRectangle(cornerRadius: DesignRadius.badge).fill(DesignColors.verifiedBg))
                }
                if state.phase != .empty {
                    HStack(spacing: DesignSpace.xs) {
                        Circle().fill(DesignColors.success).frame(width: DesignSize.statusDot, height: DesignSize.statusDot)
                        BzrText(bzrString("provider.available_now"), style: DesignType.statusPill.colored(DesignColors.slate500))
                    }
                    .padding(.horizontal, DesignSpace.s)
                    .background(Capsule().fill(DesignColors.surfaceAlt2))
                }
            }
            HStack(spacing: 0) {
                stat(value(1), "provider.stat.rating", star: true)
                stat(value(2), "provider.stat.completed")
                stat(
                    value(3).isEmpty
                        ? ""
                        : bzrPlural("provider.stat.experience.value", count: Int(value(3)) ?? 0),
                    "provider.stat.experience")

            }
            .padding(.vertical, DesignSpace.m)
            .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
        }
        .padding(DesignSpace.l)
        .frame(maxWidth: .infinity)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
    }

    private func stat(_ text: String, _ label: String, star: Bool = false) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: DesignSpace.xs) {
                if star { BzrIcon(.starFilled, tint: DesignColors.star, size: DesignSize.iconSmall) }
                BzrText(text, style: DesignType.statValue)
            }
            BzrText(bzrString(label), style: DesignType.hint, alignment: .center)
        }
        .frame(maxWidth: .infinity)
    }

    private func section<Content: View>(_ title: String, trailing: String? = nil, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) {
            HStack(spacing: DesignSpace.s) {
                Capsule().fill(DesignColors.primary400).frame(width: DesignSize.sectionBar, height: DesignSize.sectionBarHeight)
                BzrText(title, style: DesignType.homeSection)
                Spacer(minLength: DesignSpace.s)
                if let trailing {
                    BzrText(trailing, style: DesignType.hint)
                        .padding(.horizontal, DesignSpace.s)
                        .background(Capsule().fill(DesignColors.surfaceAlt2))
                }
            }
            content()
        }
    }

    private func card<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: DesignSpace.s) { content() }
            .padding(DesignSpace.l)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
    }

    private var about: some View {
        section(BzrFormatter.fill(bzrString("provider.about.named"), values: [("name", firstName)])) {
            card {
                BzrText(value(4), style: DesignType.secondary, lineLimit: aboutExpanded ? nil : Self.aboutLines)
                if !aboutExpanded {
                    Button(action: { aboutExpanded = true }) {
                        BzrText(bzrString("provider.read_more"), style: DesignType.linkSmall)
                    }
                    .buttonStyle(BzrPressStyle())
                }
            }
        }
    }

    private var specialties: some View {
        section(bzrString("provider.specialties")) {
            BzrServiceGrid(columns: 2) {
                ForEach(Array(state.options.enumerated()), id: \.offset) { _, label in
                    card {
                        HStack(spacing: DesignSpace.s) {
                            BzrIcon(.wrench, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                                .frame(width: DesignSize.orderWell, height: DesignSize.orderWell)
                                .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceContainer))
                            BzrText(label, style: DesignType.tileLabel)
                            Spacer(minLength: 0)
                        }
                    }
                }
            }
        }
    }

    private var portfolio: some View {
        section(bzrString("provider.portfolio")) {
            HStack(alignment: .top, spacing: DesignSpace.s) {
                ForEach(Array(state.itemStates.prefix(Self.portfolioTiles).enumerated()), id: \.offset) { index, caption in
                    VStack(spacing: DesignSpace.xs) {
                        ProfileRemoteImage(url: state.portfolioURLs.indices.contains(index) ? state.portfolioURLs[index] : "", label: caption, loadImage: loadImage) {
                            BzrIcon(.image, tint: DesignColors.slate400)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: DesignSize.portfolioTile)
                        .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
                        BzrText(caption, style: DesignType.hint, alignment: .center, lineLimit: 2)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private var verification: some View {
        section(bzrString("provider.verification")) {
            card {
                HStack(alignment: .top, spacing: 0) {
                    ForEach(
                        [
                            (BzrIconKey.shield, "provider.verification.identity")
                        ], id: \.1
                    ) { icon, label in
                        VStack(spacing: DesignSpace.xs) {
                            BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                                .frame(width: DesignSize.orderWell, height: DesignSize.orderWell)
                                .background(Circle().fill(DesignColors.statusPillBg))
                            BzrText(bzrString(label), style: DesignType.tileLabel, alignment: .center)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }

    private var reviews: some View {
        section(
            bzrString("provider.reviews"),
            trailing: value(6).isEmpty ? nil : bzrPlural("provider.reviews.count", count: Int(value(6)) ?? 0)
        ) {
            ForEach(Array(state.items.enumerated()), id: \.offset) { index, row in
                let fields = row.components(separatedBy: "|")
                if fields.count >= Self.reviewFields {
                    let stars = Int(fields[1]) ?? 0
                    card {
                        HStack(spacing: DesignSpace.s) {
                            BzrText(String(fields[0].prefix(1)), style: DesignType.tileTitle.colored(DesignColors.primary600))
                                .frame(minWidth: DesignSize.reviewAvatar, minHeight: DesignSize.reviewAvatar)
                                .background(Circle().fill(DesignColors.surfaceContainer))
                            VStack(alignment: .leading, spacing: 0) {
                                BzrText(fields[0], style: DesignType.tileTitle)
                                // The API supplies the review date and service area.
                                BzrText(fields[3], style: DesignType.hint)
                            }
                            Spacer(minLength: 0)
                            HStack(spacing: 0) {
                                ForEach(0..<Self.maxStars, id: \.self) { star in
                                    BzrIcon(star < stars ? .starFilled : .star, tint: DesignColors.star, size: DesignSize.iconSmall)
                                }
                            }
                            .accessibilityLabel("\(stars)/\(Self.maxStars)")
                        }
                        if !fields[2].isEmpty { BzrText(fields[2], style: DesignType.secondary) }
                        ServicePhotoSection(title: "", urls: state.imageGroups.indices.contains(index) ? state.imageGroups[index] : [], loadImage: loadImage)
                    }
                }
            }
        }
    }

    private var dock: some View {
        HStack(spacing: DesignSpace.m) {
            if !value(5).isEmpty {
                VStack(alignment: .leading, spacing: 0) {
                    BzrText(bzrString("provider.offer.price"), style: DesignType.hint)
                    BzrText(BzrFormatter.fill(bzrString("customer.home.offers.amount"), values: [("amount", value(5))]), style: DesignType.statValue)
                }
            }
            if state.visibleActions.contains("chat") {
                IconSquareButton(label: bzrString("button.chat"), onClick: { onAction("chat") })
            }
            if state.visibleActions.contains("accept_offer") {
                PrimaryButton(
                    text: BzrFormatter.fill(bzrString("provider.choose"), values: [("name", firstName)]),
                    state: state.canContinue ? .normal : .disabled, icon: .arrowForward, onClick: { onAction("accept_offer") })
            }
        }
    }
}

struct ProfileRemoteImage<Placeholder: View>: View {
    let url: String
    let label: String
    let loadImage: (String) async -> Data?
    @ViewBuilder let placeholder: () -> Placeholder
    @State private var image: UIImage?

    var body: some View {
        GeometryReader { geometry in
            Group {
                if let image { Image(uiImage: image).resizable().scaledToFill() } else { placeholder().frame(maxWidth: .infinity, maxHeight: .infinity) }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .clipped()
        }
        .accessibilityLabel(label)
        .task(id: url) {
            image = nil
            guard !url.isEmpty, let data = await loadImage(url), !Task.isCancelled else { return }
            image = UIImage(data: data)
        }
    }
}
