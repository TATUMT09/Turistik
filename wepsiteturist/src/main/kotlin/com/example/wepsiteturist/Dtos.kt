package com.example.wepsiteturist

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class UserCreateDto(
    @field:NotBlank
    val fullName: String,

    @field:Email
    val email: String,

    @field:NotBlank
    val phone: String,

    @field:NotBlank
    val country: String
)

data class UserResponseDto(
    val id: Long?,
    val fullName: String?,
    val email: String,
    val phone: String?,
    val country: String?,
    val enabled: Boolean
)


data class RoleResponseDto(
    val id: Long,
    val name: RoleName
)

data class OrganizationCreateDto(
    @field:NotBlank
    val name: String,

    @field:NotBlank
    val description: String,

    @field:NotBlank
    val address: String,

    @field:NotBlank
    val phone: String
)

data class OrganizationResponseDto(
    val id: Long,
    val name: String,
    val description: String,
    val address: String,
    val phone: String,
    val verified: Boolean
)


data class EventCreateDto(
    @field:NotBlank
    val title: String,

    @field:NotBlank
    val description: String,

    @field:NotBlank
    val locationName: String,

    @field:NotNull
    val latitude: Double,

    @field:NotNull
    val longitude: Double,

    @field:NotNull
    val eventDateTime: LocalDateTime,

    @field:NotNull
    val organizationId: Long
)

data class EventUpdateDto(
    val title: String?,
    val description: String?,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val eventDateTime: LocalDateTime?
)

data class EventResponseDto(
    val id: Long,
    val title: String,
    val description: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val eventDateTime: LocalDateTime,
    val organizationId: Long,
    val organizationName: String,
    val ownerId: Long
)

data class EventRegistrationResponseDto(
    val id: Long,
    val eventId: Long,
    val userId: Long,
    val registeredAt: LocalDateTime
)

data class ApiErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val path: String
)

data class AuthResponse(
    val accessToken: String?,
    val refreshToken: String?,

)

data class EventImageResponseDto(

    val id: Long?,
    val imageUrl: String,
    val publicToken: String,
    val viewCount: Long,
    val shareCount: Long,
    val eventId: Long?
)
data class ImageCommentCreateDto(
    val text: String
)
data class ImageCommentResponseDto(
    val id: Long?,
    val text: String,
    val userId: Long?,
    val eventId: Long?,   // 🔥 o‘zgartirilgan
    val createdAt: LocalDateTime
)
data class RegisterRequest(
    val fullName: String,
    val email: String,
    val phone: String,
    val country: String
)
data class VerifyRequest(
    val email: String,
    val code: String
)
data class LoginRequest(
    val email: String
)
data class UserSimpleDto(
    val id: Long?,
    val fullName: String?,
    val email: String,
    val roles: List<String>
)

fun User.toSimpleDto() = UserSimpleDto(
    id = this.id,
    fullName = this.fullName,
    email = this.email,
    roles = this.roles.map { it.name.name }
)
data class ImageResponseDto(
    val id: Long?,
    val imageUrl: String
)
data class HotelResponseDto(
    val id: Long?,
    val name: String,
    val description: String,
    val city: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val stars: Int,
    val averageRating: Double,
    val reviewCount: Long,
    val images: List<ImageResponseDto>,
    val amenities: List<String>
)
data class HostelResponseDto(
    val id: Long?,
    val name: String,
    val description: String,
    val city: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val averageRating: Double,
    val reviewCount: Long,
    val images: List<ImageResponseDto>,
    val amenities: List<String>
)
data class HotelReviewResponseDto(
    val id: Long?,
    val rating: Double,
    val comment: String,
    val userId: Long?,
    val userName: String?,     // 🔥 front uchun qulay
    val createdAt: LocalDateTime?
)