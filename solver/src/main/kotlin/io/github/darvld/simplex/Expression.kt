package io.github.darvld.simplex

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
}

public operator fun Number.times(variable: Variable): Expression.Term {
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
    // ax1 + bx2 + ... + ixn + c - Z = 0
    return Expression(
        leftHand = leftHand.toList(),
        rightHandValue = constantValue,
        relation = Expression.Relation.Equal
    )
}