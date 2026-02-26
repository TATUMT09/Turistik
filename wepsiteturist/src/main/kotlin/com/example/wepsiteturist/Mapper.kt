package com.example.wepsiteturist

import java.time.LocalDateTime


fun UserCreateDto.toEntity(): User =
    User(
        fullName = fullName,
        email = email,
        phone = phone,
        country = country
    )
fun User.toResponse(): UserResponseDto =
    UserResponseDto(
        id = id,
        fullName = fullName,
        email = email,
        phone = phone,
        country = country,
        enabled = enabled
    )



fun OrganizationCreateDto.toEntity(owner: User): Organization =
    Organization(
        name = name,
        description = description,
        address = address,
        phone = phone,
        owner = owner
    )

fun Organization.toResponse(): OrganizationResponseDto =
    OrganizationResponseDto(
        id = id!!,
        name = name,
        description = description,
        address = address,
        phone = phone,
        verified = verified
    )

fun EventRegistration.toResponse(): EventRegistrationResponseDto =
    EventRegistrationResponseDto(
        id = id!!,
        eventId = event.id!!,
        userId = user.id!!,
        registeredAt = registeredAt
    )


fun EventCreateDto.toEntity(organization: Organization): Event =
    Event(
        title = title,
        description = description,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        eventDateTime = eventDateTime,
        organization = organization
    )

fun Event.toResponse(): EventResponseDto =
    EventResponseDto(
        id = this.id!!,
        title = this.title,
        description = this.description,
        locationName = this.locationName,
        latitude = this.latitude,
        longitude = this.longitude,
        eventDateTime = this.eventDateTime,
        organizationId = this.organization.id!!,
        organizationName = this.organization.name,
        ownerId = this.organization.owner.id!!
    )
fun EventImage.toDto(): EventImageResponseDto {
    return EventImageResponseDto(
        id = this.id,
        imageUrl = this.imageUrl,
        publicToken = this.publicToken,
        viewCount = this.viewCount,
        shareCount = this.shareCount,
        eventId = this.event.id
    )
}
fun ImageComment.toDto(): ImageCommentResponseDto {
    return ImageCommentResponseDto(
        id = this.id,
        text = this.text,
        userId = this.user.id,
        eventId = this.event.id,   // 🔥 image emas, event
        createdAt = this.createdAt
    )
}
fun Hotel.toDto(
    averageRating: Double,
    reviewCount: Long
) = HotelResponseDto(
    id = id,
    name = name,
    description = description,
    city = city,
    address = address,
    latitude = latitude,
    longitude = longitude,
    stars = stars,
    averageRating = averageRating,
    reviewCount = reviewCount,
    images = images.map {
        ImageResponseDto(
            id = it.id,
            imageUrl = it.imageUrl
        )
    },
    amenities = amenities.map { it.name }
)
fun Hostel.toDto(
    averageRating: Double,
    reviewCount: Long
) = HostelResponseDto(
    id = id,
    name = name,
    description = description,
    city = city,
    address = address,
    latitude = latitude,
    longitude = longitude,
    averageRating = averageRating,
    reviewCount = reviewCount,
    images = images.map {
        ImageResponseDto(
            id = it.id,
            imageUrl = it.imageUrl
        )
    },
    amenities = amenities.map { it.name }
)
fun HotelReview.toDto() =
    HotelReviewResponseDto(
        id = id,
        rating = rating,
        comment = comment,
        userId = user.id,
        userName = user.fullName,
        createdAt = createdAt
    )