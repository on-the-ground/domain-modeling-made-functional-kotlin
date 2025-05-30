package org.ontheground.dmmf.ordertaking.placeorder

import arrow.core.raise.Raise
import kotlinx.serialization.Serializable
import org.ontheground.dmmf.ordertaking.common.*

// ======================================================
// This file contains the logic for working with data transfer objects (DTOs)
//
// This represents the code in chapter 11, "Serialization"
//
// Each type of DTO is defined using primitive, serializable types
// and then there are `toDomain` and `toDto` functions defined for each DTO.
//
// ======================================================


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
    /// Convert the DTO into a UnvalidatedCustomerInfo object.
    /// This always succeeds because there is no validation.
    /// Used when importing an OrderForm from the outside world into the domain.
    fun toUnvalidatedCustomerInfo() = UnvalidatedCustomerInfo(
        // this is a simple 1:1 copy which always succeeds
        this.firstName,
        this.lastName,
        this.emailAddress,
    )

    /// Convert the DTO into a CustomerInfo object
    /// Used when importing from the outside world into the domain, eg loading from a database
    context(r: Raise<ErrPrimitiveConstraints>)
    fun toDomain() = CustomerInfo(
        PersonalName(
            String50(this.firstName),
            String50(this.lastName),
        ),
        EmailAddress(this.emailAddress),
    )

}

/// Convert a CustomerInfo object into the corresponding DTO.
/// Used when exporting from the domain to the outside world.
context(_: Raise<ErrPrimitiveConstraints>)
fun CustomerInfo.toDto() = CustomerInfoDto(
    // this is a simple 1:1 copy
    this.name.firstName.value,
    this.name.lastName.value,
    this.emailAddress.value,
)

//===============================================
// DTO for Address
//===============================================

@Serializable
data class AddressDto(
    val addressLine1: String,
    val addressLine2: String? = "",
    val addressLine3: String? = "",
    val addressLine4: String? = "",
    val city: String,
    val zipCode: String,
) {

    /// Convert the DTO into a UnvalidatedAddress
    /// This always succeeds because there is no validation.
    /// Used when importing an OrderForm from the outside world into the domain.
    fun toUnvalidatedAddress() = UnvalidatedAddress(
        // this is a simple 1:1 copy
        this.addressLine1,
        this.addressLine2,
        this.addressLine3,
        this.addressLine4,
        this.city,
        this.zipCode,
    )

    /// Convert the DTO into a Address object
    /// Used when importing from the outside world into the domain, eg loading from a database.
    context(r: Raise<ErrPrimitiveConstraints>)
    fun toAddress() = Address(
        String50(this.addressLine1),
        this.addressLine2?.let { String50(it) },
        this.addressLine3?.let { String50(it) },
        this.addressLine4?.let { String50(it) },
        String50(this.city),
        ZipCode(this.zipCode),
    )
}

/// Convert a Address object into the corresponding DTO.
/// Used when exporting from the domain to the outside world.
fun Address.toDto() = AddressDto(
    // this is a simple 1:1 copy
    this.addressLine1.value,
    this.addressLine2?.value,
    this.addressLine3?.value,
    this.addressLine4?.value,
    this.city.value,
    this.zipCode.value,
)

//===============================================
// DTOs for OrderLines
//===============================================

/// From the order form used as input
@Serializable
data class OrderFormLineDto(
    val orderLineId: String,
    val productCode: String,
    val quantity: Double,
) {
    /// Convert the OrderFormLine into a UnvalidatedOrderLine
    /// This always succeeds because there is no validation.
    /// Used when importing an OrderForm from the outside world into the domain.
    fun toUnvalidatedOrderLine() = UnvalidatedOrderLine(
        // this is a simple 1:1 copy
        this.orderLineId,
        this.productCode,
        this.quantity,
    )
}


//===============================================
// DTOs for PricedOrderLines
//===============================================

/// Used in the output of the workflow
@Serializable
data class PricedOrderLineDto(
    val orderLineId: String,
    val productCode: String,
    val quantity: Double,
    val linePrice: Double,
)

/// Convert a PricedOrderLine object into the corresponding DTO.
/// Used when exporting from the domain to the outside world.
fun PricedOrderLine.toDto() = PricedOrderLineDto(
    // this is a simple 1:1 copy
    this.orderLineId.value,
    this.productCode.value(),
    this.quantity.value(),
    this.linePrice.value,
)


//===============================================
// DTO for OrderForm
//===============================================

@Serializable
data class OrderFormDto(
    val orderId: String,
    val customerInfo: CustomerInfoDto,
    val shippingAddress: AddressDto,
    val billingAddress: AddressDto,
    val lines: List<OrderFormLineDto> = emptyList(),
) {
    /// Convert the OrderForm into a UnvalidatedOrder
    /// This always succeeds because there is no validation.
    fun toUnvalidatedOrder() = UnvalidatedOrder(
        this.orderId,
        this.customerInfo.toUnvalidatedCustomerInfo(),
        this.shippingAddress.toUnvalidatedAddress(),
        this.billingAddress.toUnvalidatedAddress(),
        this.lines.map { it.toUnvalidatedOrderLine() },
    )
}


//===============================================
// DTO for OrderPlaced event
//===============================================


/// Event to send to shipping context
@Serializable
data class OrderPlacedDto(
    val orderId: String,
    val customerInfo: CustomerInfoDto,
    val shippingAddress: AddressDto,
    val billingAddress: AddressDto,
    val amountToBill: Double,
    val lines: List<PricedOrderLineDto>,
)

/// Convert a OrderPlaced object into the corresponding DTO.
/// Used when exporting from the domain to the outside world.
context(_: Raise<ErrPrimitiveConstraints>)
fun PlaceOrderEvent.OrderPlaced.toDto() = OrderPlacedDto(
    this.orderId.value,
    this.customerInfo.toDto(),
    this.shippingAddress.toDto(),
    this.billingAddress.toDto(),
    this.amountToBill.value,
    this.lines.map { it.toDto() },
)


//===============================================
// DTO for BillableOrderPlaced event
//===============================================

/// Event to send to billing context
@Serializable
data class BillableOrderPlacedDto(
    val orderId: String,
    val billingAddress: AddressDto,
    val amountToBill: Double,
)

/// Convert a BillableOrderPlaced object into the corresponding DTO.
/// Used when exporting from the domain to the outside world.
context(_: Raise<ErrPrimitiveConstraints>)
fun PlaceOrderEvent.BillableOrderPlaced.toDto() = BillableOrderPlacedDto(
    this.orderId.value,
    this.billingAddress.toDto(),
    this.amountToBill.value,
)


//===============================================
// DTO for OrderAcknowledgmentSent event
//===============================================

/// Event to send to other bounded contexts
@Serializable
data class OrderAcknowledgmentSentDto(
    val orderId: String,
    val emailAddress: String,
)

/// Convert a OrderAcknowledgmentSent object into the corresponding DTO.
/// Used when exporting from the domain to the outside world.
context(_: Raise<ErrPrimitiveConstraints>)
fun PlaceOrderEvent.OrderAcknowledgmentSent.toDto() = OrderAcknowledgmentSentDto(
    this.orderId.value,
    this.emailAddress.value,
)


//===============================================
// DTO for PlaceOrderEvent
//===============================================

/// Use a dictionary representation of a PlaceOrderEvent, suitable for JSON
/// See "Serializing Records and Choice Types Using Maps" in chapter 11
typealias PlaceOrderEventDto = Map<String, Any>

/// Convert a PlaceOrderEvent into the corresponding DTO.
/// Used when exporting from the domain to the outside world.
context(_: Raise<ErrPrimitiveConstraints>)
fun PlaceOrderEvent.toDto(): PlaceOrderEventDto = when (this) {
    is PlaceOrderEvent.OrderPlaced -> mapOf("OrderPlaced" to this.toDto())
    is PlaceOrderEvent.BillableOrderPlaced -> mapOf("BillableOrderPlaced" to this.toDto())
    is PlaceOrderEvent.OrderAcknowledgmentSent -> mapOf("OrderAcknowledgmentSent" to this.toDto())
}

//===============================================
// DTO for PlaceOrderError
//===============================================

@Serializable
data class PlaceOrderErrorDto(
    val code: String,
    val message: String,
)

fun PlaceOrderError.toDto() = when (this) {
    is ValidationError -> PlaceOrderErrorDto("ValidationError", this.value)

    is PricingError -> PlaceOrderErrorDto("PricingError", this.value)

    is RemoteServiceError -> PlaceOrderErrorDto(
        "RemoteServiceError",
        "${this.service.name}: ${this.exception.message}"
    )
}




