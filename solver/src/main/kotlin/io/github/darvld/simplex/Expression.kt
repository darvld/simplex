package io.github.darvld.simplex

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

public data class Expression(
    val leftHand: List<Term>,
    val relation: Relation,
    val rightHandValue: Double,
) {
    public enum class Relation {
        GreaterEqual,
        LessEqual,
        Equal,
    }

    public data class Term(
        val coefficient: Double,
        val label: String,
    )

    @JvmInline
    public value class Variable(public val label: String) {
        public companion object : ReadOnlyProperty<Any?, Variable> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): Variable {
                return Variable(property.name)
            }
        }
    }
}

public operator fun Number.times(variable: Expression.Variable): Expression.Term {
    return Expression.Term(toDouble(), variable.label)
}

public fun constraint(
    vararg leftHand: Expression.Term,
    relation: Expression.Relation,
    rightHandValue: Double,
): Expression {
    // ax1 + bx2 + ... + ixn = RHS
    return Expression(leftHand.toList(), relation, rightHandValue)
}

public fun objective(
    vararg leftHand: Expression.Term,
    constantValue: Double = 0.0,
): Expression {
    // original -> ax1 + bx2 + ... + ixn + c = Z
    // mapped -> -Z - ax1 - bx2 - ... - ixn = -c
    return Expression(
        leftHand = leftHand.map { it.copy(coefficient = it.coefficient * -1.0) },
        rightHandValue = constantValue * -1.0,
        relation = Expression.Relation.Equal
    )
}