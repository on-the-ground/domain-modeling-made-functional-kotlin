package org.ontheground.dmmf.ordertaking.placeorder.api

import arrow.core.raise.recover
import arrow.core.raise.withError
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ontheground.dmmf.ordertaking.common.Price
import org.ontheground.dmmf.ordertaking.placeorder.OrderFormDto
import org.ontheground.dmmf.ordertaking.placeorder.PlaceOrderError
import org.ontheground.dmmf.ordertaking.placeorder.ValidationError
import org.ontheground.dmmf.ordertaking.placeorder.implementation.*
import org.ontheground.dmmf.ordertaking.placeorder.toDto

// ======================================================
// This file contains the JSON API interface to the PlaceOrder workflow
//
// 1) The HttpRequest is turned into a DTO, which is then turned into a Domain object
// 2) The main workflow function is called
// 3) The output is turned into a DTO which is turned into a HttpResponse
// ======================================================

typealias JsonString = String

inline fun <reified T> JsonString.toDto(): T = Json.decodeFromString(this)
inline fun <reified T> T.toJson(): JsonString = Json.encodeToString(this)

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

val placeOrderApi: PlaceOrderApi = {
    // following the approach in "A Complete Serialization Pipeline" in chapter 11
    recover<PlaceOrderError, HttpResponse>(
        {
            // start with a string
            val orderFormDto = it.body.toDto<OrderFormDto>()

            val placeOrderEvents = orderFormDto
                .toUnvalidatedOrder()
                .placeOrder(
                    checkProductExists, // dependency
                    checkAddressExists, // dependency
                    getProductPrice,    // dependency
                    createOrderAcknowledgmentLetter,  // dependency
                    sendOrderAcknowledgment, // dependency
                )

            withError({ ValidationError(it.toString()) }, {
                placeOrderEvents
                    .map { it.toDto() }
                    .toJson()
                    .let { HttpResponse(200, it) }
            })
        },
        {
            it.toDto() // turn domain errors into a dto
                .toJson() // and serialize to JSON
                .let { HttpResponse(401, it) }
        },
    )
}

