package com.example.wepsiteturist

import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.boot.CommandLineRunner
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component


class CustomUserDetails(
    private val user: User
) : UserDetails {

    val id: Long
        get() = user.id!!

    override fun getAuthorities(): Collection<GrantedAuthority> =
        user.roles.map {
            SimpleGrantedAuthority("ROLE_${it.name.name}")
        }

    override fun getPassword(): String? = null
    override fun getUsername(): String = user.email

    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = user.enabled
}


@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            .orElseThrow {
                UsernameNotFoundException("User not found with email: $username")
            }

        return CustomUserDetails(user)
    }
}
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val jwtFilter: JwtFilter
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {

        http
            .cors { }
            .csrf { it.disable() }

            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            .authorizeHttpRequests { auth ->

                auth.requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/i/**",
                    "/api/images/*/likes",
                    "/api/images/*/comments"
                ).permitAll()

                auth.requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                auth.requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/organizations/verified").permitAll()

                auth.anyRequest().authenticated()
            }

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf("http://localhost:3000")
        config.allowedMethods = listOf("*")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
@Component
class DataInitializer(
    private val roleRepository: RoleRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {

        if (roleRepository.count() == 0L) {

            roleRepository.save(Role(RoleName.USER))
            roleRepository.save(Role(RoleName.ADMIN))
            roleRepository.save(Role(RoleName.TOUR_ORGANIZATION))

            println("✅ Default roles created")
        }
    }
}
fun generateCode(): String {
    return (100000..999999).random().toString()
}


