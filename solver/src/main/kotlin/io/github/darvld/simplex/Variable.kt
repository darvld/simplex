package io.github.darvld.simplex

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@JvmInline
public value class Variable(public val label: String) {
    public companion object : ReadOnlyProperty<Any?, Variable> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Variable {
            return Variable(property.name)
        }
    }
}