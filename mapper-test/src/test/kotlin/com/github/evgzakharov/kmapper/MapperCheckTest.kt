package com.github.evgzakharov.kmapper

import org.junit.jupiter.api.Test
import java.util.UUID

class MapperCheckTest {
    @Test
    fun `check mapper`() {
        val src = SrcDto(UUID.randomUUID(), "name", "surname", 23)
        val dst = DstDto(UUID.randomUUID(), "25", "name", "surname", dto = DstDto2("src dto2"))

        println(src.toDst())
        println(dst.setFrom(SrcSourceDto("name222", "surname333")))
    }
}