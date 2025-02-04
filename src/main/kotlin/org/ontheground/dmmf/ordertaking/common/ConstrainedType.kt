package org.ontheground.dmmf.ordertaking.common

import arrow.core.raise.Raise
import arrow.core.raise.ensure


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
    /// Return IllegalArgumentException if input is null, empty, or length > maxLen
    context(Raise<IllegalArgumentException>)
    fun <T> ensureStringMaxLen(
        maxLen: Int,
        i: String,
        ctor: () -> T,
    ): T {
        ensure(i.isNotEmpty()) { IllegalArgumentException(EMPTY_STRING_PRETERMS) }
        ensure(i.length <= maxLen) { IllegalArgumentException("$TOO_LONG_STRING_PRETERMS $maxLen chars") }
        return ctor()
    }

    /// Create a constrained integer using the constructor provided
    /// Return IllegalArgumentException if input is less than minVal or more than maxVal
    context(Raise<IllegalArgumentException>)
    fun <T> ensureIntInBetween(
        minVal: Int,
        maxVal: Int,
        i: Int,
        ctor: () -> T,
    ): T {
        ensure(minVal <= i) { IllegalArgumentException("Must not be less than $minVal") }
        ensure(i <= maxVal) { IllegalArgumentException("Must not be greater than $maxVal") }
        return ctor()
    }

    /// Create a constrained decimal using the constructor provided
    /// Return IllegalArgumentException if input is less than minVal or more than maxVal
    fun requireDoubleInBetween(
        minVal: Double,
        maxVal: Double,
        i: Double
    ) {
        require(minVal <= i) { "Must not be less than $minVal" }
        require(i <= maxVal) { "Must not be greater than $maxVal" }
    }


    /// Create a constrained decimal using the constructor provided
    /// Return IllegalArgumentException if input is less than minVal or more than maxVal
    context(Raise<IllegalArgumentException>)
    fun <T> ensureDoubleInBetween(
        minVal: Double,
        maxVal: Double,
        i: Double,
        ctor: () -> T,
    ): T {
        ensure(minVal <= i) { IllegalArgumentException("Must not be less than $minVal") }
        ensure(i <= maxVal) { IllegalArgumentException("Must not be greater than $maxVal") }
        return ctor()
    }

    /// Create a constrained string using the constructor provided
    /// Return IllegalArgumentException if input is null. empty, or does not match the regex pattern
    context(Raise<IllegalArgumentException>)
    fun <T> ensureStringLike(
        pattern: String,
        i: String,
        ctor: () -> T,
    ): T {
        ensure(i.isNotEmpty()) { IllegalArgumentException(EMPTY_STRING_PRETERMS) }
        ensure(pattern.toRegex().matches(i)) { IllegalArgumentException("'${i}' $PATTERN_UNMATCHED_STRING_PRETERMS '${pattern}'") }
        return ctor()
    }

}
