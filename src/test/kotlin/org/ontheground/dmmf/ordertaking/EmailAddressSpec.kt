package org.ontheground.dmmf.ordertaking

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.string.contain
import io.kotest.matchers.string.endWith
import io.kotest.matchers.string.startWith
import io.kotest.matchers.types.shouldBeInstanceOf


class EmailAddressSpec : DescribeSpec({

    describe("EmailAddress") {
        context("문자열에 @ 가 포함된 경우,") {
            val emailAddress = EmailAddress("foo@bar")
            it("EmailAddress 객체가 정상적으로 생성된다.") {
                emailAddress.shouldBeInstanceOf<EmailAddress>()
            }
        }

        context("문자열에 @ 가 없는 경우,") {
            val exception = shouldThrow<IllegalArgumentException> {
                EmailAddress("foobar")
            }

            it("EmailAddress 객체 생성에 실패한다.") {
                exception.message should endWith("must match the pattern '.+@.+'")
            }
        }

        context("문자열이 비어있는 경우,") {
            val exception = shouldThrow<IllegalArgumentException> {
                EmailAddress("")
            }

            it("Value 객체 생성에 실패한다.") {
                exception.message should startWith("Must not be null or empty")
            }
        }
    }
})
