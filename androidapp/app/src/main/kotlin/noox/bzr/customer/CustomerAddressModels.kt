package noox.bzr.customer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DEC-061, 9أ stage 3 — addresses, served cities and areas, and scheduled slots (C14–C16), read from the
// recorded server answers in src/test/resources/api/customer (MobileCustomerAddressResponsesTest).
// Request bodies carry no default values: the shared Json does not encode defaults, so every field is sent.

@Serializable
data class SlotRange(val start: String, val end: String)

/** `POST addresses` / `PATCH addresses/{id}` (C15); empty optional fields are sent as empty text, as before. */
@Serializable
data class AddressBody(
    val label: String,
    @SerialName("city_id") val cityId: Int?,
    @SerialName("area_id") val areaId: Int?,
    @SerialName("address_text") val addressText: String,
    val building: String,
    val floor: String,
    val apartment: String,
    val landmark: String,
    val lat: Double?,
    val lng: Double?,
    @SerialName("is_default") val isDefault: Boolean,
)

/** "Area · address text" — the address row on C14. */
internal fun CustomerAddress.detail(): String = displayAddress?.takeIf(String::isNotBlank) ?: "${area.name.orEmpty()} · $addressText"
