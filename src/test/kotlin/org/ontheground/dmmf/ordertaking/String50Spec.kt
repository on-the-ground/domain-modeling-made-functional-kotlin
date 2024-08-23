package org.ontheground.dmmf.ordertaking

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import org.ontheground.dmmf.ordertaking.common.ConstrainedType.isEmptyStringError
import org.ontheground.dmmf.ordertaking.common.ConstrainedType.isStringOverMaxLenError
import org.ontheground.dmmf.ordertaking.common.String50

private const val stringLen0 = ""
private val stringLen50 = StringGenerator.generate(50)
private val stringLen51 = StringGenerator.generate(51)

private fun Throwable.shouldBeStringTooLongError(): kotlin.Unit =
    this.isStringOverMaxLenError().shouldBeTrue()

private fun Throwable.shouldBeEmptyStringError(): kotlin.Unit =
    this.isEmptyStringError().shouldBeTrue()


class String50Spec : DescribeSpec({

    describe("String50") {
        context("문자열 길이가 50 이하인 경우,") {
            it("String50 객체가 정상적으로 생성된다.") {
                String50.create(stringLen50)
                    .shouldBeRight()
                    .shouldBeInstanceOf<String50>()
            }
        }

        context("문자열 길이가 50 초과하는 경우,") {
            it("String50 객체 생성에 실패한다.") {
                String50.create(stringLen51)
                    .shouldBeLeft()
                    .shouldBeStringTooLongError()
            }
        }

        context("문자열이 비어있는 경우,") {
            it("Value 객체 생성에 실패한다.") {
                String50.create(stringLen0)
                    .shouldBeLeft()
                    .shouldBeEmptyStringError()
            }
        }
    }

    describe("createNullable") {
        context("문자열 길이가 50 이하인 경우,") {
            it("Either.Right.String50 를 응답한다.") {
                String50.createNullable(stringLen50)
                    .shouldBeRight()
                    .shouldBeInstanceOf<String50>()
            }
        }

        context("문자열 길이가 50 초과하는 경우,") {
            it("Either.Left 를 응답한다.") {
                String50.createNullable(stringLen51)
                    .shouldBeLeft()
                    .shouldBeStringTooLongError()
            }
        }

        context("문자열이 비어있는 경우,") {
            it("Either.Right.null 을 응답한다.") {
                String50.createNullable(stringLen0)
                    .shouldBeRight()
                    .shouldBeNull()
            }
        }
    }

})
