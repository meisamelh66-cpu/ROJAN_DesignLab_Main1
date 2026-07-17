package ai.rojan.designlab.di

import ai.rojan.designlab.data.local.roleDataStore
import ai.rojan.designlab.data.repository.RoleRepositoryImpl
import ai.rojan.designlab.domain.repository.RoleRepository
import ai.rojan.designlab.domain.usecase.ObserveSelectedRoleUseCase
import ai.rojan.designlab.domain.usecase.SaveSelectedRoleUseCase
import android.content.Context

/**
 * Minimal manual composition root for the Role Selection flow.
 *
 * This project has no dependency-injection framework wired up yet, so
 * dependencies are constructed by hand here rather than duplicated at
 * every ViewModel-factory call site. Still Clean Architecture — every
 * ViewModel only ever sees a use case, never [RoleRepository] or
 * DataStore directly — just without a DI container doing the wiring.
 */
object RoleModule {

    private fun repository(context: Context): RoleRepository =
        RoleRepositoryImpl(context.applicationContext.roleDataStore)

    fun saveSelectedRoleUseCase(context: Context): SaveSelectedRoleUseCase =
        SaveSelectedRoleUseCase(repository(context))

    fun observeSelectedRoleUseCase(context: Context): ObserveSelectedRoleUseCase =
        ObserveSelectedRoleUseCase(repository(context))
}
