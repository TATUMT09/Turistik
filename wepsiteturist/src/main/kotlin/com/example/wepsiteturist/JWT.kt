package com.example.wepsiteturist


import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts

import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

import org.springframework.security.core.context.SecurityContextHolder

import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Component
class JwtUtil(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-expiration-ms}") private val accessExp: Long
) {

    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateAccessToken(username: String, roles: List<String>): String =
        Jwts.builder()
            .setSubject(username)
            .claim("roles", roles)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + accessExp))
            .signWith(key)
            .compact()

    fun extractUsername(token: String): String =
        getClaims(token).subject

    fun extractRoles(token: String): List<String> =
        (getClaims(token)["roles"] as? List<*>)
            ?.map { it.toString() } ?: emptyList()

    fun isValid(token: String): Boolean =
        try {
            getClaims(token)
            true
        } catch (e: Exception) {
            false
        }

    private fun getClaims(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
}



@Component
class JwtFilter(
    private val jwtUtil: JwtUtil,
    private val userDetailsService: CustomUserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain
    ) {

        val header = req.getHeader("Authorization")

        if (header?.startsWith("Bearer ") == true) {
            val token = header.substring(7)

            if (jwtUtil.isValid(token)) {
                val username = jwtUtil.extractUsername(token)
                val userDetails =
                    userDetailsService.loadUserByUsername(username)

                val auth = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                )

                SecurityContextHolder.getContext().authentication = auth
            }
        }

        chain.doFilter(req, res)
    }
}
@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:uploads/")
    }
}




