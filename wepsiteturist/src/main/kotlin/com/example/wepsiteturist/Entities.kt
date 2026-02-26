package com.example.wepsiteturist

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID
@MappedSuperclass
abstract class BaseEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}


@Entity
@Table(name = "users")
class User(

    @Column(nullable = true)
    var fullName: String? = null,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = true)
    var phone: String? = null,

    @Column(nullable = true)
    var country: String? = null,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableSet<Role> = mutableSetOf()

) : BaseEntity()


@Entity
@Table(name = "roles")
class Role(

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    var name: RoleName
) : BaseEntity()


@Entity
@Table(name = "organizations")
class Organization(

    @Column(nullable = false)
    var name: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,
    @Column(nullable = false)
    var address: String,
    @Column(nullable = false)
    var phone: String,
    @Column(nullable = false)
    var verified: Boolean = false,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User,
    @Column
    var rejectionReason: String? = null

) : BaseEntity()

@Entity
@Table(name = "events")
class Event(

    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,
    @Column(nullable = false)
    var locationName: String,
    @Column(nullable = false)
    var latitude: Double,
    @Column(nullable = false)
    var longitude: Double,
    @Column(nullable = false)
    var eventDateTime: LocalDateTime,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    var organization: Organization,
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true)
    var images: MutableList<EventImage> = mutableListOf(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EventStatus = EventStatus.PENDING,
    @Column(columnDefinition = "TEXT")
    var rejectionReason: String? = null


) : BaseEntity()

@Entity
@Table(name = "event_registrations")
class EventRegistration(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,
    @Column(nullable = false)
    var registeredAt: LocalDateTime = LocalDateTime.now()

) : BaseEntity()

@Entity
@Table(name = "event_images")
class EventImage(

    @Column(nullable = false)
    var imageUrl: String,

    @Column(unique = true, nullable = false)
    var publicToken: String = UUID.randomUUID().toString(),

    var viewCount: Long = 0,
    var shareCount: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event

) : BaseEntity()


@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Column(nullable = false, unique = true)
    var token: String,
    @Column(nullable = false)
    var expiryDate: LocalDateTime,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User

) : BaseEntity()

@Entity
@Table(
    name = "event_likes",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "event_id"])
    ]
)
class ImageLike(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event

) : BaseEntity()
@Entity
@Table(name = "event_comments")
class ImageComment(

    @Column(nullable = false, length = 1000)
    var text: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    var event: Event

) : BaseEntity()

@Entity
@Table(name = "email_verifications")
class EmailVerification(

    @Column(nullable = false)
    var fullName: String? = null,

    @Column(nullable = false)
    var email: String? = null,

    @Column(nullable = false)
    var phone: String? = null,

    @Column(nullable = false)
    var country: String? = null,

    @Column(nullable = false)
    var code: String,

    @Column(nullable = false)
    var expiresAt: LocalDateTime,
    @Enumerated(EnumType.STRING)
@Column(nullable = false)
var type: VerificationType

) : BaseEntity()

@Entity
@Table(name = "hotels")
class Hotel(

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(nullable = false)
    var city: String,

    @Column(nullable = false)
    var address: String,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    @Column(nullable = false)
    var stars: Int,

    @OneToMany(mappedBy = "hotel", cascade = [CascadeType.ALL], orphanRemoval = true)
    var images: MutableList<HotelImage> = mutableListOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "hotel_amenities",
        joinColumns = [JoinColumn(name = "hotel_id")],
        inverseJoinColumns = [JoinColumn(name = "amenity_id")]
    )
    var amenities: MutableSet<Amenity> = mutableSetOf(),

    @OneToMany(mappedBy = "hotel", cascade = [CascadeType.ALL], orphanRemoval = true)
    var reviews: MutableList<HotelReview> = mutableListOf()

) : BaseEntity()
@Entity
@Table(name = "hotel_images")
class HotelImage(

    @Column(nullable = false)
    var imageUrl: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    var hotel: Hotel

) : BaseEntity()
@Entity
@Table(name = "hotel_reviews")
class HotelReview(

    @Column(nullable = false)
    var rating: Double,

    @Column(nullable = false, columnDefinition = "TEXT")
    var comment: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    var hotel: Hotel,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User

) : BaseEntity()
@Entity
@Table(name = "hostels")
class Hostel(

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(nullable = false)
    var city: String,

    @Column(nullable = false)
    var address: String,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    @OneToMany(mappedBy = "hostel", cascade = [CascadeType.ALL], orphanRemoval = true)
    var images: MutableList<HostelImage> = mutableListOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "hostel_amenities",
        joinColumns = [JoinColumn(name = "hostel_id")],
        inverseJoinColumns = [JoinColumn(name = "amenity_id")]
    )
    var amenities: MutableSet<Amenity> = mutableSetOf(),

    @OneToMany(mappedBy = "hostel", cascade = [CascadeType.ALL], orphanRemoval = true)
    var reviews: MutableList<HostelReview> = mutableListOf()

) : BaseEntity()
@Entity
@Table(name = "hostel_images")
class HostelImage(

    @Column(nullable = false)
    var imageUrl: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id")
    var hostel: Hostel

) : BaseEntity()
@Entity
@Table(name = "hostel_reviews")
class HostelReview(

    @Column(nullable = false)
    var rating: Double,

    @Column(nullable = false, columnDefinition = "TEXT")
    var comment: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hostel_id")
    var hostel: Hostel,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User

) : BaseEntity()
@Entity
@Table(name = "amenities")
class Amenity(

    @Column(nullable = false, unique = true)
    var name: String

) : BaseEntity()
