package timlohrer.de.models

import kotlinx.serialization.Serializable

@Serializable
data class Setting(
    var openRegistration: Boolean? = null,
    val lastRestart: Long = 0
);