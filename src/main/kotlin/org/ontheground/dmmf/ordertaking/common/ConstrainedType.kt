package org.ontheground.dmmf.ordertaking.common

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