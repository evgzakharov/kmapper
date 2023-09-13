package com.github.evgzakharov.kmapper

import kotlin.reflect.KProperty1

fun interface Converter<Source : Any, Result : Any> {
    fun convert(source: Source): Result
}

abstract class MappingBuilder<Source : Any, Result : Any> : Converter<Source, Result> {
    class Configuration<Source : Any, Result : Any>(
        val dynamicFunctions: MutableMap<String, Any> = mutableMapOf()
    ) {
        infix fun <Value> KProperty1<Result, Value>.from(other: (Source) -> Value) {
            dynamicFunctions += this.name to other
        }
    }

    fun convert(source: Source, block: Configuration<Source, Result>.() -> Unit): Result {
        val configuration = Configuration<Source, Result>()
        configuration.block()
        return build(source, configuration)
    }

    protected open fun build(source: Source, configuration: Configuration<Source, Result>): Result {
        return convert(source)
    }
}