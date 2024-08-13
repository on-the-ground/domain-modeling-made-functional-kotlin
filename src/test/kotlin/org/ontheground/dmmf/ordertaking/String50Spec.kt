package org.ontheground.dmmf.ordertaking

import arrow.core.Either
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.ontheground.dmmf.ordertaking.ConstrainedType.isEmptyStringError
import org.ontheground.dmmf.ordertaking.ConstrainedType.isStringOverMaxLenError

const val stringLen0 = ""
val stringLen50 = StringGenerator.generate(50)
val stringLen51 = StringGenerator.generate(51)

class String50Spec : DescribeSpec({

    describe("String50") {
        context("문자열 길이가 50 이하인 경우,") {
            it("String50 객체가 정상적으로 생성된다.") {
                String50.create(stringLen50).shouldBeRight().shouldBeInstanceOf<String50>()
            }
        }

        context("문자열 길이가 50 초과하는 경우,") {
            it("String50 객체 생성에 실패한다.") {
                String50.create(stringLen51).shouldBeLeft()
                    .isStringOverMaxLenError().shouldBeTrue()
            }
        }

        context("문자열이 비어있는 경우,") {
            it("Value 객체 생성에 실패한다.") {
                String50.create(stringLen0).shouldBeLeft()
                    .isEmptyStringError().shouldBeTrue()
            }
        }
    }

    describe("createNullable") {
        context("문자열 길이가 50 이하인 경우,") {
            val string50 = String50.createNullable(stringLen50)

            it("Either.Right.String50 를 응답한다.") {
                string50.shouldBeRight().shouldBeInstanceOf<String50>()
            }
        }

        context("문자열 길이가 50 초과하는 경우,") {
            val string50OrThrowable = String50.createNullable(stringLen51)

            it("Either.Left 를 응답한다.") {
                string50OrThrowable.shouldBeLeft()
                    .isStringOverMaxLenError().shouldBeTrue()
            }
        }

        context("문자열이 비어있는 경우,") {
            val string50: Either<Throwable, String50?> = String50.createNullable(stringLen0)

            it("Either.Right.null 을 응답한다.") {
                string50.shouldBeRight().shouldBeNull()
            }
        }
    }

})
