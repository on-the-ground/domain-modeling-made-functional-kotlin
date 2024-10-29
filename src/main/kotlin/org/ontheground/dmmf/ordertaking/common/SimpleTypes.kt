package org.ontheground.dmmf.ordertaking.common

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.ontheground.dmmf.ordertaking.common.ConstrainedType.isEmptyStringError

// ===============================
// Simple types and constrained types related to the OrderTaking domain.
//
// E.g. Single case discriminated unions (aka wrappers), enums, etc
// ===============================

/// Constrained to be 50 chars or less, not null
@JvmInline
value class String50 private constructor(val value: String) {
    companion object {
        /// Create a String50 from a string
        /// Return Error if input is null, empty, or length > 50
        operator fun invoke(str: String): Either<Throwable, String50> =
            ConstrainedType.ensureStringMaxLen(50, str) { String50(str) }

        /// Create a nullable constrained string using the constructor provided
        /// Return null if input is null, empty.
        /// Return error if length > maxLen
        fun createNullable(str: String): Either<Throwable, String50?> =
            ConstrainedType.ensureStringMaxLen(50, str) { String50(str) }
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
    companion object {
        /// Create an EmailAddress from a string
        /// Return Error if input is null, empty, or doesn't have an "@" in it
        operator fun invoke(str: String): Either<Throwable, EmailAddress> =
            ConstrainedType.ensureStringLike(".+@.+", str) { EmailAddress(str) }
    }
}

/// A zip code
@JvmInline
value class ZipCode private constructor(val value: String) {
    companion object {
        /// Create a ZipCode from a string
        /// Return Error if input is null, empty, or doesn't have 5 digits
        operator fun invoke(str: String): Either<Throwable, ZipCode> =
            ConstrainedType.ensureStringLike("""\d{5}""", str) { ZipCode(str) }
    }
}

/// An Id for Orders. Constrained to be a non-empty string <= 50 chars
@JvmInline
value class OrderId private constructor(val value: String) {
    companion object {
        /// Create an OrderId from a string
        /// Return Error if input is null, empty, or length > 50
        operator fun invoke(str: String): Either<Throwable, OrderId> =
            ConstrainedType.ensureStringMaxLen(50, str) { OrderId(str) }
    }
}

/// An Id for OrderLines. Constrained to be a non-empty string <= 50 chars
@JvmInline
value class OrderLineId private constructor(val value: String) {
    companion object {
        /// Create an OrderLineId from a string
        /// Return Error if input is null, empty, or length > 50
        operator fun invoke(str: String): Either<Throwable, OrderLineId> =
            ConstrainedType.ensureStringMaxLen(50, str) { OrderLineId(str) }
    }
}

/// The codes for Widgets start with a "W" and then four digits
@JvmInline
value class WidgetCode private constructor(val value: String) {
    companion object {
        /// Create an WidgetCode from a string
        /// Return Error if input is null. empty, or not matching pattern
        operator fun invoke(str: String): Either<Throwable, WidgetCode> =
            // The codes for Widgets start with a "W" and then four digits
            ConstrainedType.ensureStringLike("""W\d{4}""", str) { WidgetCode(str) }
    }

}

/// The codes for Gizmos start with a "G" and then three digits.
@JvmInline
value class GizmoCode private constructor(val value: String) {
    companion object {
        /// Create an GizmoCode from a string
        /// Return Error if input is null, empty, or not matching pattern
        operator fun invoke(str: String): Either<Throwable, GizmoCode> =
            // The codes for Gizmos start with a "G" and then three digits.
            ConstrainedType.ensureStringLike("""G\d{3}""", str) { GizmoCode(str) }
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
fun String.toProductCode(): Either<Throwable, ProductCode> =
    if (this.isEmpty()) Throwable("Must not be null or empty").left()
    else if (this.startsWith("W")) WidgetCode(this).map { Widget(it) }
    else if (this.startsWith("G")) GizmoCode(this).map { Gizmo(it) }
    else Throwable("Format not recognized '${this}'").left()

/// Constrained to be an integer between 1 and 1000
@JvmInline
value class UnitQuantity private constructor(val value: Int) {
    companion object {
        /// Create a UnitQuantity from an int
        /// Return Error if input is not an integer between 1 and 1000
        operator fun invoke(i: Int): Either<Throwable, UnitQuantity> =
            ConstrainedType.ensureIntInBetween(1, 1000, i) { UnitQuantity(i) }
    }
}

/// Constrained to be a decimal between 0.05 and 100.00
@JvmInline
value class KilogramQuantity private constructor(val value: Double) {
    companion object {
        /// Create a KilogramQuantity from a decimal.
        /// Return Error if input is not a decimal between 0.05 and 100.00
        operator fun invoke(i: Double): Either<Throwable, KilogramQuantity> =
            ConstrainedType.ensureDoubleInBetween(0.05, 100.00, i) { KilogramQuantity(i) }
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
fun Double.toOrderQuantity(productCode: ProductCode): Either<Throwable, OrderQuantity> =
    when (productCode) {
        is Widget -> UnitQuantity(this.toInt()).map { i -> Unit(i) } // lift to OrderQuantity type
        is Gizmo -> KilogramQuantity(this).map { i -> Kilogram(i) } // lift to OrderQuantity type
    }


/// Constrained to be a decimal between 0.0 and 1000.00
@JvmInline
value class Price private constructor(val value: Double) {
    init {
        /// Create a Price from a decimal.
        /// Return Error if input is not a decimal between 0.0 and 1000.00
        ConstrainedType.requireDoubleInBetween(0.0, 1000.00, value)
    }

    /// Multiply a Price by a decimal qty.
    /// Return Error if new price is out of bounds.
    fun multiply(qty: Double): Either<Throwable, Price> =
        Price.invoke(qty * this.value)


    companion object {
        operator fun invoke(p: Double): Either<Throwable, Price> =
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
    companion object {
        /// Create a BillingAmount from a decimal.
        /// Return Error if input is not a decimal between 0.0 and 10000.00
        operator fun invoke(i: Double): Either<Throwable, BillingAmount> =
            ConstrainedType.ensureDoubleInBetween(0.0, 10000.00, i) { BillingAmount(i) }


        /// Sum a list of prices to make a billing amount
        /// Return Error if total is out of bounds
        fun sumPrices(prices: Array<Price>): Either<Throwable, BillingAmount> =
            invoke(prices.sumOf { it.value })
    }
}