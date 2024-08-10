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


class String50Spec : DescribeSpec({

    describe("String50") {
        context("문자열 길이가 50 이하인 경우,") {
            val lessThen50String = StringGenerator.generate(50)

            val string50 = String50(lessThen50String)
            it("String50 객체가 정상적으로 생성된다.") {
                string50.shouldBeInstanceOf<String50>()
            }
        }

        context("문자열 길이가 50 초과하는 경우,") {
            val greaterThen50 = StringGenerator.generate(51)

            val exception = shouldThrow<IllegalArgumentException> {
                String50(greaterThen50)
            }

            it("String50 객체 생성에 실패한다.") {
                exception.message should startWith("Must not be more than 50 chars")
            }
        }

        context("문자열이 비어있는 경우,") {
            val exception = shouldThrow<IllegalArgumentException> {
                String50("")
            }

            it("Value 객체 생성에 실패한다.") {
                exception.message should startWith("Must not be null or empty")
            }
        }
    }

    describe("createOption") {
        context("문자열 길이가 50 이하인 경우,") {
            val lessThen50String = StringGenerator.generate(50)

            val string50 = String50.createOption(lessThen50String)
            it("Either.Right.String50 를 응답한다.") {
                string50.shouldBeRight().shouldBeSome()
            }
        }

        context("문자열 길이가 50 초과하는 경우,") {
            val greaterThen50 = StringGenerator.generate(51)
            val string50OrThrowable = String50.createOption(greaterThen50)

            it("Either.Left 를 응답한다.") {
                string50OrThrowable.shouldBeLeft()
            }
        }

        context("문자열이 비어있는 경우,") {
            val string50: Either<Throwable, Option<String50>> = String50.createOption("")

            it("Either.Right.None 을 응답한다.") {
                string50.shouldBeRight().shouldBeNone()
            }
        }
    }


})
