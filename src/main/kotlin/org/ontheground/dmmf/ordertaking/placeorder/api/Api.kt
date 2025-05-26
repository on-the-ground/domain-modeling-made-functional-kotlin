package org.ontheground.dmmf.ordertaking.placeorder.api

import arrow.core.Either
import arrow.core.raise.either
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ontheground.dmmf.ordertaking.common.ErrPrimitiveConstraints
import org.ontheground.dmmf.ordertaking.common.Price
import org.ontheground.dmmf.ordertaking.placeorder.*
import org.ontheground.dmmf.ordertaking.placeorder.implementation.*

// ======================================================
// This file contains the JSON API interface to the PlaceOrder workflow
//
// 1) The HttpRequest is turned into a DTO, which is then turned into a Domain object
// 2) The main workflow function is called
// 3) The output is turned into a DTO which is turned into a HttpResponse
// ======================================================

typealias JsonString = String

inline fun <reified T> JsonString.deserialize() = Json.decodeFromString<T>(this)
inline fun <reified T> serializeJson(i: T): JsonString {
    return Json.encodeToString(i)
}

/// Very simplified version!
data class HttpRequest(
    val action: String,
    val uri: String,
    val body: JsonString,
)

/// Very simplified version!
data class HttpResponse(
    val httpStatusCode: Int,
    val body: JsonString,
)

/// An API takes a HttpRequest as input and returns a async response
typealias PlaceOrderApi = suspend (HttpRequest) -> HttpResponse


// =============================
// JSON serialization
// =============================

// =============================
// Implementation
// =============================

// setup dummy dependencies
private val checkProductExists: CheckProductCodeExists = { true } // dummy implementation
private val checkAddressExists: CheckAddressExists = { it }
private val getProductPrice: GetProductPrice = { Price.unsafeCreate(1.0) }  // dummy implementation
private val createOrderAcknowledgmentLetter: CreateOrderAcknowledgmentLetter = { HtmlString("some text") }
private val sendOrderAcknowledgment: SendOrderAcknowledgment = { Sent }


// -------------------------------
// workflow
// -------------------------------

/// This function converts the workflow output into a HttpResponse
fun Either<PlaceOrderError, List<PlaceOrderEvent>>.workflowResultToHttpReponse() = this.fold<HttpResponse>(
    {
        // turn domain errors into a dto
        val dto = it.toDto()
        // and serialize to JSON
        HttpResponse(401, serializeJson<PlaceOrderErrorDto>(dto))
    },
    {
        // turn domain events into dtos
        either<ErrPrimitiveConstraints, List<PlaceOrderEventDto>> {
            it.map { it.toDto() }
        }.fold(
            {
                HttpResponse(401, it.toString())
            },
            {
                // and serialize to JSON
                HttpResponse(200, serializeJson<List<PlaceOrderEventDto>>(it))
            }
        )
    },
)

val placeOrderApi: PlaceOrderApi = {
// following the approach in "A Complete Serialization Pipeline" in chapter 11

// start with a string
    either<PlaceOrderError, List<PlaceOrderEvent>> {
        it.body
            .deserialize<OrderFormDto>()
            .toUnvalidatedOrder()
            .placeOrder(
                checkProductExists, // dependency
                checkAddressExists, // dependency
                getProductPrice,    // dependency
                createOrderAcknowledgmentLetter,  // dependency
                sendOrderAcknowledgment, // dependency
            )
    }.workflowResultToHttpReponse() // now convert from the pure domain back to a HttpResponse
}

