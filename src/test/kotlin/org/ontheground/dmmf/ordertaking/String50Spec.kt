package org.ontheground.dmmf.ordertaking

import arrow.core.Either
import arrow.core.Option
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.string.startWith
import io.kotest.matchers.types.shouldBeInstanceOf

val stringLen0 = ""
val stringLen50 = StringGenerator.generate(50)
val stringLen51 = StringGenerator.generate(51)

class String50Spec : DescribeSpec({

    describe("String50") {
        context("문자열 길이가 50 이하인 경우,") {
            val string50 = String50(stringLen50)

            it("String50 객체가 정상적으로 생성된다.") {
                string50.shouldBeInstanceOf<String50>()
            }
        }

        context("문자열 길이가 50 초과하는 경우,") {
            val exception = shouldThrow<IllegalArgumentException> {
                String50(stringLen51)
            }

            it("String50 객체 생성에 실패한다.") {
                exception.message should startWith("Must not be more than 50 chars")
            }
        }

        context("문자열이 비어있는 경우,") {
            val exception = shouldThrow<IllegalArgumentException> {
                String50(stringLen0)
            }

            it("Value 객체 생성에 실패한다.") {
                exception.message should startWith("Must not be null or empty")
            }
        }
    }

    describe("createOption") {
        context("문자열 길이가 50 이하인 경우,") {
            val string50 = String50.createOption(stringLen50)
            it("Either.Right.String50 를 응답한다.") {
                string50.shouldBeRight().shouldBeSome()
            }
        }

        context("문자열 길이가 50 초과하는 경우,") {
            val string50OrThrowable = String50.createOption(stringLen51)

            it("Either.Left 를 응답한다.") {
                string50OrThrowable.shouldBeLeft()
            }
        }

        context("문자열이 비어있는 경우,") {
            val string50: Either<Throwable, Option<String50>> = String50.createOption(stringLen0)

            it("Either.Right.None 을 응답한다.") {
                string50.shouldBeRight().shouldBeNone()
            }
        }
    }


})
