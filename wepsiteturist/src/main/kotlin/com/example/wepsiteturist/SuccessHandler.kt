package com.example.wepsiteturist

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component


@Component
class OAuth2SuccessHandler(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val jwtUtil: JwtUtil
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {

        val oauth = authentication as OAuth2AuthenticationToken
        val attributes = oauth.principal.attributes

        val email = attributes["email"] as String
        val name = attributes["name"] as String

        var user = userRepository.findByEmail(email).orElse(null)

        if (user == null) {
            val role = roleRepository.findByName(RoleName.USER)
                .orElseThrow { RuntimeException("ROLE USER not found") }

            user = User(
                fullName = name,
                email = email,
                phone = "UNKNOWN",
                country = "UNKNOWN",
                roles = mutableSetOf(role)
            )

            userRepository.save(user)
        }

        val roles = user.roles.map { "ROLE_${it.name.name}" }

        val token = jwtUtil.generateAccessToken(
            username = user.email,
            roles = roles
        )


        response.contentType = "application/json"
        response.writer.write("""{ "token": "$token" }""")
    }
}
fun generateToken(length: Int = 12): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { chars.random() }
        .joinToString("")
}
