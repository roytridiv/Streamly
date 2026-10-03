package com.tridivroy.streamly.domain.model


/**
 * The signed-in account, persisted in DataStore so a session survives restarts.
 *
 * Streamly has no auth backend yet, so [SignInMethod] records which button was pressed and
 * [forMethod] builds the session it produces. When real auth arrives, only the callers of
 * `PreferencesRepository.signIn` change — no UI reads [forMethod].
 */
data class UserSession(
    val displayName: String,
    /** Without the leading "@". */
    val handle: String,
    val avatarUrl: String,
    val isPremium: Boolean,
    val method: SignInMethod,
    val signedInAtEpochSeconds: Long,
) {
    val initial: String get() = displayName.firstOrNull()?.uppercase() ?: "?"

    /** Browsing without an account. Still a session, so onboarding is not shown again. */
    val isGuest: Boolean get() = method == SignInMethod.Guest

    companion object {
        /**
         * The session each button produces. Guest is a real session with no identity: it exists so a
         * returning guest skips onboarding, and it carries no name, avatar or Premium badge.
         *
         * Google and Email both produce the same placeholder account until there is an identity
         * provider behind them.
         */
        fun forMethod(method: SignInMethod, nowSeconds: Long = System.currentTimeMillis() / 1000) =
            when (method) {
                SignInMethod.Guest -> UserSession(
                    displayName = "Guest",
                    handle = "guest",
                    avatarUrl = "",
                    isPremium = false,
                    method = method,
                    signedInAtEpochSeconds = nowSeconds,
                )

                SignInMethod.Google, SignInMethod.Email -> UserSession(
                    displayName = "Dev Streamly",
                    handle = "dev_streamly",
                    avatarUrl = "https://picsum.photos/seed/dev_streamly-avatar/160/160",
                    isPremium = true,
                    method = method,
                    signedInAtEpochSeconds = nowSeconds,
                )
            }
    }
}

enum class SignInMethod {
    Google,
    Email,
    Guest,
}
