package org.ontheground.dmmf.ordertaking.placeorder.dto

import arrow.core.raise.Raise
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.ontheground.dmmf.ordertaking.common.CustomerInfo
import org.ontheground.dmmf.ordertaking.common.EmailAddress
import org.ontheground.dmmf.ordertaking.common.PersonalName
import org.ontheground.dmmf.ordertaking.common.String50


// ==================================
// DTOs for PlaceOrder workflow
// ==================================

//===============================================
// DTO for CustomerInfo
//===============================================

@Serializable
data class CustomerInfoDto(
    val firstName: String,
    val lastName: String,
    val emailAddress: String,
) {
    /// Convert the DTO into a CustomerInfo object
    /// Used when importing from the outside world into the domain, eg loading from a database
    context(Raise<IllegalArgumentException>)
    fun toDomain() = CustomerInfo(
        name = PersonalName(
            firstName = String50(this.firstName),
            lastName = String50(this.lastName),
        ),
        emailAddress = EmailAddress(this.emailAddress),
    )

    context(Raise<SerializationException>)
    fun toJSON() = Json.encodeToString(this)

    companion object {
        /// Convert a CustomerInfo object into the corresponding DTO.
        /// Used when exporting from the domain to the outside world.
        fun fromDomain(domainObj: CustomerInfo) = CustomerInfoDto(
            domainObj.name.firstName.value,
            domainObj.name.lastName.value,
            domainObj.emailAddress.value,
        )

        context(Raise<SerializationException>)
        fun fromJSON(json: String): CustomerInfoDto = Json.decodeFromString(json)
    }

}

/// Convert the DTO into a UnvalidatedCustomerInfo object.
/// This always succeeds because there is no validation.
/// Used when importing an OrderForm from the outside world into the domain.
//    toUnvalidatedCustomerInfo(): UnvalidatedCustomerInfo {
//        // sometimes it's helpful to use an explicit type annotation
//        // to avoid ambiguity between records with the same field names.
//
//        // this is a simple 1:1 copy which always succeeds
//        return new UnvalidatedCustomerInfo(this.firstName, this.lastName, this.emailAddress);
//    }

