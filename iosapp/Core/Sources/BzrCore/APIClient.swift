import Foundation

#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif

public struct HTTPResult: Sendable {
    public let data: Data
    public let statusCode: Int

    public init(data: Data, statusCode: Int) {
        self.data = data
        self.statusCode = statusCode
    }
}

public protocol HTTPTransport: Sendable {
    func send(_ request: URLRequest) async throws -> HTTPResult
}

public struct URLSessionTransport: HTTPTransport {
    public init() {}

    public func send(_ request: URLRequest) async throws -> HTTPResult {
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let response = response as? HTTPURLResponse else {
            throw APIClientError.invalidResponse
        }
        return HTTPResult(data: data, statusCode: response.statusCode)
    }
}

public enum APIClientError: Error, Equatable {
    case invalidResponse
    case httpStatus(Int)
    /// 422 with the server's own reason (VALIDATION_FAILED, BUSINESS_RULE_VIOLATION), shown as sent (SCR-C05).
    case rejected(message: String)

    /// The server's reason for a refused request, or nil for transport and other HTTP errors.
    public var serverMessage: String? {
        if case let .rejected(message) = self { message } else { nil }
    }
}

public struct APIClient: Sendable {
    private let baseURL: URL
    private let token: String?
    private let appMode: String?
    private let transport: any HTTPTransport

    public init(
        baseURL: URL,
        token: String? = nil,
        appMode: String? = nil,
        transport: any HTTPTransport = URLSessionTransport()
    ) {
        self.baseURL = baseURL
        self.token = token
        self.appMode = appMode
        self.transport = transport
    }

    public func get<Value: Decodable & Sendable>(_ path: String, as type: Value.Type = Value.self)
        async throws -> Value
    {
        try await request(path, method: "GET", as: type)
    }

    public func post<Body: Encodable, Value: Decodable & Sendable>(
        _ path: String,
        body: Body,
        idempotencyKey: String? = nil,
        as type: Value.Type = Value.self
    ) async throws -> Value {
        try await request(
            path, method: "POST", body: try BzrJSON.encoder().encode(body),
            idempotencyKey: idempotencyKey, as: type)
    }

    public func postWithoutBody<Value: Decodable & Sendable>(
        _ path: String, as type: Value.Type = Value.self
    ) async throws -> Value {
        try await request(path, method: "POST", body: Data("{}".utf8), as: type)
    }

    public func postNoContent<Body: Encodable>(_ path: String, body: Body) async throws {
        _ = try await request(path, method: "POST", body: try BzrJSON.encoder().encode(body))
    }

    public func postNoContent(_ path: String) async throws {
        _ = try await request(path, method: "POST", body: Data("{}".utf8))
    }

    public func patch<Body: Encodable, Value: Decodable & Sendable>(
        _ path: String, body: Body, as type: Value.Type = Value.self
    ) async throws -> Value {
        try await request(path, method: "PATCH", body: try BzrJSON.encoder().encode(body), as: type)
    }

    public func put<Body: Encodable, Value: Decodable & Sendable>(
        _ path: String, body: Body, as type: Value.Type = Value.self
    ) async throws -> Value {
        try await request(path, method: "PUT", body: try BzrJSON.encoder().encode(body), as: type)
    }

    public func delete(_ path: String) async throws {
        _ = try await request(path, method: "DELETE")
    }

    public func delete<Body: Encodable>(_ path: String, body: Body) async throws {
        _ = try await request(path, method: "DELETE", body: try BzrJSON.encoder().encode(body))
    }

    public func multipart<Value: Decodable & Sendable>(
        _ path: String, field: String, fileName: String, mimeType: String, data: Data,
        as type: Value.Type = Value.self
    ) async throws -> Value {
        let boundary = "BzrBoundary\(UUID().uuidString)"
        var body = Data()
        body.append(Data("--\(boundary)\r\n".utf8))
        body.append(
            Data("Content-Disposition: form-data; name=\"\(field)\"; filename=\"\(fileName)\"\r\n".utf8))
        body.append(Data("Content-Type: \(mimeType)\r\n\r\n".utf8))
        body.append(data)
        body.append(Data("\r\n--\(boundary)--\r\n".utf8))
        let result = try await request(
            path, method: "POST", body: body,
            contentType: "multipart/form-data; boundary=\(boundary)")
        return try BzrJSON.decoder().decode(Value.self, from: result.data)
    }

    private func request<Value: Decodable & Sendable>(
        _ path: String,
        method: String,
        body: Data? = nil,
        idempotencyKey: String? = nil,
        as type: Value.Type
    ) async throws -> Value {
        let result = try await request(path, method: method, body: body, idempotencyKey: idempotencyKey)
        return try BzrJSON.decoder().decode(Value.self, from: result.data)
    }

    private func request(
        _ path: String, method: String, body: Data? = nil, idempotencyKey: String? = nil,
        contentType: String = "application/json"
    ) async throws -> HTTPResult {
        let relativePath = path.hasPrefix("/") ? String(path.dropFirst()) : path
        guard let url = URL(string: relativePath, relativeTo: baseURL) else {
            throw APIClientError.invalidResponse
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            request.httpBody = body
            request.setValue(contentType, forHTTPHeaderField: "Content-Type")
        }
        if let token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let appMode {
            request.setValue(appMode, forHTTPHeaderField: "X-App-Mode")
        }
        if let idempotencyKey {
            request.setValue(idempotencyKey, forHTTPHeaderField: "Idempotency-Key")
        }

        let result = try await transport.send(request)
        guard (200..<300).contains(result.statusCode) else {
            if result.statusCode == 422,
                let body = try? JSONSerialization.jsonObject(with: result.data) as? [String: Any],
                let error = body["error"] as? [String: Any], let message = error["message"] as? String
            {
                throw APIClientError.rejected(message: message)
            }
            throw APIClientError.httpStatus(result.statusCode)
        }
        return result
    }
}
