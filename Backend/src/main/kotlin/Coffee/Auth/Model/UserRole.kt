package Coffee.Auth.Model

enum class UserRole {
    CLIENTE,
    EMPLEADO,
    ADMIN;

    val authority: String
        get() = "ROLE_$name"
}
