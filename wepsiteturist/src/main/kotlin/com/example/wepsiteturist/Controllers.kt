package com.example.wepsiteturist

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication

@RestController
@RequestMapping("/api/admin/users")
class AdminUserController(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {

    // 👑 Faqat SUPER_ADMIN admin qila oladi
    @PostMapping("/{userId}/make-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun makeAdmin(@PathVariable userId: Long): String {

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        val adminRole = roleRepository.findByName(RoleName.ADMIN)
            .orElseThrow { RuntimeException("ADMIN role not found") }

        if (!user.roles.contains(adminRole)) {
            user.roles.add(adminRole)
            userRepository.save(user)
        }

        return "User promoted to ADMIN"
    }

    // 🛡 ADMIN va SUPER_ADMIN TOUR qila oladi
    @PostMapping("/{userId}/make-tour")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    fun makeTourOrganization(
        @PathVariable userId: Long,
        @AuthenticationPrincipal currentUser: CustomUserDetails
    ): String {

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        val current = userRepository.findById(currentUser.id)
            .orElseThrow { RuntimeException("Current user not found") }

        // ❌ ADMIN boshqa ADMIN yoki SUPER_ADMIN ni o‘zgartira olmaydi
        val targetIsAdmin = user.roles.any { it.name == RoleName.ADMIN }
        val targetIsSuper = user.roles.any { it.name == RoleName.SUPER_ADMIN }

        val currentIsAdmin = current.roles.any { it.name == RoleName.ADMIN }
        val currentIsSuper = current.roles.any { it.name == RoleName.SUPER_ADMIN }

        if (currentIsAdmin && (targetIsAdmin || targetIsSuper)) {
            throw IllegalStateException("Admin cannot modify ADMIN or SUPER_ADMIN")
        }

        val tourRole = roleRepository.findByName(RoleName.TOUR_ORGANIZATION)
            .orElseThrow { RuntimeException("TOUR_ORGANIZATION role not found") }

        if (!user.roles.contains(tourRole)) {
            user.roles.add(tourRole)
            userRepository.save(user)
        }

        return "User promoted to TOUR_ORGANIZATION"
    }
}


@RestController
@RequestMapping("/api/organizations")
class OrganizationController(
    private val organizationService: OrganizationService
) {

    // ================= CREATE =================
    @PreAuthorize("hasRole('TOUR_ORGANIZATION')")
    @PostMapping
    fun create(
        @RequestBody dto: OrganizationCreateDto,
        @AuthenticationPrincipal user: CustomUserDetails
    ): OrganizationResponseDto {
        return organizationService.create(dto, user.id)
    }

    // ================= GET ALL (ROLE BASED) =================
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','TOUR_ORGANIZATION')")
    @GetMapping
    fun getOrganizations(authentication: Authentication) =
        organizationService.getOrganizations(authentication)

    // ================= GET VERIFIED =================
    @GetMapping("/verified")
    fun getVerified() =
        organizationService.getVerified()

    // ================= GET BY ID =================
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long) =
        organizationService.getById(id)

    // ================= UPDATE =================
    @PreAuthorize("hasRole('TOUR_ORGANIZATION')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody dto: OrganizationCreateDto,
        @AuthenticationPrincipal user: CustomUserDetails
    ) =
        organizationService.update(id, dto, user.id)

    // ================= DELETE =================
    @PreAuthorize("hasRole('TOUR_ORGANIZATION')")
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: CustomUserDetails
    ) =
        organizationService.delete(id, user.id)
}

@RestController
@RequestMapping("/api/events")
class EventImageController(
    private val eventImageService: EventImageService
) {

    @PostMapping(
        value = ["/{eventId}/images"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun uploadImages(
        @PathVariable eventId: Long,
        @RequestParam("files") files: List<MultipartFile>,
        @AuthenticationPrincipal user: CustomUserDetails
    ): List<String> {

        println("🔥 CONTROLLER ISHLADI")
        println("🔥 FILES SONI = ${files.size}")

        return eventImageService.uploadImages(
            eventId = eventId,
            files = files,
            currentUserId = user.id
        )
    }

    @GetMapping("/{eventId}/images")
    fun getImages(
        @PathVariable eventId: Long
    ): List<String> =
        eventImageService.getImages(eventId)
}


@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    @PostMapping
    fun create(@Valid @RequestBody dto: UserCreateDto): UserResponseDto =
        userService.create(dto)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): UserResponseDto =
        userService.getById(id)

    @GetMapping
    fun getAll(): List<UserResponseDto> =
        userService.getAll()

    @PutMapping("/{id}")

    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody dto: UserCreateDto
    ): UserResponseDto =
        userService.update(id, dto)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) =
        userService.delete(id)

    @PatchMapping("/{id}/enabled")
    fun setEnabled(
        @PathVariable id: Long,
        @RequestParam enabled: Boolean
    ): UserResponseDto =
        userService.setEnabled(id, enabled)
}


@RestController
@RequestMapping("/api/admin/organizations")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
class AdminOrganizationController(
    private val organizationService: OrganizationService
) {

    // ================= PENDING =================
    @GetMapping("/pending")
    fun getPending() =
        organizationService.getPending()

    // ================= APPROVE =================
    @PostMapping("/{id}/approve")
    fun approve(@PathVariable id: Long) =
        organizationService.setVerified(id, true)

    // ================= REJECT =================
    @PostMapping("/{id}/reject")
    fun reject(@PathVariable id: Long) =
        organizationService.setVerified(id, false)
}
@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
class AdminEventController(
    private val eventService: EventService
) {

    // ================= PENDING LIST =================
    @GetMapping("/pending")
    fun getPending(): List<EventResponseDto> =
        eventService.getPending()

    // ================= APPROVE =================
    @PostMapping("/{id}/approve")
    fun approve(@PathVariable id: Long) {
        eventService.approve(id)
    }

    // ================= REJECT =================
    @PostMapping("/{id}/reject")
    fun reject(
        @PathVariable id: Long,
        @RequestParam reason: String
    ) {
        eventService.reject(id, reason)
    }
}

@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventService: EventService
) {

    // ================= CREATE EVENT + IMAGES =================
    @PreAuthorize("hasRole('TOUR_ORGANIZATION')")
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(
        @RequestParam title: String,
        @RequestParam description: String,
        @RequestParam locationName: String,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam eventDateTime: String,
        @RequestParam organizationId: Long,
        @RequestParam(required = false) files: List<MultipartFile>?,
        @AuthenticationPrincipal user: CustomUserDetails
    ): EventResponseDto {

        return eventService.createWithImages(
            title = title,
            description = description,
            locationName = locationName,
            latitude = latitude,
            longitude = longitude,
            eventDateTime = eventDateTime,
            organizationId = organizationId,
            files = files,
            currentUserId = user.id
        )
    }

    // ================= PUBLIC GET ALL (APPROVED) =================
    @GetMapping
    fun getAll(): List<EventResponseDto> =
        eventService.getAll()

    // ================= GET BY ID =================
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): EventResponseDto =
        eventService.getById(id)

    // ================= OWNER MY EVENTS =================
    @PreAuthorize("hasRole('TOUR_ORGANIZATION')")
    @GetMapping("/my")
    fun getMyEvents(
        @AuthenticationPrincipal user: CustomUserDetails
    ): List<EventResponseDto> =
        eventService.getMyEvents(user.id)

    // ================= UPDATE =================
    @PreAuthorize("hasRole('TOUR_ORGANIZATION')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody dto: EventUpdateDto,
        @AuthenticationPrincipal user: CustomUserDetails
    ): EventResponseDto =
        eventService.update(id, dto, user.id)

    // ================= DELETE =================
    @PreAuthorize("hasRole('TOUR_ORGANIZATION')")
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal user: CustomUserDetails
    ) {
        eventService.delete(id, user.id)
    }
}
@RestController
@RequestMapping("/api/registrations")
class EventRegistrationController(
    private val registrationService: EventRegistrationService
) {

    @PostMapping
    fun register(
        @RequestParam eventId: Long,
        @AuthenticationPrincipal user: CustomUserDetails
    ): EventRegistrationResponseDto =
        registrationService.register(eventId, user.id)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): EventRegistrationResponseDto =
        registrationService.getById(id)

    @GetMapping
    fun getAll(): List<EventRegistrationResponseDto> =
        registrationService.getAll()

    @GetMapping("/user/{userId}")
    fun getByUser(@PathVariable userId: Long): List<EventRegistrationResponseDto> =
        registrationService.getByUser(userId)

    @GetMapping("/event/{eventId}/count")
    fun countByEvent(@PathVariable eventId: Long): Long =
        registrationService.countByEvent(eventId)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) =
        registrationService.delete(id)
}

@RestController
@RequestMapping("/api/auth")
class RefreshTokenController(
    private val refreshTokenService: RefreshTokenService,
    private val jwtUtil: JwtUtil
) {

    data class RefreshRequest(val refreshToken: String)
    @PostMapping("/refresh")
    fun refresh(@RequestBody req: RefreshRequest): AuthResponse {
        val user = refreshTokenService.validate(req.refreshToken)
        val roles = user.roles.map { "ROLE_${it.name.name}" }

        val newAccessToken = jwtUtil.generateAccessToken(
            username = user.email,
            roles = roles
        )

        return AuthResponse(
            accessToken = newAccessToken,
            refreshToken = req.refreshToken
        )
    }

    @PostMapping("/force-logout/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun forceLogout(@PathVariable userId: Long) {
        refreshTokenService.forceLogout(userId)
    }

    @PostMapping("/logout")
    fun logout(@RequestBody req: RefreshRequest) {
        refreshTokenService.validate(req.refreshToken)
    }
}

@RestController
@RequestMapping("/api/auth")
class AuthController(

    private val authService: AuthService

) {

    // 🔹 1️⃣ REGISTER → code yuboradi
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest) {
        authService.register(request)
    }

    // 🔹 2️⃣ REGISTER verify → user yaratadi + token beradi
    @PostMapping("/verify-register")
    fun verifyRegister(@RequestBody request: VerifyRequest): AuthResponse {
        return authService.verifyRegister(request.email, request.code)
    }

    // 🔹 3️⃣ LOGIN → code yuboradi
    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {

        val response = authService.login(request.email)

        return if (response != null) {
            ResponseEntity.ok(response) // SUPER ADMIN
        } else {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Verification code sent to email")
        }
    }

    // 🔹 4️⃣ LOGIN verify → token beradi (user yaratmaydi!)
    @PostMapping("/verify-login")
    fun verifyLogin(@RequestBody request: VerifyRequest): AuthResponse {
        return authService.verifyLogin(request.email, request.code)
    }
}




@RestController
@RequestMapping("/i")
class PublicImageController(
    private val eventImageRepository: EventImageRepository
) {

    @GetMapping("/{token}")
    fun getPublicImage(@PathVariable token: String): EventImageResponseDto {

        val image = eventImageRepository.findByPublicToken(token)
            ?: throw RuntimeException("Image not found")

        image.viewCount++

        return image.toDto()
    }

    @PostMapping("/{token}/share")
    fun increaseShare(@PathVariable token: String) {

        val image = eventImageRepository.findByPublicToken(token)
            ?: throw RuntimeException("Image not found")

        image.shareCount++
    }
}
@RestController
@RequestMapping("/api/events")
class EventLikeController(

    private val imageLikeService: ImageLikeService
) {

    @PostMapping("/{eventId}/like")
    fun toggleLike(
        @PathVariable eventId: Long,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<String> {

        val result = imageLikeService.toggleLike(eventId, user.id)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{eventId}/likes")
    fun getLikeCount(
        @PathVariable eventId: Long
    ): ResponseEntity<Long> {

        return ResponseEntity.ok(
            imageLikeService.getLikeCount(eventId)
        )
    }
}
@RestController
@RequestMapping("/api/events")
class EventCommentController(

    private val imageCommentService: ImageCommentService
) {

    @PostMapping("/{eventId}/comments")
    fun createComment(
        @PathVariable eventId: Long,
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestBody dto: ImageCommentCreateDto
    ): ResponseEntity<ImageCommentResponseDto> {

        return ResponseEntity.ok(
            imageCommentService.createComment(eventId, user.id, dto)
        )
    }

    @GetMapping("/{eventId}/comments")
    fun getComments(
        @PathVariable eventId: Long
    ): ResponseEntity<List<ImageCommentResponseDto>> {

        return ResponseEntity.ok(
            imageCommentService.getComments(eventId)
        )
    }

    @DeleteMapping("/comments/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        @AuthenticationPrincipal user: CustomUserDetails
    ): ResponseEntity<String> {

        imageCommentService.deleteComment(commentId, user.id)
        return ResponseEntity.ok("Comment deleted")
    }
}

@RestController
@RequestMapping("/api/management")
class ManagementController(
    private val superAdminService: SuperAdminService
) {

    // ================= USERS =================

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/users")
    fun getUsers(authentication: Authentication): List<UserSimpleDto> {

        val roles = authentication.authorities.map { it.authority }

        return if (roles.contains("ROLE_SUPER_ADMIN")) {
            superAdminService.getUsersForSuperAdmin()
        } else {
            superAdminService.getUsersForAdmin()
        }
    }

    // ================= TOUR ORGANIZATIONS =================

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/tour-organizations")
    fun getTourOrganizations() =
        superAdminService.getTourOrganizations()


    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/admins")
    fun getAdmins() =
        superAdminService.getAdmins()

    // ================= SUPER ADMINS =================

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/super-admins")
    fun getSuperAdmins() =
        superAdminService.getSuperAdmins()
}
@RestController
@RequestMapping("/api/accommodations")
class AccommodationController(
    private val service: AccommodationService
) {

    // ================= HOTEL CREATE =================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/hotel", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createHotel(
        @RequestParam name: String,
        @RequestParam description: String,
        @RequestParam city: String,
        @RequestParam address: String,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam stars: Int,
        @RequestParam(required = false) amenityIds: List<Long>?,
        @RequestParam(required = false) files: List<MultipartFile>?
    ) = service.createHotel(
        name,
        description,
        city,
        address,
        latitude,
        longitude,
        stars,
        amenityIds,
        files
    )

    // ================= HOSTEL CREATE =================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/hostel", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createHostel(
        @RequestParam name: String,
        @RequestParam description: String,
        @RequestParam city: String,
        @RequestParam address: String,
        @RequestParam latitude: Double,
        @RequestParam longitude: Double,
        @RequestParam(required = false) amenityIds: List<Long>?,
        @RequestParam(required = false) files: List<MultipartFile>?
    ) = service.createHostel(
        name,
        description,
        city,
        address,
        latitude,
        longitude,
        amenityIds,
        files
    )

    // ================= GET ALL =================
    @GetMapping("/hotels")
    fun getHotels() = service.getAllHotels()

    @GetMapping("/hostels")
    fun getHostels() = service.getAllHostels()
}
@RestController
@RequestMapping("/api/amenities")
class AmenityController(
    private val amenityService: AmenityService
) {

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun create(@RequestParam name: String) =
        amenityService.create(name)

    @GetMapping
    fun getAll() =
        amenityService.getAll()

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long) =
        amenityService.delete(id)
}
@RestController
@RequestMapping("/api/hostels/{hostelId}/reviews")
class HostelReviewController(
    private val reviewService: HotelReviewService
) {

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    fun addReview(
        @PathVariable hostelId: Long,
        @AuthenticationPrincipal user: CustomUserDetails,
        @RequestParam rating: Double,
        @RequestParam comment: String
    ) = reviewService.addReview(
        hostelId,
        user.id,
        rating,
        comment
    )

    @GetMapping
    fun getReviews(@PathVariable hostelId: Long) =
        reviewService.getReviews(hostelId)

    @GetMapping("/average")
    fun getAverage(@PathVariable hostelId: Long) =
        reviewService.getAverageRating(hostelId)

    @GetMapping("/count")
    fun getCount(@PathVariable hostelId: Long) =
        reviewService.getReviewCount(hostelId)
}