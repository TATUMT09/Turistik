package com.example.wepsiteturist

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class AppDataInitializer(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {

        val userRole = roleRepository.findByName(RoleName.USER)
            .orElseGet { roleRepository.save(Role(RoleName.USER)) }

        val adminRole = roleRepository.findByName(RoleName.ADMIN)
            .orElseGet { roleRepository.save(Role(RoleName.ADMIN)) }
        val tourRole = roleRepository.findByName(RoleName.TOUR_ORGANIZATION)
            .orElseGet { roleRepository.save(Role(RoleName.TOUR_ORGANIZATION)) }
        val superAdminRole = roleRepository.findByName(RoleName.SUPER_ADMIN)
            .orElseGet { roleRepository.save(Role(RoleName.SUPER_ADMIN)) }

        if (!userRepository.existsByEmail("asadbek@super.com")) {

            userRepository.save(
                User(
                    fullName = "Asadbek SuperAdmin",
                    email = "asadbek@super.com",
                    phone = "998900000000",
                    country = "Uzbekistan",
                    enabled = true,
                    roles = mutableSetOf(superAdminRole)
                )
            )
        }
        val asadbekUser = userRepository.findByEmail("asadbek@user.com")
            .orElseGet {
                userRepository.save(
                    User(
                        fullName = "Asadbek User",
                        email = "asadbek@user.com",
                        phone = "884088803",
                        country = "Uzbekistan",
                        enabled = true,
                        roles = mutableSetOf(userRole)
                    )
                )
            }
        userRepository.findByEmail("asadbek@admin.com")
            .orElseGet {
                userRepository.save(
                    User(
                        fullName = "Asadbek Admin",
                        email = "asadbek@admin.com",
                        phone = "884088803",
                        country = "Uzbekistan",
                        enabled = true,
                        roles = mutableSetOf(adminRole)
                    )
                )
            }
        if (organizationRepository.count() == 0L) {

            if (!asadbekUser.roles.contains(tourRole)) {
                asadbekUser.roles.add(tourRole)
                userRepository.save(asadbekUser)
            }

            organizationRepository.save(
                Organization(
                    name = "Asadbek Grand Hotel",
                    description = "5-star luxury hotel in Tashkent",
                    address = "Tashkent",
                    phone = "884088803",
                    verified = true,
                    owner = asadbekUser
                )
            )
        }

        println("🔥 Asadbek USER, ADMIN, SUPER_ADMIN, HOTEL created successfully")
    }
}