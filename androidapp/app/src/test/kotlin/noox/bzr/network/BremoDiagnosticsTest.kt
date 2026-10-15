package noox.bzr.network

import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class BremoDiagnosticsTest {
    @Test fun expectedNetworkAndUserErrorsAreNotReportedAsAppDefects() {
        assertNull(safeFailure(CancellationException("cancelled")))
        assertNull(safeFailure(NetworkException(NetworkFailureKind.TIMEOUT, "https://example.test?token=secret", IOException())))
        assertNull(safeFailure(ApiException(422, ApiError("VALIDATION_FAILED", "customer@example.test"))))
    }

    @Test fun unexpectedErrorsRetainTheirStackWithoutTheirMessageOrCause() {
        val original = IllegalStateException("Bearer secret-token customer@example.test", IOException("secret"))
        val safe = safeFailure(original)!!
        assertEquals("IllegalStateException", safe.message)
        assertNull(safe.cause)
        assertArrayEquals(original.stackTrace, safe.stackTrace)
    }

    @Test fun serverErrorsContainStatusWithoutCopyingResponseMessages() {
        val safe = safeFailure(ApiException(500, ApiError("SERVER_ERROR", "private details")))!!
        assertEquals("HTTP_500", safe.message)
        assertNull(safe.cause)
    }
}
