package com.example.ui.auth

enum class UserRole(val displayName: String) {
    ADMIN("Quản trị viên"),
    STAFF("Nhân viên")
}

object AuthManager {
    // PIN codes
    private const val ADMIN_PIN = "1987"
    private const val STAFF_PIN = "123"

    /**
     * Authenticates the user with the entered pin without leaking or logging it.
     */
    fun authenticate(pin: String): UserRole? {
        return when (pin.trim()) {
            ADMIN_PIN -> UserRole.ADMIN
            STAFF_PIN -> UserRole.STAFF
            else -> null
        }
    }
}
