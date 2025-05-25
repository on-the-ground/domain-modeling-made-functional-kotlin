package org.ontheground.dmmf.ordertaking.placeorder

import arrow.core.raise.Raise
import arrow.core.raise.withError
import org.ontheground.dmmf.ordertaking.common.*

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

typealias CheckedAddress = UnvalidatedAddress

typealias CheckAddressExists =
        suspend context(Raise<AddressValidationError>) (UnvalidatedAddress) -> CheckedAddress

// ---------------------------
// Validated Order
// ---------------------------

class ValidatedOrderLine(
    val orderLineId: OrderLineId,
    val productCode: ProductCode,
    val quantity: OrderQuantity,
)

class ValidatedOrder(
    val orderId: OrderId,
    val customerInfo: CustomerInfo,
    val shippingAddress: Address,
    val billingAddress: Address,
    val lines: List<ValidatedOrderLine>,
)

typealias ValidateOrder = suspend context(Raise<ValidationError>) // effects
UnvalidatedOrder. // input
    (CheckProductCodeExists, CheckAddressExists)  // dependency
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
-> OrderAcknowledgmentSent? // output

// ---------------------------
// Create events
// ---------------------------

typealias CreateEvents =
            (PricedOrder, OrderAcknowledgmentSent?)    // input (event from previous step)
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
        val checked = this@toAddress
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
context(r: Raise<ValidationError>)
suspend fun UnvalidatedAddress.toCheckedAddress(checkAddress: CheckAddressExists): CheckedAddress =
    r.withError<ValidationError, AddressValidationError, CheckedAddress>(
        { ValidationError(it.toString()) },
        { checkAddress(this@toCheckedAddress) },
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

context(r: Raise<PricingError>)
fun ValidatedOrderLine.toPricedOrderLine(getProductPrice: GetProductPrice): PricedOrderLine {
    val qty = when (val q = this.quantity) {
        is OrderQuantity.Kilogram -> q.value
        is OrderQuantity.Unit -> q.value.toDouble()
    }
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
    val amountToBill = r.withError<PricingError, ErrPrimitiveConstraints, BillingAmount>(
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

val acknowledgeOrder: AcknowledgeOrder = { createAck, sendAck ->
    val ack = OrderAcknowledgment(
        this.customerInfo.emailAddress,
        letter = createAck(this),
    )
    // if the acknowledgement was successfully sent,
    // return the corresponding event, else return None
    when (sendAck(ack)) {
        Sent -> OrderAcknowledgmentSent(
            this.orderId,
            this.customerInfo.emailAddress,
        )

        NotSent -> null
    }
}


// ---------------------------
// Create events
// ---------------------------

fun PricedOrder.createOrderPlacedEvent(): OrderPlaced = OrderPlaced(
    this.orderId,
    this.customerInfo,
    this.shippingAddress,
    this.billingAddress,
    this.amountToBill,
    this.lines,
)

fun PricedOrder.createBillingEvent(): BillableOrderPlaced? {
    val billingAmount = this.amountToBill.value
    return if (billingAmount > 0) {
        BillableOrderPlaced(
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

context(r: Raise<PlaceOrderError>)
suspend fun UnvalidatedOrder.placeOrder(
    checkCodeExists: CheckProductCodeExists,
    checkAddressExists: CheckAddressExists,
    getProductPrice: GetProductPrice,
    createOrderAcknowledgmentLetter: CreateOrderAcknowledgmentLetter,
    sendOrderAcknowledgment: SendOrderAcknowledgment,
): List<PlaceOrderEvent> {
    val validatedOrder = r.withError(
        { ValidationError(it.toString()) },
        { this@placeOrder.validateOrder(checkCodeExists, checkAddressExists) },
    )

    val pricedOrder = r.withError(
        { PricingError(it.toString()) },
        { validatedOrder.priceOrder(getProductPrice) }
    )

    val acknowledgementOption = pricedOrder.acknowledgeOrder(createOrderAcknowledgmentLetter, sendOrderAcknowledgment)

    return createEvents(pricedOrder, acknowledgementOption)
}

