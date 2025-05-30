package org.ontheground.dmmf.ordertaking.placeorder

import arrow.core.raise.Raise
import org.ontheground.dmmf.ordertaking.common.*
import org.ontheground.dmmf.ordertaking.common.domain.base.Entity

// We are defining types and submodules, so we can use a namespace
// rather than a module at the top level

// ==================================
// This file contains the definitions of PUBLIC types (exposed at the boundary of the bounded context)
// related to the PlaceOrder workflow
// ==================================

// ------------------------------------
// inputs to the workflow

data class UnvalidatedCustomerInfo(
    val firstName: String,
    val lastName: String,
    val emailAddress: String,
)

data class UnvalidatedAddress(
    val addressLine1: String,
    val addressLine2: String?,
    val addressLine3: String?,
    val addressLine4: String?,
    val city: String,
    val zipCode: String,
)

data class UnvalidatedOrderLine(
    val orderLineId: String,
    val productCode: String,
    val quantity: Double,
)

data class UnvalidatedOrder(
    val orderId: String,
    val customerInfo: UnvalidatedCustomerInfo,
    val shippingAddress: UnvalidatedAddress,
    val billingAddress: UnvalidatedAddress,
    val lines: List<UnvalidatedOrderLine>,
)


// ------------------------------------
// outputs from the workflow (success case)


// priced state
class PricedOrderLine(
    val orderLineId: OrderLineId,
    val productCode: ProductCode,
    val quantity: OrderQuantity,
    val linePrice: Price,
) : Entity<OrderLineId>() {
    override val id: OrderLineId
        get() = orderLineId
}


class PricedOrder(
    val orderId: OrderId,
    val customerInfo: CustomerInfo,
    val shippingAddress: Address,
    val billingAddress: Address,
    val amountToBill: BillingAmount,
    val lines: List<PricedOrderLine>,
) : Entity<OrderId>() {
    override val id: OrderId
        get() = orderId
}

/// The possible events resulting from the PlaceOrder workflow
/// Not all events will occur, depending on the logic of the workflow
sealed interface PlaceOrderEvent {
    /// Event to send to shipping context
    data class OrderPlaced(
        val orderId: OrderId,
        val customerInfo: CustomerInfo,
        val shippingAddress: Address,
        val billingAddress: Address,
        val amountToBill: BillingAmount,
        val lines: List<PricedOrderLine>,
    ) : PlaceOrderEvent

    /// Event to send to billing context
    /// Will only be created if the AmountToBill is not zero
    data class BillableOrderPlaced(
        val orderId: OrderId,
        val billingAddress: Address,
        val amountToBill: BillingAmount,
    ) : PlaceOrderEvent

    /// Event will be created if the Acknowledgment was successfully posted
    data class OrderAcknowledgmentSent(
        val orderId: OrderId,
        val emailAddress: EmailAddress,
    ) : PlaceOrderEvent
}


// ------------------------------------
// error outputs


/// All the things that can go wrong in this workflow
@JvmInline
value class ValidationError(val value: String) : PlaceOrderError

@JvmInline
value class PricingError(val value: String) : PlaceOrderError

data class ServiceInfo(
    val name: String,
    val endpoint: String, // todo uri
)

data class RemoteServiceError(
    val service: ServiceInfo,
    val exception: Throwable, // todo System.Exception
) : PlaceOrderError


sealed interface PlaceOrderError


// ------------------------------------
// the workflow itself

typealias PlaceOrder =
        suspend context(Raise<PlaceOrderError>) UnvalidatedOrder.() -> List<PlaceOrderEvent>



