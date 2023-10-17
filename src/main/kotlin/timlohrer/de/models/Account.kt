package timlohrer.de.models

data class Account (
    var _id: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var password: String = "",
    var twoFactorAuth: Boolean = false,
    var twoFactorAuthKey: String = "",
    var createdAt: Long = 0,
    var lastLogin: Long = 0,
    var roles: List<String> = listOf(),
    var disabled: Boolean = false
)