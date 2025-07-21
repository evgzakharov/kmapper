package com.github.evgzakharov.kmapper

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

class MapperCheckTest {
    @Test
    fun `check mapper`() {
        val src = SrcDto(UUID.randomUUID(), "name", "surname", 23)
        val dst = DstDto(UUID.randomUUID(), "25", "name", "surname", dto = DstDto2("src dto2"))

        val srcToDst = src.toDst()
        println("srcToDst: $srcToDst")

        Assertions.assertEquals(src.id, srcToDst.id)
        Assertions.assertEquals(src.name, srcToDst.name2)
        Assertions.assertEquals(src.surname + "333555", srcToDst.surname)
        Assertions.assertEquals(src.age2.toString(), srcToDst.age2)

        val dstAfterSet = dst.setFrom(SrcSourceDto("name222", "surname333"))
        println("dstAfterSet: $dstAfterSet")

        Assertions.assertEquals("name", dstAfterSet.name2)
        Assertions.assertEquals("surname333", dstAfterSet.surname)
    }
}