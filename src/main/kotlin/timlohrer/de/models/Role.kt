package timlohrer.de.models

import kotlinx.serialization.Serializable

@Serializable
data class Role(
    val id: String = "",
    val displayName: String = "",
    val system: Boolean = false
);