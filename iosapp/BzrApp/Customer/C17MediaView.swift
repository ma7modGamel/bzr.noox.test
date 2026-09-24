import BzrCore
import DesignSystem
import SwiftUI

public struct C17MediaView: View {
    let state: CustomerUIState
    let onPickPhoto: () -> Void
    let onPickVideo: () -> Void
    let onRecordAudio: () -> Void
    let onDelete: (Int) -> Void
    let onDone: () -> Void

    public init(
        state: CustomerUIState, onPickPhoto: @escaping () -> Void = {},
        onPickVideo: @escaping () -> Void = {}, onRecordAudio: @escaping () -> Void = {},
        onDelete: @escaping (Int) -> Void = { _ in }, onDone: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onPickPhoto = onPickPhoto
        self.onPickVideo = onPickVideo
        self.onRecordAudio = onRecordAudio
        self.onDelete = onDelete
        self.onDone = onDone
    }

    public var body: some View {
        CustomerScreen(title: bzrString("media.sheet.title"), state: state) {
            InfoBanner(text: bzrString("media.limits"))
            HStack(spacing: DesignSpace.m) {
                SecondaryButton(text: bzrString("media.pick.photo"), onClick: onPickPhoto)
                SecondaryButton(text: bzrString("media.pick.video"), onClick: onPickVideo)
            }
            SecondaryButton(
                text: bzrString(state.messageKey == "media.recording" ? "media.record.stop" : "media.record.audio"),
                onClick: onRecordAudio)
            ForEach(Array(state.items.enumerated()), id: \.offset) { index, kind in
                let mediaState = mediaState(at: index)
                MediaThumb(
                    title: bzrString(kind == "video" ? "media.video" : (kind == "audio" ? "media.audio" : "media.photo")),
                    stateLabel: bzrString(
                        mediaState == .uploading ? "media.uploading" : (mediaState == .failed ? "media.failed" : "media.uploaded")),
                    kind: kind == "video" ? .video : (kind == "audio" ? .audio : .photo),
                    state: mediaState, onDelete: { onDelete(index) })
            }
            if let error = state.fieldErrors.first { InfoBanner(text: bzrString(error)) }
            PrimaryButton(
                text: bzrString("media.done"), state: state.canContinue ? .normal : .disabled,
                onClick: onDone)
        }
    }

    private func mediaState(at index: Int) -> MediaState {
        guard state.itemStates.indices.contains(index) else { return .uploaded }
        switch state.itemStates[index] {
        case "uploading": return .uploading
        case "failed": return .failed
        default: return .uploaded
        }
    }
}
