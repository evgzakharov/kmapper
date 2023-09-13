package com.github.evgzakharov.kmapper

@MapperConverter
interface StringToIntConverter: Converter<String, Int> {
    override fun convert(source: String): Int = source.toInt()
}

@MapperConverter
interface IntToStringConverter: Converter<Int, String> {
    override fun convert(source: Int): String = source.toString()
}