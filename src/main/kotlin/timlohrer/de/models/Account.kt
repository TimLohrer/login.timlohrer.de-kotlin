package timlohrer.de.models

data class Account (
    var _id: String,
    var firstName: String,
    var lastName: String,
    var email: String,
    var password: String,
    var twoFactorAuth: Boolean,
    var twoFactorAuthKey: String,
    var createdAt: String,
    var lastLogin: String,
    var roles: List<String>,
    var disabled: Boolean
)