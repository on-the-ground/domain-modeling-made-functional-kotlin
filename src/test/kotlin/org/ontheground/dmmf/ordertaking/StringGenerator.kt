package org.ontheground.dmmf.ordertaking

object StringGenerator {

    fun generate(length: Int): String {
        val charset = ('a'..'z')
        return (1..length)
            .map { charset.random() }
            .joinToString("")
    }
}
