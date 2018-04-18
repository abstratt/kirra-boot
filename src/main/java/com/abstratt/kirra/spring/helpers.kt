package com.abstratt.kirra.spring

import com.abstratt.kirra.TypeRef
import org.apache.commons.lang3.StringUtils
import kotlin.reflect.KClass

fun <E> getTypeRef(javaClass: Class<E>, kind : TypeRef.TypeKind = TypeRef.TypeKind.Entity): TypeRef =
        TypeRef(packageNameToNamespace(javaClass.`package`.name), javaClass.simpleName, kind)

fun <E : BaseEntity> KClass<E>.getTypeRef(kind : TypeRef.TypeKind = TypeRef.TypeKind.Entity): TypeRef =
    com.abstratt.kirra.spring.getTypeRef(this.java, kind)

fun packageNameToNamespace(packageName : String) : String = packageName.split('.').last()

fun getLabel(name: String): String =
        StringUtils.splitByCharacterTypeCamelCase(name).map { it.capitalize() }.joinToString(" ", "", "")

