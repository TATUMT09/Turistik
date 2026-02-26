package com.example.wepsiteturist

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*
import java.time.LocalDateTime

interface UserRepository : JpaRepository<User, Long> {

    fun findByEmail(email: String): Optional<User>

    fun existsByEmail(email: String): Boolean

    @Query("""
    select distinct u from User u
    join u.roles r
    where r.name in :roles
""")
    fun findAllByRole(roles: RoleName): List<User>
}


interface RoleRepository : JpaRepository<Role, Long> {

    fun findByName(name: RoleName): Optional<Role>
}

interface OrganizationRepository : JpaRepository<Organization, Long> {

    fun findAllByOwner(owner: User): List<Organization>
    fun findAllByVerifiedTrue(): List<Organization>
    fun findAllByOwnerId(ownerId: Long): List<Organization>
    fun findAllByVerifiedFalse(): List<Organization>

}


interface EventRepository : JpaRepository<Event, Long> {

    fun findAllByOrganization(organization: Organization): List<Event>

    fun findAllByEventDateTimeAfter(dateTime: LocalDateTime): List<Event>
    fun findAllByStatus(status: EventStatus): List<Event>

    fun findAllByOrganizationOwnerId(ownerId: Long): List<Event>
}

interface EventRegistrationRepository : JpaRepository<EventRegistration, Long> {

    fun existsByEventAndUser(event: Event, user: User): Boolean

    fun findAllByUser(user: User): List<EventRegistration>

    fun countByEvent(event: Event): Long
}
interface EventImageRepository : JpaRepository<EventImage, Long> {
    fun findByPublicToken(publicToken: String): EventImage?

    fun findAllByEvent(event: Event): List<EventImage>

    fun countByEvent(event: Event): Long

}
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

    fun findByToken(token: String): RefreshToken?

    fun deleteAllByUser(user: User)
}
interface ImageLikeRepository : JpaRepository<ImageLike, Long> {

    fun findByUserIdAndEventId(userId: Long, eventId: Long): ImageLike?

    fun countByEventId(eventId: Long): Long
}
interface ImageCommentRepository : JpaRepository<ImageComment, Long> {

    fun findAllByEventIdOrderByCreatedAtDesc(eventId: Long): List<ImageComment>

    fun countByEventId(eventId: Long): Long
}

interface EmailVerificationRepository :
    JpaRepository<EmailVerification, Long> {

    fun findTopByEmailAndTypeOrderByCreatedAtDesc(
        email: String,
        type: VerificationType
    ): EmailVerification?
    fun deleteAllByEmailAndType(
        email: String,
        type: VerificationType
    )

}
interface HotelRepository : JpaRepository<Hotel, Long>

interface HostelRepository : JpaRepository<Hostel, Long>
interface HotelReviewRepository : JpaRepository<HotelReview, Long> {

    fun findAllByHotelId(hotelId: Long): List<HotelReview>

    fun countByHotelId(hotelId: Long): Long

    @Query("SELECT AVG(r.rating) FROM HotelReview r WHERE r.hotel.id = :hotelId")
    fun calculateAverageRating(@Param("hotelId") hotelId: Long): Double?
}
interface AmenityRepository : JpaRepository<Amenity, Long>
interface HostelReviewRepository : JpaRepository<HostelReview, Long> {

    fun findAllByHostelId(hostelId: Long): List<HostelReview>

    fun countByHostelId(hostelId: Long): Long

    @Query("SELECT AVG(r.rating) FROM HostelReview r WHERE r.hostel.id = :hostelId")
    fun calculateAverageRating(@Param("hostelId") hostelId: Long): Double?
}

