import BzrCore
import DesignSystem
import SwiftUI
import UIKit

public struct C34TermsView: View {
    let state: CustomerUIState

    public init(state: CustomerUIState) { self.state = state }

    public var body: some View {
        CustomerScreen(title: bzrString("terms.title"), state: state) {
            if !state.canContinue {
                InfoBanner(text: bzrString("terms.empty"))
            } else {
                BzrText(
                    BzrFormatter.fill(bzrString("terms.version_format"), values: [("version", BzrFormatter.ltrValue(state.itemDetails.first ?? ""))]), style: DesignType.caption)
                BzrText(
                    BzrFormatter.fill(bzrString("terms.effective_format"), values: [("date", BzrFormatter.ltrValue(state.itemDetails.dropFirst().first ?? ""))]),
                    style: DesignType.caption)
                LegalHTMLText(html: state.items.first ?? "")
            }
        }
    }
}

/// The API supplies sanitized HTML. Native attributed text keeps headings, lists and emphasis;
/// it does not execute scripts or expose a JavaScript bridge.
private struct LegalHTMLText: UIViewRepresentable {
    let html: String
    @Environment(\.layoutDirection) private var direction
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    func makeUIView(context: Context) -> UITextView {
        let view = UITextView()
        view.isEditable = false
        view.isScrollEnabled = false
        view.backgroundColor = .clear
        view.textContainerInset = .zero
        view.textContainer.lineFragmentPadding = 0
        view.adjustsFontForContentSizeCategory = true
        return view
    }

    func updateUIView(_ view: UITextView, context: Context) {
        let flow = direction == .rightToLeft ? "rtl" : "ltr"
        let document =
            "<html lang=\"\(AppLanguage.current)\" dir=\"\(flow)\"><body style=\"font-family: '\(DesignType.body.fontName)'; font-size: \(DesignType.body.size)px;\">\(html)</body></html>"
        guard let data = document.data(using: .utf8),
            let attributed = try? NSMutableAttributedString(
                data: data,
                options: [
                    .documentType: NSAttributedString.DocumentType.html,
                    .characterEncoding: String.Encoding.utf8.rawValue,
                ],
                documentAttributes: nil)
        else { return }
        let fullRange = NSRange(location: 0, length: attributed.length)
        attributed.enumerateAttribute(.paragraphStyle, in: fullRange) { value, range, _ in
            let paragraph =
                (value as? NSParagraphStyle)?.mutableCopy() as? NSMutableParagraphStyle
                ?? NSMutableParagraphStyle()
            paragraph.baseWritingDirection = direction == .rightToLeft ? .rightToLeft : .leftToRight
            paragraph.alignment = .natural
            attributed.addAttribute(.paragraphStyle, value: paragraph, range: range)
        }
        _ = dynamicTypeSize
        attributed.enumerateAttribute(.font, in: fullRange) { value, range, _ in
            guard let font = value as? UIFont else { return }
            attributed.addAttribute(.font, value: UIFontMetrics.default.scaledFont(for: font, compatibleWith: view.traitCollection), range: range)
        }
        view.attributedText = attributed
        view.semanticContentAttribute = direction == .rightToLeft ? .forceRightToLeft : .forceLeftToRight
    }

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UITextView, context: Context) -> CGSize? {
        guard let width = proposal.width else { return nil }
        return uiView.sizeThatFits(CGSize(width: width, height: .greatestFiniteMagnitude))
    }
}
