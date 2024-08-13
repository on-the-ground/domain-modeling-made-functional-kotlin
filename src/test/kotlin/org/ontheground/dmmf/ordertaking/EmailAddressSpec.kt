package org.ontheground.dmmf.ordertaking

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.ontheground.dmmf.ordertaking.ConstrainedType.isEmptyStringError
import org.ontheground.dmmf.ordertaking.ConstrainedType.isStringPatternUnmatchedError


class EmailAddressSpec : DescribeSpec({

    describe("EmailAddress") {
        context("문자열에 @ 가 포함된 경우,") {
            it("EmailAddress 객체가 정상적으로 생성된다.") {
                EmailAddress.create("foo@bar").shouldBeRight().shouldBeInstanceOf<EmailAddress>()
            }
        }

        context("문자열에 @ 가 없는 경우,") {
            it("EmailAddress 객체 생성에 실패한다.") {
                EmailAddress.create("foobar").shouldBeLeft()
                    .isStringPatternUnmatchedError().shouldBeTrue()
            }
        }

        context("문자열이 비어있는 경우,") {
            it("Value 객체 생성에 실패한다.") {
                EmailAddress.create("").shouldBeLeft()
                    .isEmptyStringError().shouldBeTrue()
            }
        }
    }
})
