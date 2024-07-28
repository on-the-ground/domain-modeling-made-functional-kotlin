// We are defining types and submodules, so we can use a namespace
// rather than a module at the top level
package org.ontheground.dmmf.ordertaking.common

import arrow.core.*

// ===============================
// Simple types and constrained types related to the OrderTaking domain.
//
// E.g. Single case discriminated unions (aka wrappers), enums, etc
// ===============================

/// Constrained to be 50 chars or less, not null
@JvmInline
value class String50 private constructor(val value: String) {
    init {
        ConstrainedType.requireStringMaxLen(50)(value)
    }

    companion object {
        /// Create an String50 from a string
        /// Return Error if input is null, empty, or length > 50
        operator fun invoke(fieldName: String): (String) -> Either<Throwable, String50> {
            return { str ->
                Either.catch { String50(str) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }

        /// Create an String50 from a string
        /// Return None if input is null, empty.
        /// Return error if length > maxLen
        /// Return Some if the input is valid
        fun createOption(fieldName: String): (String) -> Either<Throwable, Option<String50>> {
            /// Create a optional constrained string using the constructor provided
            /// Return None if input is null, empty.
            /// Return error if length > maxLen
            /// Return Some if the input is valid
            return { str ->
                Either.catch { String50(str).some().right() }
                    .getOrElse { e ->
                        if (ConstrainedType.isEmptyStringError(e)) None.right()
                        else errorWithFieldName(fieldName)(e).left()
                    }
            }
        }
    }
}

///// An email address
@JvmInline
value class EmailAddress private constructor(val value: String) {
    init {
        ConstrainedType.requireStringLike(".+@.+")(value)
    }

    companion object {
        /// Create an EmailAddress from a string
        /// Return Error if input is null, empty, or doesn't have an "@" in it
        operator fun invoke(fieldName: String): (String) -> Either<Throwable, EmailAddress> {
            // anything separated by an "@"
            return { str ->
                Either.catch { EmailAddress(str) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }
    }
}

/// A zip code
@JvmInline
value class ZipCode private constructor(val value: String) {
    init {
        ConstrainedType.requireStringLike("""\d{5}""")(value)
    }

    companion object {
        /// Create a ZipCode from a string
        /// Return Error if input is null, empty, or doesn't have 5 digits
        operator fun invoke(fieldName: String): (String) -> Either<Throwable, ZipCode> {
            return { str ->
                Either.catch { ZipCode(str) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }
    }
}

/// An Id for Orders. Constrained to be a non-empty string <= 50 chars
@JvmInline
value class OrderId private constructor(val value: String) {
    init {
        ConstrainedType.requireStringMaxLen(50)(value)
    }

    companion object {
        /// Create an OrderId from a string
        /// Return Error if input is null, empty, or length > 50
        operator fun invoke(fieldName: String): (String) -> Either<Throwable, OrderId> {
            return { str ->
                Either.catch { OrderId(str) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }
    }
}

/// An Id for OrderLines. Constrained to be a non-empty string <= 50 chars
@JvmInline
value class OrderLineId private constructor(val value: String) {
    init {
        ConstrainedType.requireStringMaxLen(50)(value)
    }

    companion object {
        /// Create an OrderLineId from a string
        /// Return Error if input is null, empty, or length > 50
        operator fun invoke(fieldName: String): (String) -> Either<Throwable, OrderLineId> {
            return { str ->
                Either.catch { OrderLineId(str) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }
    }
}

/// The codes for Widgets start with a "W" and then four digits
@JvmInline
value class WidgetCode private constructor(val value: String) {
    init {
        ConstrainedType.requireStringLike("""W\d{4}""")(value)
    }

    companion object {
        /// Create an WidgetCode from a string
        /// Return Error if input is null. empty, or not matching pattern
        operator fun invoke(fieldName: String): (String) -> Either<Throwable, WidgetCode> {
            // The codes for Widgets start with a "W" and then four digits
            return { str ->
                Either.catch { WidgetCode(str) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }
    }

}

/// The codes for Gizmos start with a "G" and then three digits.
@JvmInline
value class GizmoCode private constructor(val value: String) {
    init {
        ConstrainedType.requireStringLike("""G\d{3}""")(value)
    }

    companion object {
        /// Create an GizmoCode from a string
        /// Return Error if input is null, empty, or not matching pattern
        operator fun invoke(fieldName: String): (String) -> Either<Throwable, GizmoCode> {
            // The codes for Gizmos start with a "G" and then three digits.
            return { str ->
                Either.catch { GizmoCode(str) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }
    }
}

///// A ProductCode is either a Widget or a Gizmo
sealed interface ProductCode {
    /// Return the string value inside a ProductCode
    fun value() {
        when (this) {
            is Widget -> this.value.value
            is Gizmo -> this.value.value
        }
    }
}

@JvmInline
value class Widget(val value: WidgetCode) : ProductCode

@JvmInline
value class Gizmo(val value: GizmoCode) : ProductCode

/// Create an ProductCode from a string
/// Return Error if input is null, empty, or not matching pattern
fun createProductCode(
    fieldName: String,
): (String) -> Either<Throwable, ProductCode> {
    return { code ->
        if (code.isNullOrEmpty()) Throwable("${fieldName}: Must not be null or empty").left()
        else if (code.startsWith("W")) WidgetCode(fieldName)(code).map { w -> Widget(w) }
        else if (code.startsWith("G")) GizmoCode(fieldName)(code).map { g -> Gizmo(g) }
        else Throwable("${fieldName}: Format not recognized '${code}'").left()
    }
}


/// Constrained to be a integer between 1 and 1000
@JvmInline
value class UnitQuantity private constructor(val value: Int) {
    init {
        ConstrainedType.requireIntInBetween(1, 1000)(value)
    }

    companion object {
        /// Create a UnitQuantity from a int
        /// Return Error if input is not an integer between 1 and 1000
        operator fun invoke(fieldName: String): (Int) -> Either<Throwable, UnitQuantity> {
            return { i ->
                Either.catch { UnitQuantity(i) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }
    }
}

/// Constrained to be a decimal between 0.05 and 100.00
@JvmInline
value class KilogramQuantity private constructor(val value: Double) {
    init {
        ConstrainedType.requireDoubleInBetween(0.05, 100.00)(value)
    }

    companion object {
        /// Create a KilogramQuantity from a decimal.
        /// Return Error if input is not a decimal between 0.05 and 100.00
        operator fun invoke(fieldName: String): (Double) -> Either<Throwable, KilogramQuantity> {
            return { i ->
                Either.catch { KilogramQuantity(i) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }
    }
}

/// A Quantity is either a Unit or a Kilogram
sealed interface OrderQuantity {
    /// Return the value inside a OrderQuantity
    fun value(qty: OrderQuantity): Double {
        return when (qty) {
            is Unit -> qty.value.value.toDouble()
            is Kilogram -> qty.value.value
        }
    }
}

@JvmInline
value class Unit(val value: UnitQuantity) : OrderQuantity

@JvmInline
value class Kilogram(val value: KilogramQuantity) : OrderQuantity

/// Create a OrderQuantity from a productCode and quantity
fun createOrderQuantity(
    fieldName: String,
    productCode: ProductCode,
): (Double) -> Either<Throwable, OrderQuantity> {
    return { quantity ->
        when (productCode) {
            is Widget -> UnitQuantity(fieldName)(quantity.toInt()).map { u -> Unit(u) } // lift to OrderQuantity type
            is Gizmo -> KilogramQuantity(fieldName)(quantity).map { u -> Kilogram(u) } // lift to OrderQuantity type
        }
    }
}


/// Constrained to be a decimal between 0.0 and 1000.00
@JvmInline
value class Price private constructor(val value: Double) {
    init {
        /// Create a Price from a decimal.
        /// Return Error if input is not a decimal between 0.0 and 1000.00
        ConstrainedType.requireDoubleInBetween(0.0, 1000.00)(value)
    }

    /// Multiply a Price by a decimal qty.
    /// Return Error if new price is out of bounds.
    fun multiply(qty: Double): Price {
        return Price(qty * this.value)
    }


    companion object {
        operator fun invoke(fieldName: String): (Double) -> Either<Throwable, Price> {
            /// Create a Price from a decimal.
            /// Throw an exception if out of bounds. This should only be used if you know the value is valid.
            return { p ->
                Either.catch { Price(p) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }

        fun unsafeCreate(fieldName: String): (Double) -> Price {
            /// Create a Price from a decimal.
            /// Throw an exception if out of bounds. This should only be used if you know the value is valid.
            return { p ->
                try {
                    Price(p)
                } catch (e: Throwable) {
                    throw Throwable(message = "Not expecting Price to be out of bounds", cause = e)
                }
            }
        }
    }
}

/// Constrained to be a decimal between 0.0 and 10000.00
@JvmInline
value class BillingAmount private constructor(val value: Double) {
    init {
        ConstrainedType.requireDoubleInBetween(0.0, 10000.00)(value)
    }

    companion object {
        /// Create a BillingAmount from a decimal.
        /// Return Error if input is not a decimal between 0.0 and 10000.00
        operator fun invoke(fieldName: String): (Double) -> Either<Throwable, BillingAmount> {
            return { i ->
                Either.catch { BillingAmount(i) }
                    .mapLeft { e -> errorWithFieldName(fieldName)(e) }
            }
        }

        /// Sum a list of prices to make a billing amount
        /// Return Error if total is out of bounds
        fun sumPrices(prices: Array<Price>): BillingAmount {
            return BillingAmount(prices.sumOf { p -> p.value })
        }
    }
}

/// Represents a PDF attachment
class PdfAttachment(
    val Name: String,
    val Bytes: ByteArray,
)


// ===============================
// Reusable constructors and getters for constrained types
// ===============================

/// Useful functions for constrained types
object ConstrainedType {
    private val isEmptyStringErrorMessage = "must not be null or empty"

    fun isEmptyStringError(e: Throwable): Boolean {
        return e.message == isEmptyStringErrorMessage
    }

    /// Create a constrained string using the constructor provided
    /// Return Error if input is null, empty, or length > maxLen
    fun requireStringMaxLen(
        maxLen: Int
    ): (String) -> kotlin.Unit {
        return { str ->
            require(!str.isNullOrEmpty(), { isEmptyStringErrorMessage })
            require(str.length <= maxLen, { "Must not be more than ${maxLen} chars" })
        }
    }

    /// Create a constrained integer using the constructor provided
    /// Return Error if input is less than minVal or more than maxVal
    fun requireIntInBetween(
        minVal: Int,
        maxVal: Int,
    ): (Int) -> kotlin.Unit {
        return { i ->
            require(minVal <= i, { "Must not be less than ${minVal}" })
            require(i <= maxVal, { "Must not be greater than ${maxVal}" })
        }
    }

    /// Create a constrained decimal using the constructor provided
    /// Return Error if input is less than minVal or more than maxVal
    fun requireDoubleInBetween(
        minVal: Double,
        maxVal: Double,
    ): (Double) -> kotlin.Unit {
        return { i: Double ->
            require(minVal <= i, { "Must not be less than ${minVal}" })
            require(i <= maxVal, { "Must not be greater than ${maxVal}" })
        }
    }

    /// Create a constrained string using the constructor provided
    /// Return Error if input is null. empty, or does not match the regex pattern
    fun requireStringLike(
        pattern: String,
    ): (String) -> kotlin.Unit {
        return { str ->
            require(!str.isNullOrEmpty(), { "Must not be null or empty" })
            require(pattern.toRegex().matches(str), { "'${str}' must match the pattern '${pattern}'" })
        }
    }
}


fun errorWithFieldName(fieldName: String): (e: Throwable) -> Throwable {
    return { e ->
        Throwable(
            message = "${fieldName}: ${e.message ?: ""}",
            cause = e,
        )
    }
}

