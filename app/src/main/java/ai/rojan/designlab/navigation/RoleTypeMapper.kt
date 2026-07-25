package ai.rojan.designlab.navigation

import ai.rojan.designlab.domain.model.Role
import ai.rojan.designlab.domain.model.RoleType


fun RoleType.toDomainRole(): Role = when (this) {

    RoleType.CUSTOMER ->
        Role.CUSTOMER

    RoleType.BEAUTY_SPECIALIST ->
        Role.STYLIST

    RoleType.SALON_MANAGER ->
        Role.SALON_MANAGER
}