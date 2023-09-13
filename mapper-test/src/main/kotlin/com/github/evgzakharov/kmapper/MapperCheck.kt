package com.github.evgzakharov.kmapper

import java.util.UUID

data class SrcDto(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val surname: String,
    val age2: Int,
    val dto: SrcDto2 = SrcDto2("test dto"),
    val array: MutableList<String> = mutableListOf("1", "2", "3"),
    val wrapper: String = "5",
    val wrapper2: SrcWrapper = SrcWrapper("5"),
    val enum: SrcEnum = SrcEnum.B
)

enum class SrcEnum {
    A, B, C
}

data class SrcWrapper(
    val value: String
)

data class SrcDto2(
    val source: String,
    val source2: String = "src"
)

data class SrcSourceDto(
    val name2: String,
    val surname: String,
    val enum: SrcEnum = SrcEnum.B,
    val default2: String = "5",
    val dto: SrcDto2 = SrcDto2("test dto"),
)

data class DstWrapper(
    val value: Int
)

data class DstDto(
    var id: UUID,
    val age2: String = "3",
    var name2: String,
    var surname: String,
    val default: String = "default",
    var default2: Int = 3,
    var dto: DstDto2,
    val array: List<Int> = listOf(3, 2, 1),
    val wrapper: DstWrapper = DstWrapper(10),
    val wrapper2: Int = 10,
    var enum: DstEnum = DstEnum.A
)

enum class DstEnum {
    A, B, C
}

data class DstDto2(
    val source: String,
    val source2: String = "dst"
)

@MapperConverter
interface Src2ToDst2Converter: Converter<SrcDto2, DstDto2>

@MapperConverter(dynamicFields = ["name2", "surname", "default"])
interface SrcToDstConverter: Converter<SrcDto, DstDto>

fun SrcDto.toDst(): DstDto = SrcToDstConverterImpl.convert(this) {
    DstDto::name2 from SrcDto::name
    DstDto::surname from { this@toDst.surname + "333555" }
    DstDto::default from { "override default" }
}

@MapperSetter(ignoreFields = ["name2"])
interface SrcToDstSetter: Setter<SrcSourceDto, DstDto>

fun DstDto.setFrom(from: SrcSourceDto): DstDto = SrcToDstSetterImpl.setFrom(from, this)