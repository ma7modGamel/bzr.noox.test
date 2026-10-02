import AVFoundation
import BzrCore
import DesignSystem
import PhotosUI
import SwiftUI
import UIKit
import UniformTypeIdentifiers

public struct C17MediaView: View {
    let state: CustomerUIState
    let onPickPhoto: () -> Void
    let onPickVideo: () -> Void
    let onRecordAudio: () -> Void
    let onDelete: (Int) -> Void
    let onRetry: (Int) -> Void
    let onDone: () -> Void
    let onUpload: (CustomerMediaUpload, String) -> Void
    let onRecordingChange: (Bool) -> Void
    @State private var photoSelection: PhotosPickerItem?
    @State private var videoSelection: PhotosPickerItem?
    @State private var showPhotoSource = false
    @State private var showPhotoPicker = false
    @State private var showVideoPicker = false
    @State private var showCamera = false
    @State private var audioCapture: NativeAudioCapture?

    public init(
        state: CustomerUIState, onPickPhoto: @escaping () -> Void = {},
        onPickVideo: @escaping () -> Void = {}, onRecordAudio: @escaping () -> Void = {},
        onDelete: @escaping (Int) -> Void = { _ in }, onRetry: @escaping (Int) -> Void = { _ in },
        onDone: @escaping () -> Void = {},
        onUpload: @escaping (CustomerMediaUpload, String) -> Void = { _, _ in },
        onRecordingChange: @escaping (Bool) -> Void = { _ in }
    ) {
        self.state = state
        self.onPickPhoto = onPickPhoto
        self.onPickVideo = onPickVideo
        self.onRecordAudio = onRecordAudio
        self.onDelete = onDelete
        self.onRetry = onRetry
        self.onDone = onDone
        self.onUpload = onUpload
        self.onRecordingChange = onRecordingChange
    }

    public var body: some View {
        CustomerScreen(title: bzrString("media.sheet.title"), state: state) {
            InfoBanner(text: bzrString("media.limits"))
            HStack(spacing: DesignSpace.m) {
                SecondaryButton(text: bzrString("media.pick.photo")) {
                    onPickPhoto()
                    showPhotoSource = true
                }
                SecondaryButton(text: bzrString("media.pick.video")) {
                    onPickVideo()
                    showVideoPicker = true
                }
            }
            SecondaryButton(
                text: bzrString(
                    state.messageKey == "media.recording" ? "media.record.stop" : "media.record.audio"),
                onClick: toggleAudioRecording)
            ForEach(Array(state.items.enumerated()), id: \.offset) { index, kind in
                let mediaState = mediaState(at: index)
                MediaThumb(
                    title: bzrString(
                        kind == "video" ? "media.video" : (kind == "audio" ? "media.audio" : "media.photo")),
                    stateLabel: bzrString(
                        mediaState == .uploading
                            ? "media.uploading" : (mediaState == .failed ? "media.failed" : "media.uploaded")),
                    kind: kind == "video" ? .video : (kind == "audio" ? .audio : .photo),
                    state: mediaState, onDelete: { onDelete(index) }
                )
                .onTapGesture { if mediaState == .failed { onRetry(index) } }
                .accessibilityHint(mediaState == .failed ? bzrString("common.retry") : "")
            }
            if let error = state.fieldErrors.first { InfoBanner(text: bzrString(error)) }
            PrimaryButton(
                text: bzrString("media.done"), state: state.canContinue ? .normal : .disabled,
                onClick: onDone)
        }
        .confirmationDialog(bzrString("media.source.title"), isPresented: $showPhotoSource) {
            Button(bzrString("media.photo")) { showPhotoPicker = true }
            if UIImagePickerController.isSourceTypeAvailable(.camera) {
                Button(bzrString("media.source.camera")) { showCamera = true }
            }
            Button(bzrString("common.cancel"), role: .cancel) {}
        }
        .photosPicker(isPresented: $showPhotoPicker, selection: $photoSelection, matching: .images)
        .photosPicker(isPresented: $showVideoPicker, selection: $videoSelection, matching: .videos)
        .sheet(isPresented: $showCamera) {
            NativeCameraPicker { image in
                guard let data = image.jpegData(compressionQuality: 0.9), data.count <= 10 * 1024 * 1024
                else { return }
                onUpload(
                    CustomerMediaUpload(fileName: "bremo-photo.jpg", mimeType: "image/jpeg", data: data),
                    "photo")
            }
        }
        .onChange(of: photoSelection) { _, item in loadPhoto(item) }
        .onChange(of: videoSelection) { _, item in loadVideo(item) }
        .onDisappear {
            audioCapture?.cancel()
            audioCapture = nil
            onRecordingChange(false)
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

    private func loadPhoto(_ item: PhotosPickerItem?) {
        guard let item else { return }
        Task {
            guard let source = try? await item.loadTransferable(type: Data.self),
                let image = UIImage(data: source),
                let data = image.jpegData(compressionQuality: 0.9),
                data.count <= 10 * 1024 * 1024
            else { return }
            onUpload(
                CustomerMediaUpload(fileName: "bremo-photo.jpg", mimeType: "image/jpeg", data: data),
                "photo")
        }
    }

    private func loadVideo(_ item: PhotosPickerItem?) {
        guard let item else { return }
        Task {
            guard let data = try? await item.loadTransferable(type: Data.self),
                data.count <= 50 * 1024 * 1024
            else { return }
            let type = item.supportedContentTypes.first ?? .mpeg4Movie
            onUpload(
                CustomerMediaUpload(
                    fileName: "bremo-video.\(type.preferredFilenameExtension ?? "mp4")",
                    mimeType: type.preferredMIMEType ?? "video/mp4", data: data),
                "video")
        }
    }

    private func toggleAudioRecording() {
        onRecordAudio()
        if let audioCapture {
            finishAudioRecording(audioCapture)
            return
        }
        NativeAudioCapture.requestPermission { granted in
            guard granted, let capture = try? NativeAudioCapture() else { return }
            audioCapture = capture
            onRecordingChange(true)
            capture.start()
            DispatchQueue.main.asyncAfter(deadline: .now() + AudioRecordingSpec.maxDurationSeconds) {
                guard audioCapture === capture else { return }
                finishAudioRecording(capture)
            }
        }
    }

    private func finishAudioRecording(_ capture: NativeAudioCapture) {
        let upload = capture.stop()
        audioCapture = nil
        onRecordingChange(false)
        if let upload { onUpload(upload, "audio") }
    }
}

private struct NativeCameraPicker: UIViewControllerRepresentable {
    let onCapture: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss

    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        picker.delegate = context.coordinator
        return picker
    }
    func updateUIViewController(_: UIImagePickerController, context _: Context) {}

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: NativeCameraPicker
        init(parent: NativeCameraPicker) { self.parent = parent }
        func imagePickerController(
            _: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            if let image = info[.originalImage] as? UIImage { parent.onCapture(image) }
            parent.dismiss()
        }
        func imagePickerControllerDidCancel(_: UIImagePickerController) { parent.dismiss() }
    }
}

private final class NativeAudioCapture: NSObject, AVAudioRecorderDelegate {
    private let url: URL
    private let recorder: AVAudioRecorder

    override init() throws {
        url = FileManager.default.temporaryDirectory
            .appendingPathComponent(
                "bremo-audio-\(UUID().uuidString).\(AudioRecordingSpec.fileExtension)")
        recorder = try AVAudioRecorder(url: url, settings: AudioRecordingSpec.settings)
        super.init()
        recorder.delegate = self
        recorder.prepareToRecord()
    }

    static func requestPermission(_ completion: @escaping (Bool) -> Void) {
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            DispatchQueue.main.async { completion(granted) }
        }
    }

    func start() {
        try? AVAudioSession.sharedInstance().setCategory(.record, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
        recorder.record(forDuration: AudioRecordingSpec.maxDurationSeconds)
    }

    func stop() -> CustomerMediaUpload? {
        recorder.stop()
        defer { try? FileManager.default.removeItem(at: url) }
        guard let data = try? Data(contentsOf: url), data.count <= AudioRecordingSpec.maxFileSizeBytes
        else { return nil }
        return CustomerMediaUpload(
            fileName: url.lastPathComponent, mimeType: AudioRecordingSpec.mimeType, data: data)
    }

    func cancel() {
        recorder.stop()
        try? FileManager.default.removeItem(at: url)
    }
}
