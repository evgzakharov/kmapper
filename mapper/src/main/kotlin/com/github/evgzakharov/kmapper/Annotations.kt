package com.github.evgzakharov.kmapper

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MapperConverter(
    val dynamicFields: Array<String> = emptyArray()
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class MapperSetter(
    val ignoreFields: Array<String> = emptyArray()
)