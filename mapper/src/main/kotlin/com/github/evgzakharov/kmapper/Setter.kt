package com.github.evgzakharov.kmapper

interface Setter<Source : Any, Result : Any>

abstract class SetterBuilder<Source : Any, Result : Any> : Setter<Source, Result> {
    fun setFrom(source: Source, target: Result): Result {
        return build(source, target)
    }

    protected abstract fun build(source: Source, target: Result): Result
}