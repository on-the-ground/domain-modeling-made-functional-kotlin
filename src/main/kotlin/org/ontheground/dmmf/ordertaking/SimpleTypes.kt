package org.ontheground.dmmf.ordertaking

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.ontheground.dmmf.ordertaking.ConstrainedType.isEmptyStringError

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
        /// Create a String50 from a string
        /// Return Error if input is null, empty, or length > 50
        fun create(str: String): Either<Throwable, String50> =
            Either.catch { String50(str) }

        /// Create a nullable constrained string using the constructor provided
        /// Return null if input is null, empty.
        /// Return error if length > maxLen
        fun createNullable(str: String): Either<Throwable, String50?> =
            Either.catch { String50(str) }
                .fold(
                    ifLeft = {
                        if (it.isEmptyStringError()) null.right()
                        else it.left()
                    },
                    ifRight = { it.right() }
                )
    }
}

/// An email address
@JvmInline
value class EmailAddress private constructor(val value: String) {
    init {
        ConstrainedType.requireStringLike(".+@.+")(value)
    }

    companion object {
        /// Create an EmailAddress from a string
        /// Return Error if input is null, empty, or doesn't have an "@" in it
        fun create(str: String): Either<Throwable, EmailAddress> =
            Either.catch { EmailAddress(str) }
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
        fun create(str: String): Either<Throwable, ZipCode> =
            Either.catch { ZipCode(str) }
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
        fun create(str: String): Either<Throwable, OrderId> =
            Either.catch { OrderId(str) }
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
        fun create(str: String): Either<Throwable, OrderLineId> =
            Either.catch { OrderLineId(str) }
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
        fun create(str: String): Either<Throwable, WidgetCode> =
            // The codes for Widgets start with a "W" and then four digits
            Either.catch { WidgetCode(str) }
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
        fun create(str: String): Either<Throwable, GizmoCode> =
            // The codes for Gizmos start with a "G" and then three digits.
            Either.catch { GizmoCode(str) }
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
fun createProductCode(code: String): Either<Throwable, ProductCode> =
    if (code.isEmpty()) Throwable("Must not be null or empty").left()
    else if (code.startsWith("W")) WidgetCode.create(code).map { Widget(it) }
    else if (code.startsWith("G")) GizmoCode.create(code).map { Gizmo(it) }
    else Throwable("Format not recognized '${code}'").left()

/// Constrained to be an integer between 1 and 1000
@JvmInline
value class UnitQuantity private constructor(val value: Int) {
    init {
        ConstrainedType.requireIntInBetween(1, 1000)(value)
    }

    companion object {
        /// Create a UnitQuantity from an int
        /// Return Error if input is not an integer between 1 and 1000
        fun create(i: Int): Either<Throwable, UnitQuantity> =
            Either.catch { UnitQuantity(i) }
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
        fun create(i: Double): Either<Throwable, KilogramQuantity> =
            Either.catch { KilogramQuantity(i) }
    }
}

/// A Quantity is either a Unit or a Kilogram
sealed interface OrderQuantity {
    /// Return the value inside a OrderQuantity
    fun value(qty: OrderQuantity): Double =
        when (qty) {
            is Unit -> qty.value.value.toDouble()
            is Kilogram -> qty.value.value
        }
}

@JvmInline
value class Unit(val value: UnitQuantity) : OrderQuantity

@JvmInline
value class Kilogram(val value: KilogramQuantity) : OrderQuantity

/// Create a OrderQuantity from a productCode and quantity
fun createOrderQuantity(
    productCode: ProductCode,
): (Double) -> Either<Throwable, OrderQuantity> = {
    when (productCode) {
        is Widget -> UnitQuantity.create(it.toInt()).map { i -> Unit(i) } // lift to OrderQuantity type
        is Gizmo -> KilogramQuantity.create(it).map { i -> Kilogram(i) } // lift to OrderQuantity type
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
    fun multiply(qty: Double): Either<Throwable, Price> =
        create(qty * this.value)


    companion object {
        fun create(p: Double): Either<Throwable, Price> =
        /// Create a Price from a decimal.
            /// Throw an exception if out of bounds. This should only be used if you know the value is valid.
            Either.catch { Price(p) }


        fun unsafeCreate(p: Double): Price {
            /// Create a Price from a decimal.
            /// Throw an exception if out of bounds. This should only be used if you know the value is valid.
            try {
                return Price(p)
            } catch (e: Throwable) {
                throw Throwable(message = "Not expecting Price to be out of bounds", cause = e)
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
        fun create(i: Double): Either<Throwable, BillingAmount> =
            Either.catch { BillingAmount(i) }


        /// Sum a list of prices to make a billing amount
        /// Return Error if total is out of bounds
        fun sumPrices(prices: Array<Price>): Either<Throwable, BillingAmount> =
            create(prices.sumOf { it.value })
    }
}

// ===============================
// Reusable validation logic for constrained types
// ===============================

/// Useful functions for constrained types
object ConstrainedType {
    private const val EMPTY_STRING_PRETERMS = "Must not be null or empty"
    private const val TOO_LONG_STRING_PRETERMS = "Must not be more than "
    private const val PATTERN_UNMATCHED_STRING_PRETERMS = "must match the pattern"

    fun Throwable.isEmptyStringError(): Boolean =
        this.message == EMPTY_STRING_PRETERMS

    fun Throwable.isStringOverMaxLenError(): Boolean =
        (this.message ?: "").startsWith(TOO_LONG_STRING_PRETERMS)

    fun Throwable.isStringPatternUnmatchedError(): Boolean =
        (this.message ?: "").contains(PATTERN_UNMATCHED_STRING_PRETERMS)

    /// Create a constrained string using the constructor provided
    /// Return Error if input is null, empty, or length > maxLen
    fun requireStringMaxLen(
        maxLen: Int
    ): (String) -> kotlin.Unit = {
        require(it.isNotEmpty()) { EMPTY_STRING_PRETERMS }
        require(it.length <= maxLen) { "$TOO_LONG_STRING_PRETERMS $maxLen chars" }
    }

    /// Create a constrained integer using the constructor provided
    /// Return Error if input is less than minVal or more than maxVal
    fun requireIntInBetween(
        minVal: Int,
        maxVal: Int,
    ): (Int) -> kotlin.Unit = {
        require(minVal <= it) { "Must not be less than $minVal" }
        require(it <= maxVal) { "Must not be greater than $maxVal" }
    }

    /// Create a constrained decimal using the constructor provided
    /// Return Error if input is less than minVal or more than maxVal
    fun requireDoubleInBetween(
        minVal: Double,
        maxVal: Double,
    ): (Double) -> kotlin.Unit = {
        require(minVal <= it) { "Must not be less than $minVal" }
        require(it <= maxVal) { "Must not be greater than $maxVal" }
    }


    /// Create a constrained string using the constructor provided
    /// Return Error if input is null. empty, or does not match the regex pattern
    fun requireStringLike(
        pattern: String,
    ): (String) -> kotlin.Unit = {
        require(it.isNotEmpty()) { EMPTY_STRING_PRETERMS }
        require(pattern.toRegex().matches(it)) { "'${it}' $PATTERN_UNMATCHED_STRING_PRETERMS '${pattern}'" }
    }

}
