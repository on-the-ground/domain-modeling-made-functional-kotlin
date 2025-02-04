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
    val name : PersonalName,
    val emailAddress : EmailAddress,
)

// ==================================
// Address-related
// ==================================

data class Address(
    val addressLine1 : String50,
    val addressLine2 : String50?,
    val addressLine3 : String50?,
    val addressLine4 : String50?,
    val city : String50,
    val zipCode : ZipCode,
)

// ==================================
// Product-related types
// ==================================

// Note that the definition of a Product is in a different bounded
// context, and in this context, products are only represented by a ProductCode
// (see the SimpleTypes module).


