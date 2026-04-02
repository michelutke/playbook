package ch.teamorg.data

expect object PushRegistration {
    fun login(userId: String)
    fun logout()
}
