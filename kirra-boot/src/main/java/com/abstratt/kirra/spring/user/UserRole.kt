package com.abstratt.kirra.spring.user

import com.abstratt.kirra.pojo.*
import com.abstratt.kirra.spring.*
import com.abstratt.kirra.spring.userprofile.UserProfile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import javax.persistence.ManyToOne
import javax.persistence.MappedSuperclass
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Role

@MappedSuperclass
abstract class RoleEntity(
    id : Long?,
    @ManyToOne
    var user : UserProfile? = null
) : BaseEntity(id), IRoleEntity {
}

abstract class RoleEntityService<RE : RoleEntity, RR : RoleRepository<RE>>(entityClass : KClass<RE>) : BaseService<RE, RR>(entityClass), IRoleEntityService<RE> {
    override fun findByUser(userProfile : IUserProfile) : RE? = repository.findByUser(userProfile)
}

interface UserRole : IUserRole {
    fun toAuthorityName() : String = "ROLE_${(this as Enum<*>).name}"
    fun toGrantedAuthority() : GrantedAuthority = SimpleGrantedAuthority(toAuthorityName())
}

@NoRepositoryBean
interface RoleRepository<E : RoleEntity> : BaseRepository<E> {
    fun findByUser(user : IUserProfile) : E?
}

@Service
open class RoleService {
    @Autowired
    lateinit var kirraJavaApplication: KirraJavaApplication

    @Autowired
    lateinit var kirraRepositoryRegistry: KirraRepositoryRegistry

    fun findAuthorities(user : UserProfile): List<GrantedAuthority> =
            findRoleObjects(user).map { (it.getRole() as UserRole).toGrantedAuthority() }

    fun findRoleObjects(user : UserProfile): List<RoleEntity> =
            kirraJavaApplication.applicationUserRoles.map { asRole<RoleEntity>(user, it) }.filterNotNull()

    fun <RE: RoleEntity> asRole(user : UserProfile, role : UserRole) : RE? = asRole(user, role.roleName())

    fun <RE: RoleEntity> asRole(user : UserProfile, roleEntityClass : KClass<RE>) : RE? = asRole(user, roleEntityClass.simpleName!!)

    private fun <RE : RoleEntity> asRole(user: UserProfile, roleName: String): RE? {
        val roleRepository = kirraRepositoryRegistry.getRepository<RoleEntity>(roleName) as? RoleRepository<RE>
        assert(roleRepository != null)
        val found = roleRepository?.findByUser(user)
        return found
    }

    fun toUserDetails(user : UserProfile): KirraUserDetails =
            KirraUserDetails(user.id!!, user.username!!, user.readPassword()!!, findAuthorities(user))
}



