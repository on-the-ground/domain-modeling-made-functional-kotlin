package org.ontheground.dmmf.ordertaking.common

import arrow.core.raise.Raise
import arrow.core.raise.withError

// ===============================
// Simple types and constrained types related to the OrderTaking domain.
//
// E.g. Single case discriminated unions (aka wrappers), enums, etc
// ===============================

/// Constrained to be 50 chars or less, not null
class StringTooLong : Error()

@JvmInline
value class String50 private constructor(val value: String) {
    companion object {
        /// Create a String50 from a string
        /// Return Error if input is null, empty, or length > 50
        context(Raise<StringTooLong>)
        operator fun invoke(str: String): String50 =
            withError({ _ -> StringTooLong() }) {
                ConstrainedType.ensureStringMaxLen(50, str) {
                    String50(str)
                }
            }


        /// Create a nullable constrained string using the constructor provided
        /// Return null if input is null, empty.
        /// Return error if length > maxLen
        context(Raise<StringTooLong>)
        fun createNullable(str: String): String50? {
            if (str.length == 0) return null
            return withError<StringTooLong, Error, String50>({ _ -> StringTooLong() }) {
                ConstrainedType.ensureStringMaxLen(50, str) { String50(str) }
            }
        }

    }
}

class InvalidEmailAddress : Error()
/// An email address
@JvmInline
value class EmailAddress private constructor(val value: String) {
    companion object {
        /// Create an EmailAddress from a string
        /// Return Error if input is null, empty, or doesn't have an "@" in it
        context(Raise<InvalidEmailAddress>)
        operator fun invoke(str: String): EmailAddress =
            withError({_ -> InvalidEmailAddress() }) {
                ConstrainedType.ensureStringLike(".+@.+", str) { EmailAddress(str) }
            }

    }
}

class InvalidZipCode : Error()
/// A zip code
@JvmInline
value class ZipCode private constructor(val value: String) {
    companion object {
        /// Create a ZipCode from a string
        /// Return Error if input is null, empty, or doesn't have 5 digits
        context(Raise<InvalidZipCode>)
        operator fun invoke(str: String): ZipCode =
            withError({_ -> InvalidZipCode() }){
                ConstrainedType.ensureStringLike("""\d{5}""", str) { ZipCode(str) }
            }

    }
}

/// An Id for Orders. Constrained to be a non-empty string <= 50 chars
@JvmInline
value class OrderId private constructor(val value: String) {
    companion object {
        /// Create an OrderId from a string
        /// Return Error if input is null, empty, or length > 50
        context(Raise<StringTooLong>)
        operator fun invoke(str: String): OrderId =
            withError({ _ -> StringTooLong() }) {
                ConstrainedType.ensureStringMaxLen(50, str) { OrderId(str) }
            }
    }
}

/// An Id for OrderLines. Constrained to be a non-empty string <= 50 chars
@JvmInline
value class OrderLineId private constructor(val value: String) {
    companion object {
        /// Create an OrderLineId from a string
        /// Return Error if input is null, empty, or length > 50
        context(Raise<Error>)
        operator fun invoke(str: String): OrderLineId =
            withError({ _ -> StringTooLong() }) {
                ConstrainedType.ensureStringMaxLen(50, str) { OrderLineId(str) }
            }
    }
}

/// The codes for Widgets start with a "W" and then four digits
@JvmInline
value class WidgetCode private constructor(val value: String) {
    companion object {
        /// Create an WidgetCode from a string
        /// Return Error if input is null. empty, or not matching pattern
        context(Raise<Error>)
        operator fun invoke(str: String): WidgetCode =
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
        context(Raise<Error>)
        operator fun invoke(str: String): GizmoCode =
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
context(Raise<Throwable>)
fun String.toProductCode(): ProductCode =
    if (this.isEmpty()) raise(Throwable("Must not be null or empty"))
    else if (this.startsWith("W")) Widget(WidgetCode(this))
    else if (this.startsWith("G")) Gizmo(GizmoCode(this))
    else raise(Throwable("Format not recognized '${this}'"))

/// Constrained to be an integer between 1 and 1000
@JvmInline
value class UnitQuantity private constructor(val value: Int) {
    companion object {
        /// Create a UnitQuantity from an int
        /// Return Error if input is not an integer between 1 and 1000
        context(Raise<Error>)
        operator fun invoke(i: Int): UnitQuantity =
            ConstrainedType.ensureIntInBetween(1, 1000, i) { UnitQuantity(i) }
    }
}

/// Constrained to be a decimal between 0.05 and 100.00
@JvmInline
value class KilogramQuantity private constructor(val value: Double) {
    companion object {
        /// Create a KilogramQuantity from a decimal.
        /// Return Error if input is not a decimal between 0.05 and 100.00
        context(Raise<Error>)
        operator fun invoke(i: Double): KilogramQuantity =
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
context(Raise<Error>)
fun Double.toOrderQuantity(productCode: ProductCode): OrderQuantity =
    when (productCode) {
        is Widget -> Unit(UnitQuantity(this.toInt())) // lift to OrderQuantity type
        is Gizmo -> Kilogram(KilogramQuantity(this)) // lift to OrderQuantity type
    }


/// Constrained to be a decimal between 0.0 and 1000.00
@JvmInline
value class Price private constructor(val value: Double) {
    init {
        /// Create a Price from a decimal.
        /// Return Error if input is not a decimal between 0.0 and 1000.00

    }

    /// Multiply a Price by a decimal qty.
    /// Return Error if new price is out of bounds.
    context(Raise<Error>)
    fun multiply(qty: Double): Price =
        invoke(qty * this.value)


    companion object {
        context(Raise<Error>)
        operator fun invoke(p: Double): Price =
        /// Create a Price from a decimal.
            /// Throw an exception if out of bounds. This should only be used if you know the value is valid.
            ConstrainedType.ensureDoubleInBetween(0.0, 1000.00, p) { Price(p) }
    }
}

/// Constrained to be a decimal between 0.0 and 10000.00
@JvmInline
value class BillingAmount private constructor(val value: Double) {
    companion object {
        /// Create a BillingAmount from a decimal.
        /// Return Error if input is not a decimal between 0.0 and 10000.00
        context(Raise<Error>)
        operator fun invoke(i: Double): BillingAmount =
            ConstrainedType.ensureDoubleInBetween(0.0, 10000.00, i) { BillingAmount(i) }


        /// Sum a list of prices to make a billing amount
        /// Return Error if total is out of bounds
        context(Raise<Error>)
        fun sumPrices(prices: Array<Price>): BillingAmount =
            invoke(prices.sumOf { it.value })
    }
}