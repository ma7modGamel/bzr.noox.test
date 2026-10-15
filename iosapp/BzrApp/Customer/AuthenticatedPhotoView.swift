import DesignSystem
import SwiftUI
import UIKit

struct AuthenticatedPhotoView: View {
    let url: String
    let loadImage: (String) async -> Data?
    @State private var image: UIImage?
    @State private var failed = false
    @State private var revision = 0
    @State private var presented = false

    var body: some View {
        VStack(spacing: DesignSpace.s) {
            if let image {
                Button {
                    presented = true
                } label: {
                    Image(uiImage: image).resizable().scaledToFit().frame(maxWidth: .infinity).frame(height: DesignSize.mapCardHeight)
                }
                .buttonStyle(BzrPressStyle())
                .accessibilityLabel(bzrString("media.viewer.open"))
            } else if failed {
                BzrText(bzrString("media.viewer.failed"), style: DesignType.caption)
                SecondaryButton(text: bzrString("media.viewer.retry"), onClick: { revision += 1 })
            } else {
                ProgressView(bzrString("media.viewer.loading")).frame(height: DesignSize.mapCardHeight)
            }
        }
        .task(id: "\(url):\(revision)") {
            image = nil
            failed = false
            guard let data = await loadImage(url), !Task.isCancelled, let loaded = UIImage(data: data) else {
                if !Task.isCancelled { failed = true }
                return
            }
            image = loaded
        }
        .sheet(isPresented: $presented) {
            if let image { PhotoViewer(image: image) }
        }
    }
}

private struct PhotoViewer: View {
    let image: UIImage
    @Environment(\.dismiss) private var dismiss
    @State private var zoom: CGFloat = 1
    @GestureState private var pinch: CGFloat = 1

    var body: some View {
        VStack(spacing: DesignSpace.m) {
            Image(uiImage: image).resizable().scaledToFit()
                .scaleEffect(min(4, max(1, zoom * pinch)))
                .gesture(MagnificationGesture().updating($pinch) { value, state, _ in state = value }.onEnded { zoom = min(4, max(1, zoom * $0)) })
                .clipped()
                .accessibilityLabel(bzrString("media.viewer.open"))
            SecondaryButton(text: bzrString("media.viewer.close"), onClick: { dismiss() })
        }
        .padding(DesignSpace.screenHorizontal)
        .background(DesignColors.surface)
    }
}

struct ServicePhotoSection: View {
    let title: String
    let urls: [String]
    let loadImage: (String) async -> Data?
    var body: some View {
        if !urls.isEmpty {
            VStack(alignment: .leading, spacing: DesignSpace.m) {
                if !title.isEmpty { BzrText(title, style: DesignType.homeSection) }
                ForEach(urls, id: \.self) { AuthenticatedPhotoView(url: $0, loadImage: loadImage) }
            }
        }
    }
}
