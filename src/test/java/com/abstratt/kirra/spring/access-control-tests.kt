package com.abstratt.kirra.spring

import com.abstratt.kirra.Schema
import com.abstratt.kirra.spring.testing.sample.*
import com.abstratt.kirra.spring.userprofile.UserProfileService
import com.abstratt.kirra.spring.userprofile.UserProfile
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.Rollback
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.transaction.TransactionScoped

open class AccessControlTests : TestBase() {
    @Test
    fun currentUser_employee(): Unit {
        securityService.selectedUsername = employee1!!.user!!.username
        val currentUser = securityService.getCurrentUser()
        Assert.assertNotNull(currentUser)
        val roles = securityService.getCurrentUserRoles()
        Assert.assertNotNull(roles)
        Assert.assertTrue(roles!!.any { it.getRole() == SampleRole.Employee })
    }

    @Test
    fun currentUser_customer(): Unit {
        securityService.selectedUsername = customer1!!.user!!.username
        val currentUser = securityService.getCurrentUser()
        Assert.assertNotNull(currentUser)
        val roles = securityService.getCurrentUserRoles()
        Assert.assertNotNull(roles)
        Assert.assertTrue(roles!!.any { it.getRole() == SampleRole.Customer })
    }

    @Test
    fun currentUser_anonymous(): Unit {
        securityService.selectedUsername = ""
        val currentUser = securityService.getCurrentUser()
        Assert.assertNull(currentUser)
    }

    @Test
    fun customerInstanceCapabilities(): Unit {
        val customerAsInstance1 = kirraSpringBridge.toInstance(customer1!!)

        securityService.selectedUsername = customer1!!.user!!.username

        val instanceCapabilities = instanceManagement.getInstanceCapabilities(customerAsInstance1.typeRef, customerAsInstance1.objectId)
        Assert.assertEquals(sortedSetOf("Read", "Update"), instanceCapabilities.instance.toSortedSet())

        securityService.selectedUsername = customer2!!.user!!.username

        val instanceCapabilities2 = instanceManagement.getInstanceCapabilities(customerAsInstance1.typeRef, customerAsInstance1.objectId)
        Assert.assertEquals(sortedSetOf("Read"), instanceCapabilities2.instance.toSortedSet())
    }

    @Test
    fun customerEntityCapabilities(): Unit {
        securityService.selectedUsername = employee1!!.user!!.username

        val customerTypeRef = getTypeRef(Customer::class.java)
        val capabilities = instanceManagement.getEntityCapabilities(customerTypeRef)
        Assert.assertEquals(sortedSetOf("Create", "List"), capabilities.entity.toSortedSet())

        Assert.assertEquals(sortedSetOf("StaticCall"), capabilities.queries["allCustomers"]!!.toSortedSet())

        securityService.selectedUsername = customer1!!.user!!.username

        val capabilities2 = instanceManagement.getEntityCapabilities(customerTypeRef)
        Assert.assertEquals(sortedSetOf("List"), capabilities2.entity.toSortedSet())

        Assert.assertEquals(sortedSetOf("StaticCall"), capabilities.queries["allCustomers"]!!.toSortedSet())
    }

    @Test
    fun userProfileCapabilities(): Unit {
        securityService.selectedUsername = employee1!!.user!!.username

        val userProfileTypeRef = getTypeRef(UserProfile::class.java)
        val entityCapabilities = instanceManagement.getEntityCapabilities(userProfileTypeRef)
        Assert.assertEquals(sortedSetOf("Create", "List"), entityCapabilities.entity.toSortedSet())

        val instanceCapabilities = instanceManagement.getInstanceCapabilities(userProfileTypeRef, user1!!.id.toString())
        Assert.assertEquals(sortedSetOf("Delete", "Read", "Update"), instanceCapabilities.instance.toSortedSet())
    }

    /**
     * Category does not define access constraints, so you get all the default capabilities.
     */
    @Test
    fun category_instanceCapabilities(): Unit {
        securityService.selectedUsername = employee1!!.user!!.username

        val categoryTypeRef = getTypeRef(Category::class.java)
        val entityCapabilities = instanceManagement.getEntityCapabilities(categoryTypeRef)
        Assert.assertEquals(sortedSetOf("Create", "List"), entityCapabilities.entity.toSortedSet())

        val instanceCapabilities = instanceManagement.getInstanceCapabilities(categoryTypeRef, user1!!.id.toString())
        Assert.assertEquals(sortedSetOf("Delete", "Read", "Update"), instanceCapabilities.instance.toSortedSet())
    }

}