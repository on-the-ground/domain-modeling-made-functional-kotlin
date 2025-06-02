package org.ontheground.dmmf.ordertaking.placeorder.implementation

import arrow.core.raise.Raise
import arrow.core.raise.withError
import org.ontheground.dmmf.ordertaking.common.*
import org.ontheground.dmmf.ordertaking.common.domain.base.Entity
import org.ontheground.dmmf.ordertaking.placeorder.*

// ======================================================
// This file contains the final implementation for the PlaceOrder workflow
//
// This represents the code in chapter 10, "Working with Errors"
//
// There are two parts:
// * the first section contains the (type-only) definitions for each step
// * the second section contains the implementations for each step
//   and the implementation of the overall workflow
// ======================================================


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

@JvmInline
value class CheckedAddress(val value: UnvalidatedAddress)

typealias CheckAddressExists =
        suspend context(Raise<AddressValidationError>) (UnvalidatedAddress) -> CheckedAddress

interface AddressValidator {
    val checkAddressExists: CheckAddressExists
}

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

typealias ValidateOrder = suspend context(Raise<ValidationError>, AddressValidator) // effects
UnvalidatedOrder. // input
    (CheckProductCodeExists)  // dependency
-> ValidatedOrder // output

// ---------------------------
// Pricing step
// ---------------------------

typealias GetProductPrice = (ProductCode) -> Price

// priced state is defined Domain.WorkflowTypes

typealias PriceOrder = context(Raise<PricingError>) // effects
ValidatedOrder.  // input
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
/// Note that this does NOT generate a Result-type error (at least not in this workflow)
/// because on failure we will continue anyway.
/// On success, we will generate an OrderAcknowledgmentSent event,
/// but on failure we won't.

sealed interface SendResult
object Sent : SendResult
object NotSent : SendResult

typealias SendOrderAcknowledgment =
            (OrderAcknowledgment) -> SendResult

interface AcknowledgmentSender {
    val sendOrderAcknowledgment: SendOrderAcknowledgment
}

typealias AcknowledgeOrder = context(AcknowledgmentSender) // ambient context
PricedOrder. //input
    (CreateOrderAcknowledgmentLetter)      // dependency
-> PlaceOrderEvent.OrderAcknowledgmentSent? // output

// ---------------------------
// Create events
// ---------------------------

typealias CreateEvents =
            (PricedOrder, PlaceOrderEvent.OrderAcknowledgmentSent?)    // input (event from the previous step)
        -> List<PlaceOrderEvent>              // output

// ======================================================
// Section 2 : Implementation
// ======================================================

// ---------------------------
// ValidateOrder step
// ---------------------------

context(r: Raise<ValidationError>)
fun UnvalidatedCustomerInfo.toCustomerInfo(): CustomerInfo = r.withError(
    { ValidationError(it.toString()) },
    {
        val unvalidated = this@toCustomerInfo
        val firstName = String50(unvalidated.firstName)
        val lastName = String50(unvalidated.lastName)
        val emailAddress = EmailAddress(unvalidated.emailAddress)
        CustomerInfo(PersonalName(firstName, lastName), emailAddress)
    },
)


context(r: Raise<ValidationError>)
fun CheckedAddress.toAddress(): Address = r.withError(
    { ValidationError(it.toString()) },
    {
        val checked = this@toAddress.value
        val addressLine1 = String50(checked.addressLine1)
        val addressLine2 = checked.addressLine2?.let { String50(it) }
        val addressLine3 = checked.addressLine3?.let { String50(it) }
        val addressLine4 = checked.addressLine4?.let { String50(it) }
        val city = String50(checked.city)
        val zipCode = ZipCode(checked.zipCode)
        Address(addressLine1, addressLine2, addressLine3, addressLine4, city, zipCode)
    },
)


/// Call the checkAddressExists and convert the error to a ValidationError
context(r: Raise<ValidationError>, av: AddressValidator)
suspend fun UnvalidatedAddress.toCheckedAddress(): CheckedAddress =
    r.withError<ValidationError, AddressValidationError, CheckedAddress>(
        { ValidationError(it.toString()) },
        { av.checkAddressExists(this@toCheckedAddress) },
    )

/// Helper function for validateOrder
context(r: Raise<ValidationError>)
fun String.toOrderId(): OrderId = r.withError(
    { ValidationError(it.toString()) },
    { OrderId(this@toOrderId) },
)

/// Helper function for validateOrder
context(r: Raise<ValidationError>)
fun String.toOrderLineId(): OrderLineId = r.withError(
    { ValidationError(it.toString()) },
    { OrderLineId(this@toOrderLineId) },
)


/// Helper function for validateOrder
context(r: Raise<ValidationError>)
fun String.toProductCode(checkProductCodeExists: CheckProductCodeExists): ProductCode = r.withError(
    { ValidationError(it.toString()) },
    {
        val code = ProductCode(this@toProductCode)
        if (checkProductCodeExists(code)) {
            code
        } else {
            r.raise(ValidationError("Invalid: ${code}"))
        }
    },
)

/// Helper function for validateOrder
context(r: Raise<ValidationError>)
fun Double.toOrderQuantity(productCode: ProductCode): OrderQuantity = r.withError(
    { ValidationError(it.toString()) },
    { OrderQuantity(productCode, this@toOrderQuantity) },
)

/// Helper function for validateOrder
context(r: Raise<ValidationError>)
fun UnvalidatedOrderLine.toValidatedOrderLine(checkProductExists: CheckProductCodeExists): ValidatedOrderLine {
    val orderLineId = this.orderLineId.toOrderLineId()
    val productCode = this.productCode.toProductCode(checkProductExists)
    val quantity = this.quantity.toOrderQuantity(productCode)
    return ValidatedOrderLine(orderLineId, productCode, quantity)
}

val validateOrder: ValidateOrder = { checkCodeExists ->
    ValidatedOrder(
        orderId = this.orderId.toOrderId(),
        customerInfo = this.customerInfo.toCustomerInfo(),
        shippingAddress = this.shippingAddress.toCheckedAddress().toAddress(),
        billingAddress = this.billingAddress.toCheckedAddress().toAddress(),
        lines = this.lines.map { it.toValidatedOrderLine(checkCodeExists) },
    )
}


// ---------------------------
// PriceOrder step
// ---------------------------

context(r: Raise<PricingError>)
fun ValidatedOrderLine.toPricedOrderLine(getProductPrice: GetProductPrice): PricedOrderLine {
    val qty = this.quantity.value()
    val linePrice = r.withError(
        { PricingError(it.toString()) },
        { getProductPrice(this@toPricedOrderLine.productCode).multiply(qty) },
    )
    return PricedOrderLine(
        this.orderLineId,
        this.productCode,
        this.quantity,
        linePrice,
    )
}

context(r: Raise<PricingError>)
fun ValidatedOrder.priceOrder(getProductPrice: GetProductPrice): PricedOrder {
    val lines = this.lines.map { it.toPricedOrderLine(getProductPrice) }
    val linePrices = lines.map { it.linePrice }
    val amountToBill = r.withError(
        { PricingError(it.toString()) },
        { BillingAmount.sumPrices(linePrices) }

    )
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

context(aS: AcknowledgmentSender)
fun PricedOrder.acknowledgeOrder(createAck: CreateOrderAcknowledgmentLetter) =
    // if the acknowledgement was successfully sent,
    // return the corresponding event, else return None
    OrderAcknowledgment(this.customerInfo.emailAddress, createAck(this))
        .let {
            when (aS.sendOrderAcknowledgment(it)) {
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

context(r: Raise<PlaceOrderError>, _: AddressValidator, _: AcknowledgmentSender)
suspend fun UnvalidatedOrder.placeOrder(
    checkCodeExists: CheckProductCodeExists,
    getProductPrice: GetProductPrice,
    createOrderAcknowledgmentLetter: CreateOrderAcknowledgmentLetter,
): List<PlaceOrderEvent> {
    val validatedOrder = r.withError(
        { ValidationError(it.toString()) },
        { this@placeOrder.validateOrder(checkCodeExists) },
    )

    val pricedOrder = r.withError(
        { PricingError(it.toString()) },
        { validatedOrder.priceOrder(getProductPrice) }
    )

    return pricedOrder.toPlaceOrderEvents(createOrderAcknowledgmentLetter)
}

context(_: AcknowledgmentSender)
fun PricedOrder.toPlaceOrderEvents(createAck: CreateOrderAcknowledgmentLetter): List<PlaceOrderEvent> =
    createEvents(this, this.acknowledgeOrder(createAck))

