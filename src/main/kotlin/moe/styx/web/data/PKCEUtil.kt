package moe.styx.web.data

import io.ktor.util.collections.*
import java.security.SecureRandom

object PKCEUtil {
    private val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + '-' + '.' + '_' + '~'

    val generatedVerifiers = ConcurrentMap<String, String>()

    fun generateVerifier(length: Int): String {
        val random = SecureRandom()
        return (1..length)
            .map { allowedChars[random.nextInt(0, allowedChars.size)] }
            .joinToString("")
    }
}