package org.ontheground.dmmf.ordertaking.placeorder.implementationwoeffects

import arrow.core.raise.Raise
import arrow.core.raise.either
import org.ontheground.dmmf.ordertaking.common.*
import org.ontheground.dmmf.ordertaking.common.domain.base.Entity
import org.ontheground.dmmf.ordertaking.placeorder.*

// ======================================================
// This file contains the implementation for the PlaceOrder workflow
// WITHOUT any effects like Result or Async
//
// This represents the code in chapter 9, "Composing a Pipeline"
//
// There are two parts:
// * the first section contains the (type-only) definitions for each step
// * the second section contains the implementations for each step
//   and the implementation of the overall workflow
// ======================================================


// ------------------------------------
// the workflow itself, without effects


// ======================================================
// Section 1 : Define each step in the workflow using types
// ======================================================

// ---------------------------
// Validation step
// ---------------------------

// Product validation

typealias CheckProductCodeExists = (ProductCode) -> Boolean

// Address validation
sealed interface AddressValidationError
object InvalidFormat : AddressValidationError
object AddressNotFound : AddressValidationError

typealias CheckedAddress = UnvalidatedAddress

typealias CheckAddressExists =
            (UnvalidatedAddress) -> CheckedAddress

// ---------------------------
// Validated Order
// ---------------------------

class ValidatedOrderLine(
    val orderLineId: OrderLineId,
    val productCode: ProductCode,
    val quantity: OrderQuantity,
) : Entity<OrderLineId>() {
    override val id: OrderLineId
        get() = orderLineId
}

class ValidatedOrder(
    val orderId: OrderId,
    val customerInfo: CustomerInfo,
    val shippingAddress: Address,
    val billingAddress: Address,
    val lines: List<ValidatedOrderLine>,
) : Entity<OrderId>() {
    override val id: OrderId
        get() = orderId
}

typealias ValidateOrder = UnvalidatedOrder. // input
    (CheckProductCodeExists, CheckAddressExists)  // dependency
-> ValidatedOrder // output

// ---------------------------
// Pricing step
// ---------------------------

typealias GetProductPrice = (ProductCode) -> Price

// priced state is defined Domain.WorkflowTypes

typealias PriceOrder = ValidatedOrder.  // input
    (GetProductPrice)     // dependency
-> PricedOrder  // output


// ---------------------------
// Send OrderAcknowledgment
// ---------------------------

@JvmInline
value class HtmlString(val value: String)

data class OrderAcknowledgment(
    val emailAddress: EmailAddress,
    val letter: HtmlString,
)

typealias CreateOrderAcknowledgmentLetter =
            (PricedOrder) -> HtmlString

/// Send the order acknowledgement to the customer
/// Note that this does NOT generate an Result-type error (at least not in this workflow)
/// because on failure we will continue anyway.
/// On success, we will generate a OrderAcknowledgmentSent event,
/// but on failure we won't.

sealed interface SendResult
object Sent : SendResult
object NotSent : SendResult

typealias SendOrderAcknowledgment =
            (OrderAcknowledgment) -> SendResult

typealias AcknowledgeOrder = PricedOrder. //input
    (CreateOrderAcknowledgmentLetter, SendOrderAcknowledgment)      // dependency
-> PlaceOrderEvent.OrderAcknowledgmentSent? // output

// ---------------------------
// Create events
// ---------------------------

typealias CreateEvents =
            (PricedOrder, PlaceOrderEvent.OrderAcknowledgmentSent?)    // input (event from previous step)
        -> List<PlaceOrderEvent>              // output

// ======================================================
// Section 2 : Implementation
// ======================================================

// ---------------------------
// ValidateOrder step
// ---------------------------

fun <E, A> throwOnError(
    block: Raise<E>.() -> A,
): A = either { block() }
    .fold(
        ifLeft = { throw RuntimeException(it.toString()) },
        ifRight = { it }
    )

fun UnvalidatedCustomerInfo.toCustomerInfo(): CustomerInfo {
    val unvalidated = this
    val firstName = throwOnError { String50(unvalidated.firstName) }
    val lastName = throwOnError { String50(unvalidated.lastName) }
    val emailAddress = throwOnError { EmailAddress(unvalidated.emailAddress) }
    return CustomerInfo(PersonalName(firstName, lastName), emailAddress)
}


fun CheckedAddress.toAddress(): Address {
    val checked = this
    val addressLine1 = throwOnError { String50(checked.addressLine1) }
    val addressLine2 = checked.addressLine2?.let { throwOnError { String50(it) } }
    val addressLine3 = checked.addressLine3?.let { throwOnError { String50(it) } }
    val addressLine4 = checked.addressLine4?.let { throwOnError { String50(it) } }
    val city = throwOnError { String50(checked.city) }
    val zipCode = throwOnError { ZipCode(checked.zipCode) }
    return Address(addressLine1, addressLine2, addressLine3, addressLine4, city, zipCode)
}


/// Call the checkAddressExists and convert the error to a ValidationError
fun UnvalidatedAddress.toCheckedAddress(checkAddress: CheckAddressExists): CheckedAddress {
    return checkAddress(this)
}

/// Helper function for validateOrder
fun String.toOrderId(): OrderId = throwOnError { OrderId(this@toOrderId) }


/// Helper function for validateOrder
fun String.toOrderLineId(): OrderLineId = throwOnError { OrderLineId(this@toOrderLineId) }


/// Helper function for validateOrder
fun String.toProductCode(checkProductCodeExists: CheckProductCodeExists): ProductCode {
    val code = throwOnError { ProductCode(this@toProductCode) }
    return if (checkProductCodeExists(code)) {
        code
    } else {
        throw RuntimeException("Invalid: ${code}")
    }
}

/// Helper function for validateOrder
fun Double.toOrderQuantity(productCode: ProductCode): OrderQuantity = throwOnError {
    OrderQuantity(productCode, this@toOrderQuantity)
}

/// Helper function for validateOrder
fun UnvalidatedOrderLine.toValidatedOrderLine(checkProductExists: CheckProductCodeExists): ValidatedOrderLine {
    val orderLineId = this.orderLineId.toOrderLineId()
    val productCode = this.productCode.toProductCode(checkProductExists)
    val quantity = this.quantity.toOrderQuantity(productCode)
    return ValidatedOrderLine(orderLineId, productCode, quantity)
}

val validateOrder: ValidateOrder = { checkCodeExists, checkAddressExists ->
    val orderId = this.orderId.toOrderId()
    val customerInfo = this.customerInfo.toCustomerInfo()
    val shippingAddress = this.shippingAddress.toCheckedAddress(checkAddressExists).toAddress()
    val billingAddress = this.billingAddress.toCheckedAddress(checkAddressExists).toAddress()
    val lines = this.lines.map { it.toValidatedOrderLine(checkCodeExists) }
    ValidatedOrder(
        orderId,
        customerInfo,
        shippingAddress,
        billingAddress,
        lines,
    )
}


// ---------------------------
// PriceOrder step
// ---------------------------

fun ValidatedOrderLine.toPricedOrderLine(getProductPrice: GetProductPrice): PricedOrderLine {
    val qty = this.quantity.value()
    val linePrice = throwOnError { getProductPrice(this@toPricedOrderLine.productCode).multiply(qty) }
    return PricedOrderLine(
        this.orderLineId,
        this.productCode,
        this.quantity,
        linePrice,
    )
}

fun ValidatedOrder.priceOrder(getProductPrice: GetProductPrice): PricedOrder {
    val lines = this.lines.map { it.toPricedOrderLine(getProductPrice) }
    val linePrices = lines.map { it.linePrice }
    val amountToBill = throwOnError { BillingAmount.sumPrices(linePrices) }

    return PricedOrder(
        this.orderId,
        this.customerInfo,
        this.shippingAddress,
        this.billingAddress,
        amountToBill,
        lines,
    )
}

// ---------------------------
// AcknowledgeOrder step
// ---------------------------

val acknowledgeOrder: AcknowledgeOrder = { createAck, sendAck ->
    val ack = OrderAcknowledgment(
        this.customerInfo.emailAddress,
        letter = createAck(this),
    )
    // if the acknowledgement was successfully sent,
    // return the corresponding event, else return None
    when (sendAck(ack)) {
        Sent -> PlaceOrderEvent.OrderAcknowledgmentSent(
            this.orderId,
            this.customerInfo.emailAddress,
        )

        NotSent -> null
    }
}


// ---------------------------
// Create events
// ---------------------------

fun PricedOrder.createOrderPlacedEvent() = PlaceOrderEvent.OrderPlaced(
    this.orderId,
    this.customerInfo,
    this.shippingAddress,
    this.billingAddress,
    this.amountToBill,
    this.lines,
)

fun PricedOrder.createBillingEvent(): PlaceOrderEvent.BillableOrderPlaced? {
    val billingAmount = this.amountToBill.value
    return if (billingAmount > 0) {
        PlaceOrderEvent.BillableOrderPlaced(
            this.orderId,
            this.billingAddress,
            this.amountToBill,
        )
    } else {
        null
    }
}


val createEvents: CreateEvents = { pricedOrder, ackSent ->
    val acknowledgmentEvents = ackSent?.let { listOf(it) } ?: emptyList()
    val orderPlacedEvents = listOf(pricedOrder.createOrderPlacedEvent())
    val billingEvents = pricedOrder.createBillingEvent()?.let { listOf(it) } ?: emptyList()
    // return all the events
    acknowledgmentEvents + orderPlacedEvents + billingEvents
}

// ---------------------------
// overall workflow
// ---------------------------

fun UnvalidatedOrder.placeOrder(
    checkCodeExists: CheckProductCodeExists,
    checkAddressExists: CheckAddressExists,
    getProductPrice: GetProductPrice,
    createOrderAcknowledgmentLetter: CreateOrderAcknowledgmentLetter,
    sendOrderAcknowledgment: SendOrderAcknowledgment,
): List<PlaceOrderEvent> {
    val validatedOrder = this@placeOrder.validateOrder(checkCodeExists, checkAddressExists)

    val pricedOrder = validatedOrder.priceOrder(getProductPrice)

    val acknowledgementOption = pricedOrder.acknowledgeOrder(createOrderAcknowledgmentLetter, sendOrderAcknowledgment)

    return createEvents(pricedOrder, acknowledgementOption)
}

