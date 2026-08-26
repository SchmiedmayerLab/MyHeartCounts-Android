//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.foundation

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * A class that represents a type reference capturing generic types at runtime.
 *
 * Instances of this type can be build via [typeReference] function.
 */
sealed interface TypeReference<out T : Any> {
    val type: Type
}

/**
 * Returns the simple name of the type represented by this [TypeReference].
 */
val <T : Any> TypeReference<T>.simpleTypeName: String
    get() = type.typeName.substringAfterLast('.')

/**
 * Base class for [TypeReference]s. Instances of this class are created and returned
 * via the [typeReference] function.
 *
 * The generic argument is read back from the anonymous subclass' generic signature, which only
 * survives when the `Signature` attribute is kept and the subclasses are not merged into one
 * another. [erasure] is the same type as seen by the compiler at the call site and is used both as
 * a fallback when no signature is present and as a cross-check that the signature that *is* present
 * actually belongs to this type reference.
 *
 * @param erasure The erased type of [T] as captured at the [typeReference] call site.
 */
@PublishedApi
internal abstract class TypeReferenceImpl<T : Any>(private val erasure: Class<*>) : TypeReference<T> {
    override val type: Type by lazy {
        val captured = (javaClass.genericSuperclass as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
        if (captured != null && captured.rawClass() == erasure) captured else erasure
    }

    override fun equals(other: Any?) = other is TypeReference<*> && type == other.type
    override fun hashCode() = type.hashCode()
    override fun toString(): String = type.typeName

    private fun Type.rawClass(): Class<*>? = when (this) {
        is Class<*> -> this
        is ParameterizedType -> rawType as? Class<*>
        else -> null
    }
}

/**
 * Creates a [TypeReference] for the specified type.
 *
 * Example usage:
 * ```
 * val typeRef = typeReference<List<String>>()
 * ```
 *
 * @param T The type to capture.
 */
inline fun <reified T : Any> typeReference(): TypeReference<T> =
    object : TypeReferenceImpl<T>(T::class.javaObjectType) {}
