package org.ontheground.dmmf.ordertaking.common

// ==================================
// Common compound types used throughout the OrderTaking domain
//
// Includes: customers, addresses, etc.
// Plus common errors.
//
// ==================================


// ==================================
// Customer-related types
// ==================================

data class PersonalName(
    val firstName : String50,
    val lastName : String50,
)


data class CustomerInfo(
    val Name : PersonalName,
    val EmailAddress : EmailAddress,
)

// ==================================
// Address-related
// ==================================

data class Address(
    val AddressLine1 : String50,
    val AddressLine2 : String50?,
    val AddressLine3 : String50?,
    val AddressLine4 : String50?,
    val City : String50,
    val ZipCode : ZipCode,
)

// ==================================
// Product-related types
// ==================================

// Note that the definition of a Product is in a different bounded
// context, and in this context, products are only represented by a ProductCode
// (see the SimpleTypes module).


