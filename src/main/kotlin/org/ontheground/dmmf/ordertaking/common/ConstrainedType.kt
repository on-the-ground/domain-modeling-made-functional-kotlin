package org.ontheground.dmmf.ordertaking.common

import arrow.core.Either
import arrow.core.raise.either
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
    /// Return Error if input is null, empty, or length > maxLen
    fun <T> ensureStringMaxLen(
        maxLen: Int,
        i: String,
        ctor: () -> T,
    ): Either<Throwable, T> = either {
        ensure(i.isNotEmpty()) { Error(EMPTY_STRING_PRETERMS) }
        ensure(i.length <= maxLen) { Error("$TOO_LONG_STRING_PRETERMS $maxLen chars") }
        ctor()
    }

    /// Create a constrained integer using the constructor provided
    /// Return Error if input is less than minVal or more than maxVal
    fun <T> ensureIntInBetween(
        minVal: Int,
        maxVal: Int,
        i: Int,
        ctor: () -> T,
    ): Either<Throwable, T> = either {
        ensure(minVal <= i) { Error("Must not be less than $minVal") }
        ensure(i <= maxVal) { Error("Must not be greater than $maxVal") }
        ctor()
    }

    /// Create a constrained decimal using the constructor provided
    /// Return Error if input is less than minVal or more than maxVal
    fun requireDoubleInBetween(
        minVal: Double,
        maxVal: Double,
        i: Double
    ): kotlin.Unit {
        require(minVal <= i) { "Must not be less than $minVal" }
        require(i <= maxVal) { "Must not be greater than $maxVal" }
    }


    /// Create a constrained decimal using the constructor provided
    /// Return Error if input is less than minVal or more than maxVal
    fun <T> ensureDoubleInBetween(
        minVal: Double,
        maxVal: Double,
        i: Double,
        ctor: () -> T,
    ): Either<Throwable, T> = either {
        ensure(minVal <= i) { Error("Must not be less than $minVal") }
        ensure(i <= maxVal) { Error("Must not be greater than $maxVal") }
        ctor()
    }

    /// Create a constrained string using the constructor provided
    /// Return Error if input is null. empty, or does not match the regex pattern
    fun <T> ensureStringLike(
        pattern: String,
        i: String,
        ctor: () -> T,
    ): Either<Throwable, T> = either {
        ensure(i.isNotEmpty()) { Error(EMPTY_STRING_PRETERMS) }
        ensure(pattern.toRegex().matches(i)) { Error("'${i}' $PATTERN_UNMATCHED_STRING_PRETERMS '${pattern}'") }
        ctor()
    }

}