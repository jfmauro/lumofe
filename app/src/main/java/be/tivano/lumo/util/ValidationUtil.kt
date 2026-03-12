package be.tivano.lumo.util

object ValidationUtil {

    private val NAME_PATTERN = Regex("^[a-zA-ZÀ-ÿ\\s\\-']{2,50}$")
    private val EMAIL_PATTERN = Regex("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")
    private val PHONE_PATTERN = Regex("^\\+?[1-9]\\d{9,14}$")

    fun isValidFirstName(value: String): Boolean = NAME_PATTERN.matches(value.trim())
    fun isValidLastName(value: String): Boolean = NAME_PATTERN.matches(value.trim())
    fun isValidEmail(value: String): Boolean = EMAIL_PATTERN.matches(value.trim().lowercase())
    fun isValidPhone(value: String): Boolean {
        if (value.isBlank()) return true
        return PHONE_PATTERN.matches(value.replace("\\s".toRegex(), ""))
    }

    fun isFormValid(firstName: String, lastName: String, email: String, phone: String): Boolean {
        return isValidFirstName(firstName)
                && isValidLastName(lastName)
                && isValidEmail(email)
                && isValidPhone(phone)
    }
}
