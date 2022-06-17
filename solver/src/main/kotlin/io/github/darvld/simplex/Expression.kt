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