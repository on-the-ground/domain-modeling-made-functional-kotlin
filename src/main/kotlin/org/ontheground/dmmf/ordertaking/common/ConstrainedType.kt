package org.ontheground.dmmf.ordertaking.common

import arrow.core.raise.Raise
import arrow.core.raise.ensure

sealed interface ErrPrimitiveConstraints

sealed interface ErrStringMaxLen : ErrPrimitiveConstraints
sealed interface ErrNumberInBetween: ErrPrimitiveConstraints
sealed interface ErrStringLike: ErrPrimitiveConstraints

object ErrEmptyString : ErrStringMaxLen, ErrStringLike
class ErrStringTooLong(val maxLen: Int) : ErrStringMaxLen
class ErrPatternUnmatched(val pattern: String, val string: String) : ErrStringLike

class ErrNumberLessThanMin(val min: Number) : ErrNumberInBetween
class ErrNumberGreaterThanMax(val max: Number) : ErrNumberInBetween

// ===============================
// Reusable validation logic for constrained types
// ===============================

/// Useful functions for constrained types
object ConstrainedType {
    /// Create a constrained string using the constructor provided
    /// Return IllegalArgumentException if input is null, empty, or length > maxLen
    context(r: Raise<ErrPrimitiveConstraints>)
    fun <T> ensureStringMaxLen(
        maxLen: Int,
        i: String,
        ctor: () -> T,
    ): T {
        r.ensure(i.isNotEmpty()) { ErrEmptyString }
        r.ensure(i.length <= maxLen) { ErrStringTooLong(maxLen) }
        return ctor()
    }

    /// Create a constrained integer using the constructor provided
    /// Return IllegalArgumentException if input is less than minVal or more than maxVal
    context(r: Raise<ErrPrimitiveConstraints>)
    fun <T> ensureIntInBetween(
        minVal: Int,
        maxVal: Int,
        i: Int,
        ctor: () -> T,
    ): T {
        r.ensure(minVal <= i) { ErrNumberLessThanMin(minVal) }
        r.ensure(i <= maxVal) { ErrNumberGreaterThanMax(maxVal) }
        return ctor()
    }

    /// Create a constrained decimal using the constructor provided
    /// Return IllegalArgumentException if input is less than minVal or more than maxVal
    context(r: Raise<ErrPrimitiveConstraints>)
    fun <T> ensureDoubleInBetween(
        minVal: Double,
        maxVal: Double,
        i: Double,
        ctor: () -> T,
    ): T {
        r.ensure(minVal <= i) { ErrNumberLessThanMin(minVal) }
        r.ensure(i <= maxVal) { ErrNumberGreaterThanMax(maxVal) }
        return ctor()
    }

    /// Create a constrained string using the constructor provided
    /// Return IllegalArgumentException if input is null. empty, or does not match the regex pattern
    context(r: Raise<ErrPrimitiveConstraints>)
    fun <T> ensureStringLike(
        pattern: String,
        i: String,
        ctor: () -> T,
    ): T {
        r.ensure(i.isNotEmpty()) { ErrEmptyString }
        r.ensure(pattern.toRegex().matches(i)) { ErrPatternUnmatched(pattern, i) }
        return ctor()
    }

}
