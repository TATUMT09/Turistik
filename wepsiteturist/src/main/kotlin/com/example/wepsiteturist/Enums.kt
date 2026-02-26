package com.example.wepsiteturist

enum class RoleName {
    SUPER_ADMIN,
    ADMIN,
    TOUR_ORGANIZATION,
    USER
}
enum class VerificationType {
    REGISTER,
    LOGIN
}
enum class AccommodationType {
    HOTEL,
    HOSTEL
}
enum class EventStatus {
    PENDING,     // admin ko‘rishi kerak
    APPROVED,    // hammaga ochiq
    REJECTED     // rad etilgan
}