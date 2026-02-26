package com.example.wepsiteturist

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.time.LocalDateTime
import org.springframework.security.core.Authentication

@Service
class UserService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {


    fun create(dto: UserCreateDto): UserResponseDto {

        if (userRepository.existsByEmail(dto.email)) {
            throw RuntimeException("Email already exists")
        }

        val userRole = roleRepository.findByName(RoleName.USER)
            .orElseThrow { RuntimeException("USER role not found") }

        val user = User(
            fullName = dto.fullName,
            email = dto.email,
            phone = dto.phone,
            country = dto.country,
            enabled = true,
            roles = mutableSetOf(userRole)
        )

        return userRepository.save(user).toResponse()
    }



    fun getById(id: Long): UserResponseDto {
        val user = userRepository.findById(id)
            .orElseThrow { RuntimeException("User not found") }

        return user.toResponse()
    }

    fun getAll(): List<UserResponseDto> =
        userRepository.findAll()
            .map { it.toResponse() }
    fun update(id: Long, dto: UserCreateDto): UserResponseDto {

        val user = userRepository.findById(id)
            .orElseThrow { RuntimeException("User not found") }


        if (user.roles.any { it.name == RoleName.SUPER_ADMIN }) {
            throw IllegalStateException("SuperAdmin cannot be modified")
        }

        user.fullName = dto.fullName
        user.phone = dto.phone
        user.country = dto.country

        return userRepository.save(user).toResponse()
    }


    fun delete(id: Long) {

        val user = userRepository.findById(id)
            .orElseThrow { RuntimeException("User not found") }


        if (user.roles.any { it.name == RoleName.SUPER_ADMIN }) {
            throw IllegalStateException("SuperAdmin cannot be deleted")
        }

        userRepository.delete(user)
    }


    fun setEnabled(id: Long, enabled: Boolean): UserResponseDto {

        val user = userRepository.findById(id)
            .orElseThrow { RuntimeException("User not found") }

        if (user.roles.any { it.name == RoleName.SUPER_ADMIN }) {
            throw IllegalStateException("SuperAdmin cannot be disabled")
        }

        user.enabled = enabled

        return userRepository.save(user).toResponse()
    }
}
@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository
) {

    fun create(dto: OrganizationCreateDto, ownerId: Long): OrganizationResponseDto {

        val owner = userRepository.findById(ownerId)
            .orElseThrow { RuntimeException("Owner not found") }

        val existing = organizationRepository.findAllByOwner(owner)
        if (existing.isNotEmpty()) {
            throw IllegalStateException("User already owns an organization")
        }

        val organization = organizationRepository.save(
            dto.toEntity(owner)
        )

        return organization.toResponse()
    }

    fun getById(id: Long): OrganizationResponseDto =
        organizationRepository.findById(id)
            .map { it.toResponse() }
            .orElseThrow { RuntimeException("Organization not found") }

    fun getAll(): List<OrganizationResponseDto> =
        organizationRepository.findAll()
            .map { it.toResponse() }

    fun getVerified(): List<OrganizationResponseDto> =
        organizationRepository.findAllByVerifiedTrue()
            .map { it.toResponse() }

    fun update(
        id: Long,
        dto: OrganizationCreateDto,
        currentUserId: Long
    ): OrganizationResponseDto {

        val organization = organizationRepository.findById(id)
            .orElseThrow { RuntimeException("Organization not found") }

        if (organization.owner.id != currentUserId) {
            throw IllegalStateException("You are not owner of this organization")
        }

        if (organization.verified) {
            throw IllegalStateException("Verified organization cannot be edited")
        }

        organization.name = dto.name
        organization.description = dto.description
        organization.address = dto.address
        organization.phone = dto.phone

        return organizationRepository.save(organization).toResponse()
    }

    fun delete(id: Long, currentUserId: Long) {

        val organization = organizationRepository.findById(id)
            .orElseThrow { RuntimeException("Organization not found") }

        if (organization.owner.id != currentUserId) {
            throw IllegalStateException("You are not owner of this organization")
        }

        organizationRepository.delete(organization)
    }

    fun setVerified(id: Long, verified: Boolean): OrganizationResponseDto {

        val organization = organizationRepository.findById(id)
            .orElseThrow { RuntimeException("Organization not found") }

        organization.verified = verified

        return organizationRepository.save(organization).toResponse()
    }

    // 🔥 ROLE BO‘YICHA KO‘RISH
    fun getOrganizations(authentication: Authentication): List<OrganizationResponseDto> {

        val currentUser = authentication.principal as CustomUserDetails

        val isAdmin = authentication.authorities.any {
            it.authority == "ROLE_ADMIN" || it.authority == "ROLE_SUPER_ADMIN"
        }

        return if (isAdmin) {
            organizationRepository.findAll().map { it.toResponse() }
        } else {
            organizationRepository.findAllByOwnerId(currentUser.id)
                .map { it.toResponse() }
        }
    }
    fun getPending(): List<OrganizationResponseDto> =
        organizationRepository.findAllByVerifiedFalse()
            .map { it.toResponse() }
}

@Service
@Transactional
class EventService(
    private val eventRepository: EventRepository,
    private val organizationRepository: OrganizationRepository
) {

    // ================= CREATE EVENT WITH IMAGES =================
    fun createWithImages(
        title: String,
        description: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        eventDateTime: String,
        organizationId: Long,
        files: List<MultipartFile>?,
        currentUserId: Long
    ): EventResponseDto {

        val parsedDateTime = LocalDateTime.parse(eventDateTime)

        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { IllegalArgumentException("Organization not found") }

        require(organization.verified) {
            "Organization is not verified yet"
        }

        require(organization.owner.id == currentUserId) {
            "You are not owner of this organization"
        }

        val event = eventRepository.save(
            Event(
                title = title,
                description = description,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                eventDateTime = parsedDateTime,
                organization = organization,
                status = EventStatus.PENDING,      // 🔥 MUHIM
                rejectionReason = null
            )
        )

        files?.let {
            validateFiles(it)

            val eventDirectory = createEventDirectory(event.id!!)

            it.forEach { file ->
                val savedPath = saveFile(file, eventDirectory, event.id!!)
                event.images.add(
                    EventImage(
                        imageUrl = savedPath,
                        event = event
                    )
                )
            }
        }

        return eventRepository.save(event).toResponse()
    }

    // ================= ADMIN APPROVE =================
    fun approve(eventId: Long) {

        val event = eventRepository.findById(eventId)
            .orElseThrow { RuntimeException("Event not found") }

        event.status = EventStatus.APPROVED
        event.rejectionReason = null
    }

    // ================= ADMIN REJECT =================
    fun reject(eventId: Long, reason: String) {

        val event = eventRepository.findById(eventId)
            .orElseThrow { RuntimeException("Event not found") }

        event.status = EventStatus.REJECTED
        event.rejectionReason = reason
    }

    // ================= ADMIN PENDING LIST =================
    fun getPending(): List<EventResponseDto> =
        eventRepository
            .findAllByStatus(EventStatus.PENDING)
            .map { it.toResponse() }

    // ================= PUBLIC GET ALL =================
    fun getAll(): List<EventResponseDto> =
        eventRepository
            .findAllByStatus(EventStatus.APPROVED)
            .map { it.toResponse() }

    // ================= OWNER MY EVENTS =================
    fun getMyEvents(userId: Long): List<EventResponseDto> =
        eventRepository
            .findAllByOrganizationOwnerId(userId)
            .map { it.toResponse() }

    // ================= GET BY ID =================
    fun getById(id: Long): EventResponseDto =
        eventRepository.findById(id)
            .map { it.toResponse() }
            .orElseThrow { NoSuchElementException("Event not found") }

    // ================= UPDATE =================
    fun update(
        id: Long,
        dto: EventUpdateDto,
        currentUserId: Long
    ): EventResponseDto {

        val event = eventRepository.findById(id)
            .orElseThrow { NoSuchElementException("Event not found") }

        require(event.organization.owner.id == currentUserId) {
            "You are not owner of this event"
        }

        require(event.status != EventStatus.APPROVED) {
            "Approved event cannot be edited"
        }

        dto.title?.let { event.title = it }
        dto.description?.let { event.description = it }
        dto.locationName?.let { event.locationName = it }
        dto.latitude?.let { event.latitude = it }
        dto.longitude?.let { event.longitude = it }
        dto.eventDateTime?.let { event.eventDateTime = it }

        event.status = EventStatus.PENDING // 🔥 qayta tasdiqlanishi kerak

        return eventRepository.save(event).toResponse()
    }

    // ================= DELETE =================
    fun delete(id: Long, currentUserId: Long) {

        val event = eventRepository.findById(id)
            .orElseThrow { NoSuchElementException("Event not found") }

        require(event.organization.owner.id == currentUserId) {
            "You are not owner of this event"
        }

        eventRepository.delete(event)
    }

    // ================= FILE VALIDATION =================
    private fun validateFiles(files: List<MultipartFile>) {

        require(files.size <= 3) {
            "Maximum 3 images allowed"
        }

        files.forEach {
            require(!it.isEmpty) { "Empty file detected" }

            val contentType = it.contentType ?: ""
            require(contentType.startsWith("image/")) {
                "Only image files are allowed"
            }

            require(it.size <= 5 * 1024 * 1024) {
                "File size must be less than 5MB"
            }
        }
    }

    // ================= CREATE DIRECTORY =================
    private fun createEventDirectory(eventId: Long): Path {
        val path = Paths.get("uploads/events/$eventId")
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
        return path
    }

    // ================= SAVE FILE =================
    private fun saveFile(
        file: MultipartFile,
        directory: Path,
        eventId: Long
    ): String {

        val fileName = UUID.randomUUID().toString() + "_" + file.originalFilename
        val filePath = directory.resolve(fileName)

        Files.copy(file.inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)

        return "/uploads/events/$eventId/$fileName"
    }
}
    @Service
class EventRegistrationService(
    private val registrationRepository: EventRegistrationRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {

    fun register(eventId: Long, userId: Long): EventRegistrationResponseDto {

        val event = eventRepository.findById(eventId)
            .orElseThrow { RuntimeException("Event not found") }

        if (event.eventDateTime.isBefore(LocalDateTime.now())) {
            throw IllegalStateException("Event already started, registration closed")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        if (registrationRepository.existsByEventAndUser(event, user)) {
            throw IllegalStateException("Already registered")
        }

        return registrationRepository.save(
            EventRegistration(event = event, user = user)
        ).toResponse()
    }


    /* READ by ID */
    fun getById(id: Long): EventRegistrationResponseDto {
        val registration = registrationRepository.findById(id)
            .orElseThrow { RuntimeException("Registration not found") }

        return registration.toResponse()
    }

    fun getAll(): List<EventRegistrationResponseDto> =
        registrationRepository.findAll()
            .map { it.toResponse() }

    fun getByUser(userId: Long): List<EventRegistrationResponseDto> {
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        return registrationRepository.findAllByUser(user)
            .map { it.toResponse() }
    }

    fun countByEvent(eventId: Long): Long {
        val event = eventRepository.findById(eventId)
            .orElseThrow { RuntimeException("Event not found") }

        return registrationRepository.countByEvent(event)
    }

    fun delete(id: Long) {
        if (!registrationRepository.existsById(id)) {
            throw RuntimeException("Registration not found")
        }
        registrationRepository.deleteById(id)
    }
}

@Service
@Transactional
class AdminOrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository
) {

    fun approve(id: Long): OrganizationResponseDto {

        val org = organizationRepository.findById(id)
            .orElseThrow { RuntimeException("Organization not found") }

        if (org.verified) {
            throw IllegalStateException("Organization already approved")
        }

        org.verified = true
        org.rejectionReason = null

        val owner = org.owner

        val tourRole = roleRepository.findByName(RoleName.TOUR_ORGANIZATION)
            .orElseThrow { RuntimeException("TOUR_ORGANIZATION role not found") }

        if (!owner.roles.contains(tourRole)) {
            owner.roles.add(tourRole)
            userRepository.save(owner)
        }

        return organizationRepository.save(org).toResponse()
    }

    fun reject(id: Long, reason: String): OrganizationResponseDto {

        if (reason.isBlank()) {
            throw IllegalArgumentException("Rejection reason must not be empty")
        }

        val org = organizationRepository.findById(id)
            .orElseThrow { RuntimeException("Organization not found") }

        org.verified = false
        org.rejectionReason = reason

        return organizationRepository.save(org).toResponse()
    }
}
@Service
@Transactional
class EventImageService(
    private val eventRepository: EventRepository,
    private val eventImageRepository: EventImageRepository
) {

    // ✅ ABSOLUTE PATH (ENG MUHIM TUZATISH)
    private val uploadRoot: Path =
        Paths.get(System.getProperty("user.dir"), "uploads", "events")

    fun uploadImages(
        eventId: Long,
        files: List<MultipartFile>,
        currentUserId: Long
    ): List<String> {

        if (files.isEmpty()) {
            throw IllegalArgumentException("Files are required")
        }

        val event = eventRepository.findById(eventId)
            .orElseThrow { RuntimeException("Event not found") }

        if (event.eventDateTime.isBefore(LocalDateTime.now())) {
            throw IllegalStateException("Event already finished")
        }

        if (event.organization.owner.id != currentUserId) {
            throw IllegalStateException("You are not owner of this event")
        }

        val existingCount = eventImageRepository.countByEvent(event)
        val total = existingCount + files.size

        if (total !in 3..4) {
            throw IllegalStateException("Event must have 3–4 images total")
        }

        // ✅ EVENT PAPKA
        val eventDir = uploadRoot.resolve(event.id.toString())

        try {
            Files.createDirectories(eventDir)
        } catch (e: Exception) {
            throw IllegalStateException("Could not create upload directory")
        }

        val savedUrls = mutableListOf<String>()

        files.forEach { file ->
            try {
                val safeFilename =
                    (file.originalFilename ?: "image.jpg")
                        .replace("\\s+".toRegex(), "_")

                val filename = "${UUID.randomUUID()}_$safeFilename"
                val path = eventDir.resolve(filename)

                // ✅ FILE SYSTEM GA YOZISH
                file.transferTo(path.toFile())

                val image = EventImage(
                    imageUrl = "/uploads/events/${event.id}/$filename",
                    event = event
                )

                eventImageRepository.save(image)
                savedUrls.add(image.imageUrl)

            } catch (e: Exception) {
                throw IllegalStateException(
                    "File upload failed: ${file.originalFilename}"
                )
            }
        }

        return savedUrls
    }

    fun getImages(eventId: Long): List<String> {
        val event = eventRepository.findById(eventId)
            .orElseThrow { RuntimeException("Event not found") }

        return eventImageRepository.findAllByEvent(event)
            .map { it.imageUrl }
    }
}

@Service
@Transactional
class RefreshTokenService(
    private val refreshRepo: RefreshTokenRepository,
    private val userRepo: UserRepository,
    @Value("\${jwt.refresh-expiration-ms}") private val refreshExp: Long
) {

    fun create(user: User): RefreshToken {
        refreshRepo.deleteAllByUser(user)

        return refreshRepo.save(
            RefreshToken(
                token = UUID.randomUUID().toString(),
                expiryDate = LocalDateTime.now().plusSeconds(refreshExp / 1000),
                user = user
            )
        )

    }

    fun validate(token: String): User {
        val refresh = refreshRepo.findByToken(token)
            ?: throw RuntimeException("Invalid refresh token")

        if (refresh.expiryDate.isBefore(LocalDateTime.now())) {
            refreshRepo.delete(refresh)
            throw RuntimeException("Refresh token expired")
        }

        return refresh.user
    }
    fun forceLogout(userId: Long) {

        val user = userRepo.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        refreshRepo.deleteAllByUser(user)
    }

}
@Service
@Transactional
class ImageLikeService(

    private val imageLikeRepository: ImageLikeRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository

) {

    fun toggleLike(eventId: Long, userId: Long): String {

        val event = eventRepository.findById(eventId)
            .orElseThrow { RuntimeException("Event not found") }

        val existingLike =
            imageLikeRepository.findByUserIdAndEventId(userId, eventId)

        return if (existingLike != null) {
            imageLikeRepository.delete(existingLike)
            "Unliked"
        } else {

            val user = userRepository.findById(userId)
                .orElseThrow { RuntimeException("User not found") }

            val like = ImageLike(
                user = user,
                event = event
            )

            imageLikeRepository.save(like)
            "Liked"
        }
    }

    fun getLikeCount(eventId: Long): Long {
        return imageLikeRepository.countByEventId(eventId)
    }
}
@Service
@Transactional
class ImageCommentService(

    private val imageCommentRepository: ImageCommentRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository

) {

    fun createComment(
        eventId: Long,
        userId: Long,
        dto: ImageCommentCreateDto
    ): ImageCommentResponseDto {

        val event = eventRepository.findById(eventId)
            .orElseThrow { RuntimeException("Event not found") }

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        val comment = ImageComment(
            text = dto.text,
            user = user,
            event = event
        )

        return imageCommentRepository.save(comment).toDto()
    }

    fun getComments(eventId: Long): List<ImageCommentResponseDto> {
        return imageCommentRepository
            .findAllByEventIdOrderByCreatedAtDesc(eventId)
            .map { it.toDto() }
    }

    fun deleteComment(commentId: Long, userId: Long) {

        val comment = imageCommentRepository.findById(commentId)
            .orElseThrow { RuntimeException("Comment not found") }

        if (comment.user.id != userId) {
            throw RuntimeException("You cannot delete this comment")
        }

        imageCommentRepository.delete(comment)
    }
}
@Service
class EmailService(
    private val mailSender: JavaMailSender
) {

    fun sendVerificationCode(email: String, code: String) {

        val message = SimpleMailMessage()
        message.setTo(email)
        message.setSubject("Turistik tasdiqlash kodi")
        message.setText("Sizning tasdiqlash kodingiz: $code")

        mailSender.send(message)
    }
}
@Service
@Transactional
class AuthService(

    private val emailVerificationRepository: EmailVerificationRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val jwtUtil: JwtUtil,
    private val refreshTokenService: RefreshTokenService

) {

    // ===============================
    // 1️⃣ REGISTER
    // ===============================
    fun register(dto: RegisterRequest) {

        if (userRepository.existsByEmail(dto.email)) {
            throw RuntimeException("Email already exists")
        }

        val code = generateCode()

        val verification = EmailVerification(
            fullName = dto.fullName,
            email = dto.email,
            phone = dto.phone,
            country = dto.country,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(5),
            type = VerificationType.REGISTER
        )

        emailVerificationRepository.save(verification)
        emailService.sendVerificationCode(dto.email, code)
    }

    // ===============================
    // 2️⃣ VERIFY REGISTER
    // ===============================
    fun verifyRegister(email: String, code: String): AuthResponse {

        val verification = emailVerificationRepository
            .findTopByEmailAndTypeOrderByCreatedAtDesc(email, VerificationType.REGISTER)
            ?: throw RuntimeException("Verification not found")

        if (verification.code != code)
            throw RuntimeException("Invalid code")

        if (verification.expiresAt.isBefore(LocalDateTime.now()))
            throw RuntimeException("Code expired")

        if (userRepository.existsByEmail(email))
            throw RuntimeException("User already exists")

        val user = User(
            fullName = verification.fullName,
            email = verification.email!!,
            phone = verification.phone,
            country = verification.country,
            enabled = true
        )

        val savedUser = userRepository.save(user)

        val roles = listOf("ROLE_USER")

        val accessToken = jwtUtil.generateAccessToken(
            username = savedUser.email,
            roles = roles
        )

        val refreshToken = refreshTokenService.create(savedUser)

        emailVerificationRepository.delete(verification)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token
        )
    }


    fun login(email: String): AuthResponse? {

        val user = userRepository.findByEmail(email)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User not found. Please register."
                )
            }

        if (!user.enabled)
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "User disabled"
            )

        val isSuperAdmin = user.roles.any {
            it.name == RoleName.SUPER_ADMIN
        }

        // ✅ SUPER ADMIN → darhol token
        if (isSuperAdmin) {

            val roles = user.roles.map { "ROLE_${it.name.name}" }

            val accessToken = jwtUtil.generateAccessToken(
                username = user.email,
                roles = roles
            )

            val refreshToken = refreshTokenService.create(user)

            return AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken.token
            )
        }

        // 🔐 Qolgan role → OTP yuboramiz

        // Eski LOGIN kodlarni o‘chiramiz
        emailVerificationRepository
            .deleteAllByEmailAndType(email, VerificationType.LOGIN)

        val code = generateCode()

        val verification = EmailVerification(
            fullName = user.fullName,
            email = user.email,
            phone = user.phone,
            country = user.country,
            code = code,
            expiresAt = LocalDateTime.now().plusMinutes(5),
            type = VerificationType.LOGIN
        )

        emailVerificationRepository.save(verification)
        emailService.sendVerificationCode(user.email, code)

        return null
    }

    // ===============================
    // 4️⃣ VERIFY LOGIN (TOKEN BERISH)
    // ===============================
    fun verifyLogin(email: String, code: String): AuthResponse {

        val verification = emailVerificationRepository
            .findTopByEmailAndTypeOrderByCreatedAtDesc(email, VerificationType.LOGIN)
            ?: throw RuntimeException("Verification not found")

        if (verification.code != code)
            throw RuntimeException("Invalid code")

        if (verification.expiresAt.isBefore(LocalDateTime.now()))
            throw RuntimeException("Code expired")

        val user = userRepository.findByEmail(email)
            .orElseThrow { RuntimeException("User not found") }

        val roles = user.roles.map { "ROLE_${it.name.name}" }

        val accessToken = jwtUtil.generateAccessToken(
            username = user.email,
            roles = roles
        )

        val refreshToken = refreshTokenService.create(user)

        emailVerificationRepository.delete(verification)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken.token
        )
    }
}
@Service
class SuperAdminService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository
) {

    // ================= SUPER ADMIN =================

    fun getUsersForSuperAdmin(): List<UserSimpleDto> =
        userRepository.findAll()
            .map { it.toSimpleDto() }

    fun getAdmins(): List<UserSimpleDto> =
        userRepository.findAllByRole(RoleName.ADMIN)
            .map { it.toSimpleDto() }

    fun getSuperAdmins(): List<UserSimpleDto> =
        userRepository.findAllByRole(RoleName.SUPER_ADMIN)
            .map { it.toSimpleDto() }

    // ================= ADMIN =================

    fun getUsersForAdmin(): List<UserSimpleDto> {

        val users = userRepository.findAll()

        return users
            .filter { user ->
                user.roles.any {
                    it.name == RoleName.USER ||
                            it.name == RoleName.TOUR_ORGANIZATION
                }
            }
            .map { it.toSimpleDto() }
    }



    fun getTourOrganizations(): List<UserSimpleDto> =
        userRepository.findAllByRole(RoleName.TOUR_ORGANIZATION)
            .map { it.toSimpleDto() }


}
@Service
@Transactional
class AccommodationService(
    private val hotelRepository: HotelRepository,
    private val hostelRepository: HostelRepository,
    private val amenityRepository: AmenityRepository,
    private val hotelReviewRepository: HotelReviewRepository,
    private val hostelReviewRepository: HostelReviewRepository
) {

    // ================= HOTEL CREATE =================

    fun createHotel(
        name: String,
        description: String,
        city: String,
        address: String,
        latitude: Double,
        longitude: Double,
        stars: Int,
        amenityIds: List<Long>?,
        files: List<MultipartFile>?
    ): HotelResponseDto {

        require(stars in 1..5) { "Stars must be between 1 and 5" }
        validateFiles(files)

        val hotel = hotelRepository.save(
            Hotel(
                name = name,
                description = description,
                city = city,
                address = address,
                latitude = latitude,
                longitude = longitude,
                stars = stars
            )
        )

        // 🔥 Amenity ulash
        amenityIds?.let {
            val amenities = amenityRepository.findAllById(it)
            hotel.amenities.addAll(amenities)
        }

        // 🔥 Image saqlash
        files?.forEach {
            val path = saveFile(it, "hotels", hotel.id!!)
            hotel.images.add(HotelImage(imageUrl = path, hotel = hotel))
        }

        return hotelRepository.save(hotel).toDto(
            averageRating = getHotelAverageRating(hotel.id!!),
            reviewCount = getHotelReviewCount(hotel.id!!)
        )
    }

    // ================= HOSTEL CREATE =================

    fun createHostel(
        name: String,
        description: String,
        city: String,
        address: String,
        latitude: Double,
        longitude: Double,
        amenityIds: List<Long>?,
        files: List<MultipartFile>?
    ): HostelResponseDto {

        validateFiles(files)

        val hostel = hostelRepository.save(
            Hostel(
                name = name,
                description = description,
                city = city,
                address = address,
                latitude = latitude,
                longitude = longitude
            )
        )

        // 🔥 Amenity ulash
        amenityIds?.let {
            val amenities = amenityRepository.findAllById(it)
            hostel.amenities.addAll(amenities)
        }

        // 🔥 Image saqlash
        files?.forEach {
            val path = saveFile(it, "hostels", hostel.id!!)
            hostel.images.add(HostelImage(imageUrl = path, hostel = hostel))
        }

        return hostelRepository.save(hostel).toDto(
            averageRating = getHostelAverageRating(hostel.id!!),
            reviewCount = getHostelReviewCount(hostel.id!!)
        )
    }

    // ================= GET ALL =================

    fun getAllHotels(): List<HotelResponseDto> =
        hotelRepository.findAll().map {
            it.toDto(
                averageRating = getHotelAverageRating(it.id!!),
                reviewCount = getHotelReviewCount(it.id!!)
            )
        }

    fun getAllHostels(): List<HostelResponseDto> =
        hostelRepository.findAll().map {
            it.toDto(
                averageRating = getHostelAverageRating(it.id!!),
                reviewCount = getHostelReviewCount(it.id!!)
            )
        }

    // ================= RATING =================

    private fun getHotelAverageRating(hotelId: Long): Double =
        hotelReviewRepository.calculateAverageRating(hotelId) ?: 0.0

    private fun getHotelReviewCount(hotelId: Long): Long =
        hotelReviewRepository.countByHotelId(hotelId)

    private fun getHostelAverageRating(hostelId: Long): Double =
        hostelReviewRepository.calculateAverageRating(hostelId) ?: 0.0

    private fun getHostelReviewCount(hostelId: Long): Long =
        hostelReviewRepository.countByHostelId(hostelId)

    // ================= FILE VALIDATION =================

    private fun validateFiles(files: List<MultipartFile>?) {
        require(files?.size ?: 0 <= 3) { "Max 3 images allowed" }

        files?.forEach {
            require(!it.isEmpty) { "Empty file detected" }
            require(it.contentType?.startsWith("image/") == true) {
                "Only image files allowed"
            }
            require(it.size <= 5 * 1024 * 1024) {
                "File must be less than 5MB"
            }
        }
    }

    // ================= FILE SAVE =================

    private fun saveFile(
        file: MultipartFile,
        folder: String,
        id: Long
    ): String {

        val directory = Paths.get("uploads/$folder/$id")
        if (!Files.exists(directory)) {
            Files.createDirectories(directory)
        }

        val fileName = UUID.randomUUID().toString() + "_" + file.originalFilename
        val filePath = directory.resolve(fileName)

        Files.copy(
            file.inputStream,
            filePath,
            StandardCopyOption.REPLACE_EXISTING
        )

        return "/uploads/$folder/$id/$fileName"
    }
}
@Service
@Transactional
class AmenityService(
    private val amenityRepository: AmenityRepository
) {

    fun create(name: String): Amenity =
        amenityRepository.save(Amenity(name))

    fun getAll(): List<Amenity> =
        amenityRepository.findAll()

    fun delete(id: Long) {
        amenityRepository.deleteById(id)
    }
}
@Service
@Transactional
class HotelReviewService(
    private val reviewRepository: HotelReviewRepository,
    private val hotelRepository: HotelRepository,
    private val userRepository: UserRepository
) {

    fun addReview(
        hotelId: Long,
        userId: Long,
        rating: Double,
        comment: String
    ): HotelReviewResponseDto {

        require(rating in 1.0..10.0) { "Rating must be between 1 and 10" }

        val hotel = hotelRepository.findById(hotelId)
            .orElseThrow { RuntimeException("Hotel not found") }

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        val review = reviewRepository.save(
            HotelReview(
                rating = rating,
                comment = comment,
                hotel = hotel,
                user = user
            )
        )

        return review.toDto()
    }

    fun getReviews(hotelId: Long): List<HotelReviewResponseDto> =
        reviewRepository.findAllByHotelId(hotelId)
            .map { it.toDto() }

    fun getAverageRating(hotelId: Long): Double =
        reviewRepository.calculateAverageRating(hotelId) ?: 0.0

    fun getReviewCount(hotelId: Long): Long =
        reviewRepository.countByHotelId(hotelId)
}