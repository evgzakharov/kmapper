package com.github.evgzakharov.kmapper

import kotlin.reflect.KProperty1

fun interface Converter<Source : Any, Result : Any> {
    fun convert(source: Source): Result
}

abstract class MappingBuilder<Source : Any, Result : Any> {
    class Configuration<Source : Any, Result : Any>(
        val dynamicFunctions: MutableMap<String, Any> = mutableMapOf()
    ) {
        @Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
        infix fun <Value1, Value2, Value> KProperty1<Result, Value1>.from(other: (Source) -> Value2)
            where Value : Value1, Value : Value2 {
            dynamicFunctions += this.name to other
        }

        infix fun <Value> KProperty1<Result, Value>.fromUnchecked(other: (Source) -> Value) {
            dynamicFunctions += this.name to other
        }
    }

    open fun createConverter(block: Configuration<Source, Result>.() -> Unit): Converter<Source, Result> {
        val configuration = Configuration<Source, Result>()
        configuration.block()
        return build(configuration)
    }

    protected abstract fun build(configuration: Configuration<Source, Result>): Converter<Source, Result>
}