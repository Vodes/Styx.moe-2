package moe.styx.web.auth

import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinService
import jakarta.servlet.http.Cookie
import moe.styx.common.config.UnifiedConfig
import java.net.URI

object AuthCookie {
    fun setCurrentToken(token: String, expiresIn: Int) {
        if (token.isBlank())
            return

        VaadinService.getCurrentResponse().addCookie(createTokenCookie(token, expiresIn))
    }

    fun clearCurrentToken() {
        VaadinService.getCurrentResponse().addCookie(createTokenCookie("", 0))
    }

    private fun createTokenCookie(token: String, expiresIn: Int): Cookie {
        return Cookie("access_token", token).apply {
            path = "/"
            maxAge = expiresIn
            isHttpOnly = true
            secure = UnifiedConfig.current.base.siteBaseURL().startsWith("https://")
            setAttribute("SameSite", "Lax")
            configuredCookieDomain()?.let { domain = it }
        }
    }

    fun configuredCookieDomain(): String? {
        val host = runCatching { URI.create(UnifiedConfig.current.base.siteBaseURL()).host?.lowercase() }.getOrNull()
            ?: return null
        if (host.count { it == '.' } == 2) {
            return host.replaceBefore('.', "")
        } else if (host.count { it == '.' } == 1) {
            val splitHost = host.split(".")
            return ".${splitHost[0]}.${splitHost[1]}"
        }
        return null
    }

    fun getCurrentToken(request: VaadinRequest?): String? {
        if (request == null)
            return null
        val cookies = request.cookies
        val tokenCookie = cookies.find { it.name == "access_token" }
        if (tokenCookie != null)
            return tokenCookie.value

        return UnifiedConfig.current.webConfig.debugAuthToken
    }
}