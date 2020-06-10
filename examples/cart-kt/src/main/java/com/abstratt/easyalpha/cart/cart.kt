
package com.abstratt.easyalpha.cart

import com.abstratt.kirra.pojo.*
import com.abstratt.kirra.spring.*
import com.abstratt.kirra.spring.user.RoleEntity
import com.abstratt.kirra.spring.user.RoleEntityService
import com.abstratt.kirra.spring.user.RoleRepository
import com.abstratt.kirra.spring.user.UserRole
import com.abstratt.kirra.spring.userprofile.UserProfile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import javax.persistence.*

interface ShoppingCartMarker

enum class ShoppingCartRole : UserRole, ShoppingCartRoleConstants {
    Employee, Customer, Administrator
}

interface ShoppingCartRoleConstants {
    companion object {
        const val Employee = "Employee"
        const val Customer = "Customer"
        const val Administrator = "Administrator"
    }
}

@Entity
class Category : BaseEntity() {
    @Column(nullable = false)
    var name: String? = null
}

@Repository
interface CategoryRepository : BaseRepository<Category> {
}

@Service
open class CategoryService : BaseService<Category, CategoryRepository>(Category::class) {
    @Autowired
    lateinit open var productRepository : ProductRepository

    @RelationshipAccessor
    fun products(category : Category) : Collection<Product> =
            productRepository.findAllByCategory(category)
}

@Entity
@Named(description = "Products that can be added to a car")
class Product : BaseEntity() {
    val moniker : String get() = this.name ?: "N/A"
    @Column(nullable = false)
    var name: String? = null
    @Column(nullable = false)
    var price: Double? = null
    @ManyToOne(optional = false)
    var category: Category? = null
    @Column(nullable = true)
    var available : Boolean? = true
}

@Entity
class OrderItem(
        @ManyToOne(optional = false, cascade = arrayOf(CascadeType.ALL))
        var order: Order? = null,
        @ManyToOne(optional = false)
        var product: Product? = null,
        @Column(nullable = false)
        var quantity: Int? = 1
) : BaseEntity() {
    val price get() = (this.product?.price ?: 0.0) * (this.quantity ?: 0)
}

enum class OrderStatus {
    @Column
    Open, Closed, Canceled
}

@Entity
@Table(name = "customer_order")
class Order(id: Long? = null) : BaseEntity(id) {
    @Enumerated(EnumType.STRING)
    var status : OrderStatus? = OrderStatus.Open

    @ManyToOne(cascade = arrayOf(CascadeType.ALL), optional = false)
    var customer: Customer? = null

    @OneToMany(orphanRemoval = true, mappedBy = "order")
    var items: MutableCollection<OrderItem> = ArrayList()

    @ActionOp
    fun addItem(product: Product, quantity: Int = 1): OrderItem {
        val newItem = OrderItem(order = this, product = product, quantity = quantity)
        items.add(newItem)
        return newItem
    }
}

@Repository
interface OrderRepository : BaseRepository<Order> {
    fun findAllByStatus(toMatch : OrderStatus) : Iterable<Order>
}

@Service
open class OrderService : BaseService<Order, OrderRepository>(Order::class) {
    @Autowired
    lateinit var productRepository : ProductRepository

    @QueryOp
    open fun byStatus(toMatch : OrderStatus) = repository.findAllByStatus(toMatch)

    @DomainAccessor
    fun addItem_product(order : Order, pageRequest: PageRequest?) :Page<Product> =
            productRepository.findAllByAvailableIsTrue(pageRequest)
}

@Entity
abstract class Person(
        id: Long? = null,
        @Column(unique = true)
        var name: String? = null,
        user : UserProfile? = null
) : RoleEntity(id, user)


@Entity
class Employee(id : Long? = null, name : String? = null, user : UserProfile? = null) : Person(id, name, user) {
    override fun getRole(): UserRole = ShoppingCartRole.Employee
}

@Entity
class Administrator(id : Long? = null, name : String? = null, user : UserProfile? = null) : Person(id, name, user) {
    override fun getRole(): UserRole = ShoppingCartRole.Administrator
}

@Entity
class Customer(id : Long? = null, name : String? = null, user : UserProfile? = null) : Person(id, name, user) {
    @OneToMany(orphanRemoval = false, mappedBy = "customer")
    var orders: MutableCollection<Order> = ArrayList()

    override fun getRole(): UserRole = ShoppingCartRole.Customer

    object accessControl : AccessControl<Customer, Person>(
            constraint(
                    roles(Customer::class),
                    can(Capability.Update),
                    provided { e, re -> e == re }
            ),
            constraint(
                    roles(Customer::class),
                    can(Capability.List, Capability.Read)
            ),
            constraint(
                    roles(Employee::class),
                    can(Capability.List, Capability.Read, Capability.Create)
            ),
            constraint(
                    CustomerService::allCustomers,
                    roles(Employee::class),
                    can(Capability.Call)
            )
    )
}

@Service
open class CustomerService : BaseService<Customer, CustomerRepository>(Customer::class) {
    @QueryOp
    fun allCustomers(pageable : Pageable? = null) : Page<Customer> = repository.findAll(pageable)

    @RelationshipAccessor
    open fun roleAsCustomer(profile : UserProfile) = repository.findByUser(profile)
}

@Service
open class EmployeeService : RoleEntityService<Employee, EmployeeRepository>(Employee::class) {
    @RelationshipAccessor
    open fun roleAsEmployee(profile : UserProfile) = findByUser(profile)
}

@Service
open class AdministratorService : RoleEntityService<Administrator, AdministratorRepository>(Administrator::class) {
    @RelationshipAccessor
    open fun roleAsAdministrator(profile : UserProfile) = findByUser(profile)
}



@Repository
interface CustomerRepository : RoleRepository<Customer>

@Repository
interface EmployeeRepository : RoleRepository<Employee>

@Repository
interface AdministratorRepository : RoleRepository<Administrator>

@Repository
interface ProductRepository : BaseRepository<Product> {
    @RelationshipAccessor
    fun findAllByCategory(category : Category) : Collection<Product>

    fun findAllByAvailableIsTrue(pageRequest : Pageable?) : Page<Product>
}

@Repository
interface OrderItemRepository : BaseRepository<OrderItem>

