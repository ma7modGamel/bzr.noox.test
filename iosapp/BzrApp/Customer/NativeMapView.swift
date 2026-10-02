import CoreLocation
import DesignSystem
import MapKit
import SwiftUI

/// Native MapKit surface used by C09 and C15. Business state stays in BzrCore.
struct NativeMapView: UIViewRepresentable {
    let selectedCoordinate: CLLocationCoordinate2D?
    let providerCoordinate: CLLocationCoordinate2D?
    let destinationCoordinate: CLLocationCoordinate2D?
    let selectable: Bool
    let onSelect: (Double, Double) -> Void

    init(
        selectedCoordinate: CLLocationCoordinate2D? = nil,
        providerCoordinate: CLLocationCoordinate2D? = nil,
        destinationCoordinate: CLLocationCoordinate2D? = nil,
        selectable: Bool = false,
        onSelect: @escaping (Double, Double) -> Void = { _, _ in }
    ) {
        self.selectedCoordinate = selectedCoordinate
        self.providerCoordinate = providerCoordinate
        self.destinationCoordinate = destinationCoordinate
        self.selectable = selectable
        self.onSelect = onSelect
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.delegate = context.coordinator
        map.showsCompass = true
        map.showsScale = false
        map.pointOfInterestFilter = .excludingAll
        if selectable {
            let gesture = UITapGestureRecognizer(
                target: context.coordinator, action: #selector(Coordinator.didTap(_:)))
            map.addGestureRecognizer(gesture)
            context.coordinator.requestLocation(for: map)
            let trackingButton = MKUserTrackingButton(mapView: map)
            trackingButton.translatesAutoresizingMaskIntoConstraints = false
            map.addSubview(trackingButton)
            NSLayoutConstraint.activate([
                trackingButton.trailingAnchor.constraint(
                    equalTo: map.trailingAnchor, constant: -DesignSpace.m),
                trackingButton.bottomAnchor.constraint(equalTo: map.bottomAnchor, constant: -DesignSpace.m),
            ])
        }
        update(map)
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        context.coordinator.parent = self
        update(map)
    }

    private func update(_ map: MKMapView) {
        map.removeAnnotations(map.annotations.filter { !($0 is MKUserLocation) })
        let annotations = [
            selectedCoordinate.map {
                MapAnnotation(coordinate: $0, title: bzrString("address.map.title"))
            },
            providerCoordinate.map {
                MapAnnotation(coordinate: $0, title: bzrString("tracking.provider_location"))
            },
            destinationCoordinate.map {
                MapAnnotation(coordinate: $0, title: bzrString("tracking.destination_location"))
            },
        ].compactMap { $0 }
        map.addAnnotations(annotations)
        if annotations.count > 1 {
            map.showAnnotations(
                annotations,
                edgePadding: UIEdgeInsets(
                    top: DesignSpace.l, left: DesignSpace.l,
                    bottom: DesignSpace.l, right: DesignSpace.l),
                animated: true)
        } else if let coordinate = annotations.first?.coordinate {
            map.setRegion(
                MKCoordinateRegion(
                    center: coordinate,
                    span: MKCoordinateSpan(latitudeDelta: 0.015, longitudeDelta: 0.015)),
                animated: true)
        } else if map.annotations.isEmpty {
            map.setRegion(
                MKCoordinateRegion(
                    center: CLLocationCoordinate2D(latitude: 31.4368, longitude: 31.6670),
                    span: MKCoordinateSpan(latitudeDelta: 0.12, longitudeDelta: 0.12)),
                animated: false)
        }
    }

    final class Coordinator: NSObject, MKMapViewDelegate, CLLocationManagerDelegate {
        var parent: NativeMapView
        private let locationManager = CLLocationManager()

        init(_ parent: NativeMapView) {
            self.parent = parent
            super.init()
            locationManager.delegate = self
        }

        func requestLocation(for map: MKMapView) {
            map.showsUserLocation = true
            if locationManager.authorizationStatus == .notDetermined {
                locationManager.requestWhenInUseAuthorization()
            }
        }

        @objc func didTap(_ gesture: UITapGestureRecognizer) {
            guard let map = gesture.view as? MKMapView, gesture.state == .ended else { return }
            let coordinate = map.convert(gesture.location(in: map), toCoordinateFrom: map)
            parent.onSelect(coordinate.latitude, coordinate.longitude)
        }
    }
}

private final class MapAnnotation: NSObject, MKAnnotation {
    let coordinate: CLLocationCoordinate2D
    let title: String?

    init(coordinate: CLLocationCoordinate2D, title: String) {
        self.coordinate = coordinate
        self.title = title
    }
}
