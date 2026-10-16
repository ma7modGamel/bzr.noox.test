import BzrCore
import DesignSystem
import SwiftUI

/// The shared blocks of Figma «إنهاء الخدمة والدفع» 3002:4134 (DEC-076) for SCR-C21 (payment) and SCR-C23 (confirm):
/// the completion hero, the provider and job card, and the invoice. Before/after photos are left out — the API has none
/// and a stock picture would misrepresent the customer's own job (BACKEND-NOTES B-05).
enum FinishServiceBlocks {
    static func amount(_ value: String) -> String {
        BzrFormatter.fill(bzrString("customer.home.offers.amount"), values: [("amount", value)])
    }

    static func hero(name: String) -> some View {
        VStack(spacing: DesignSpace.xs) {
            ZStack {
                Circle().fill(DesignColors.successTint)
                Circle().fill(DesignColors.success).frame(width: DesignSize.successCore, height: DesignSize.successCore)
                BzrIcon(.check, tint: DesignColors.onPrimary)
            }
            .frame(width: DesignSize.successRing, height: DesignSize.successRing)
            HStack(spacing: DesignSpace.xs) {
                Circle().fill(DesignColors.success).frame(width: DesignSize.statusDot, height: DesignSize.statusDot)
                BzrText(bzrString("completion.done"), style: DesignType.statusPill.colored(DesignColors.success))
            }
            .padding(.horizontal, DesignSpace.s)
            .background(Capsule().fill(DesignColors.successTint))
            if !name.isEmpty {
                BzrText(
                    BzrFormatter.fill(bzrString("completion.heading"), values: [("name", name.components(separatedBy: " ").first ?? name)]),
                    style: DesignType.screenTitle, alignment: .center)
            }
            BzrText(bzrString("completion.body"), style: DesignType.subtitle, alignment: .center)
        }
        .frame(maxWidth: .infinity)
    }

    static func providerCard(name: String, verified: Bool, title: String, address: String) -> some View {
        card {
            HStack {
                BzrText(name, style: DesignType.offerName)
                Spacer(minLength: DesignSpace.s)
                if verified {
                    BzrText(bzrString("completion.certified"), style: DesignType.statusPill.colored(DesignColors.verifiedText))
                        .padding(.horizontal, DesignSpace.s)
                        .background(Capsule().fill(DesignColors.verifiedBg))
                }
            }
            if !title.isEmpty { BzrText(title, style: DesignType.secondary) }
            if !address.isEmpty {
                HStack(spacing: DesignSpace.xs) {
                    BzrIcon(.location, tint: DesignColors.slate400, size: DesignSize.iconSmall)
                    BzrText(address, style: DesignType.hint)
                }
            }
        }
    }

    static func invoice(labor: String, materials: String, total: String) -> some View {
        card {
            HStack(spacing: DesignSpace.s) {
                BzrIcon(.receipt, tint: DesignColors.decorativeIcon, size: DesignSize.iconSmall)
                BzrText(bzrString("completion.invoice"), style: DesignType.homeSection)
            }
            line(bzrString("proposal.base"), amount(labor), style: DesignType.tileTitle)
            if !materials.isEmpty && materials != "0" {
                line(bzrString("completion.extra"), amount(materials), style: DesignType.tileTitle)
            }
            Rectangle().fill(DesignColors.border).frame(height: DesignBorder.width)
            line(bzrString("completion.total"), amount(total), style: DesignType.amountDisplay.colored(DesignColors.amountText))
        }
    }

    static func problemLink(_ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: DesignSpace.xs) {
                BzrIcon(.info, tint: DesignColors.slate500, size: DesignSize.iconSmall)
                BzrText(bzrString("completion.problem"), style: DesignType.caption)
            }
            .frame(minHeight: DesignSize.touchTargetMin)
        }
        .buttonStyle(BzrPressStyle())
    }

    private static func line(_ label: String, _ value: String, style: BzrTextStyle) -> some View {
        HStack {
            BzrText(label, style: DesignType.caption)
            Spacer(minLength: DesignSpace.s)
            BzrText(value, style: style)
        }
    }

    private static func card<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) { content() }
            .padding(DesignSpace.l)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
    }
}
