package org.ontheground.dmmf.ordertaking.common

import arrow.core.raise.Raise

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
        /// Raise ErrPrimitiveConstraints if input is null, empty, or length > 50
        context(_: Raise<ErrPrimitiveConstraints>)
        operator fun invoke(str: String): String50 = ConstrainedType.ensureStringMaxLen(50, str) { String50(str) }

        /// Create a nullable constrained string using the constructor provided
        /// Return null if input is null, empty.
        /// Raise ErrPrimitiveConstraints if length > maxLen
        context(_: Raise<ErrPrimitiveConstraints>)
        fun createNullable(str: String): String50? =
            if (str.isEmpty()) null
            else ConstrainedType.ensureStringMaxLen(50, str) { String50(str) }
    }
}

/// An email address
@JvmInline
value class EmailAddress private constructor(val value: String) {
    companion object {
        /// Create an EmailAddress from a string
        /// Raise ErrPrimitiveConstraints if input is null, empty, or doesn't have an "@" in it
        context(_: Raise<ErrPrimitiveConstraints>)
        operator fun invoke(str: String): EmailAddress =
            ConstrainedType.ensureStringLike(".+@.+", str) { EmailAddress(str) }
    }
}

/// A zip code
@JvmInline
value class ZipCode private constructor(val value: String) {
    companion object {
        /// Create a ZipCode from a string
        /// Raise ErrPrimitiveConstraints if input is null, empty, or doesn't have 5 digits
        context(_: Raise<ErrPrimitiveConstraints>)
        operator fun invoke(str: String): ZipCode = ConstrainedType.ensureStringLike("""\d{5}""", str) { ZipCode(str) }
    }
}

/// An Id for Orders. Constrained to be a non-empty string <= 50 chars
@JvmInline
value class OrderId private constructor(val value: String) {
    companion object {
        /// Create an OrderId from a string
        /// Raise ErrPrimitiveConstraints if input is null, empty, or length > 50
        context(_: Raise<ErrPrimitiveConstraints>)
        operator fun invoke(str: String): OrderId = ConstrainedType.ensureStringMaxLen(50, str) { OrderId(str) }
    }
}

/// An Id for OrderLines. Constrained to be a non-empty string <= 50 chars
@JvmInline
value class OrderLineId private constructor(val value: String) {
    companion object {
        /// Create an OrderLineId from a string
        /// Raise ErrPrimitiveConstraints if input is null, empty, or length > 50
        context(_: Raise<ErrPrimitiveConstraints>)
        operator fun invoke(str: String): OrderLineId = ConstrainedType.ensureStringMaxLen(50, str) { OrderLineId(str) }
    }
}

/// The codes for Widgets start with a "W" and then four digits
@JvmInline
value class WidgetCode private constructor(val value: String) {
    companion object {
        /// Create an WidgetCode from a string
        /// Raise ErrPrimitiveConstraints if input is null. empty, or not matching pattern
        context(_: Raise<ErrPrimitiveConstraints>)
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
        /// Raise ErrPrimitiveConstraints if input is null, empty, or not matching pattern
        context(_: Raise<ErrPrimitiveConstraints>)
        operator fun invoke(str: String): GizmoCode =
            // The codes for Gizmos start with a "G" and then three digits.
            ConstrainedType.ensureStringLike("""G\d{3}""", str) { GizmoCode(str) }
    }
}

///// A ProductCode is either a Widget or a Gizmo
sealed interface ProductCode {
    /// Return the string value inside a ProductCode
    fun value() = when (this) {
        is Widget -> this.value.value
        is Gizmo -> this.value.value
    }
}

@JvmInline
value class Widget(val value: WidgetCode) : ProductCode

@JvmInline
value class Gizmo(val value: GizmoCode) : ProductCode

/// Create an ProductCode from a string
/// Raise ErrPrimitiveConstraints if input is null, empty, or not matching pattern
context(r: Raise<ErrPrimitiveConstraints>)
fun ProductCode(str: String): ProductCode =
    ConstrainedType.ensureStringLike("""W\d{4}|G\d{3}""", str) {
        when {
            str.startsWith("W") -> Widget(WidgetCode(str))
            str.startsWith("G") -> Gizmo(GizmoCode(str))
            else -> throw (RuntimeException("Invalid widget code: $str"))
        }
    }

/// A Quantity is either a Unit or a Kilogram
sealed interface OrderQuantity {
    /// Constrained to be a decimal between 0.05 and 100.00
    @JvmInline
    value class Kilogram private constructor(val value: Double) : OrderQuantity {
        companion object {
            /// Create a KilogramQuantity from a decimal.
            /// Raise ErrPrimitiveConstraints if input is not a decimal between 0.05 and 100.00
            context(_: Raise<ErrPrimitiveConstraints>)
            operator fun invoke(i: Double) = ConstrainedType.ensureDoubleInBetween(0.05, 100.00, i) { Kilogram(i) }
        }
    }

    /// Constrained to be an integer between 1 and 1000
    @JvmInline
    value class Unit private constructor(val value: Int) : OrderQuantity {
        companion object {
            /// Create a UnitQuantity from an int
            /// Raise ErrPrimitiveConstraints if input is not an integer between 1 and 1000
            context(_: Raise<ErrPrimitiveConstraints>)
            operator fun invoke(i: Int) = ConstrainedType.ensureIntInBetween(1, 1000, i) { Unit(i) }
        }
    }

    /// Return the string value inside a ProductCode
    fun value() = when (this) {
        is Kilogram -> this.value
        is Unit -> this.value.toDouble()
    }
}

/// Create a OrderQuantity from a productCode and quantity
context(_: Raise<ErrPrimitiveConstraints>)
fun OrderQuantity(productCode: ProductCode, quantity: Double): OrderQuantity = when (productCode) {
    is Widget -> OrderQuantity.Unit(quantity.toInt()) // lift to OrderQuantity type
    is Gizmo -> OrderQuantity.Kilogram(quantity) // lift to OrderQuantity type
}

/// Constrained to be a decimal between 0.0 and 1000.00
@JvmInline
value class Price private constructor(val value: Double) {
    /// Multiply a Price by a decimal qty.
    /// Raise ErrPrimitiveConstraints if new price is out of bounds.
    context(_: Raise<ErrPrimitiveConstraints>)
    fun multiply(qty: Double): Price = invoke(qty * this.value)

    companion object {
        /// Create a Price from a decimal.
        /// Raise ErrPrimitiveConstraints if out of bounds. This should only be used if you know the value is valid.
        context(_: Raise<ErrPrimitiveConstraints>)
        operator fun invoke(p: Double): Price =
            ConstrainedType.ensureDoubleInBetween(0.0, 1000.00, p) { Price(p) }

        fun unsafeCreate(p: Double) = Price(p)
    }
}

/// Constrained to be a decimal between 0.0 and 10000.00
@JvmInline
value class BillingAmount private constructor(val value: Double) {
    companion object {
        /// Create a BillingAmount from a decimal.
        /// Raise ErrPrimitiveConstraints if input is not a decimal between 0.0 and 10000.00
        context(_: Raise<ErrPrimitiveConstraints>)
        operator fun invoke(i: Double): BillingAmount =
            ConstrainedType.ensureDoubleInBetween(0.0, 10000.00, i) { BillingAmount(i) }

        /// Sum a list of prices to make a billing amount
        /// Raise ErrPrimitiveConstraints if total is out of bounds
        context(_: Raise<ErrPrimitiveConstraints>)
        fun sumPrices(prices: List<Price>): BillingAmount = invoke(prices.sumOf { it.value })
    }
}