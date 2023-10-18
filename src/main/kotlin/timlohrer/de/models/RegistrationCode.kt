package timlohrer.de.models

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationCode(
    val code: String = ""
);