package org.ontheground.dmmf.ordertaking

import arrow.core.raise.either
import arrow.core.raise.Raise
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.builtins.serializer
import org.ontheground.dmmf.ordertaking.common.CustomerInfo
import org.ontheground.dmmf.ordertaking.common.EmailAddress
import org.ontheground.dmmf.ordertaking.common.PersonalName
import org.ontheground.dmmf.ordertaking.common.String50
import org.ontheground.dmmf.ordertaking.placeorder.dto.CustomerInfoDto
import java.util.Calendar
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

context(Raise<Throwable>)
fun fn(): String {
    val person = CustomerInfo(
        name = PersonalName(
            String50("Alex"),
            String50("Adams"),
        ),
        emailAddress = EmailAddress("bac@gmail.com"),
    )

    return CustomerInfoDto.fromDomain(person).toJSON()
}

context(Raise<Throwable>)
fun fn2(): CustomerInfo {
   val json2 = """{
       |"firstName":"",
       |"lastName":"Adams",
       |"emailAddress":"bac@gmail.com"
       |}""".trimMargin()
   return CustomerInfoDto.fromJSON(json2).toDomain()
}

@JvmInline
value class Temp(val value: LocalDate)

class DtoSpec : DescribeSpec({
    describe("EmailAddress") {
        context("문자열에 @ 가 포함된 경우,") {
            it("EmailAddress 객체가 정상적으로 생성된다.") {

                //val json = either{fn()}.shouldBeRight()
                val info = either{fn2()}

                println(info)
            }
        }
    }
})