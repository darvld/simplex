@file:Suppress("NOTHING_TO_INLINE")

package io.github.darvld.simplex

public inline fun String.toConstraintOrNull(): Expression? {
    return ExpressionParser.parseConstraint(this)
}

public inline fun String.toObjectiveOrNull(): Expression? {
    return ExpressionParser.parseObjective(this)
}

public object ExpressionParser {
    public fun parseObjective(string: String): Expression? {
        val trimmed = string.replace(" ", "")

        val equation = Regex("Z=([^<>=]+)").find(trimmed) ?: return null
        val terms = parseTerms(equation.groupValues[1])

        return Expression(
            leftHand = terms,
            relation = Expression.Relation.Equal,
            rightHandValue = 0.0
        )
    }

    public fun parseConstraint(string: String): Expression? {
        val trimmed = string.replace(" ", "")

        val equation = Regex("([^<>=]+)([><]?=)(.+)").find(trimmed) ?: return null
        val (leftHand, relation, rightHand) = equation.destructured

        val terms = parseTerms(leftHand)

        val op = when (relation) {
            "=" -> Expression.Relation.Equal
            ">=" -> Expression.Relation.GreaterEqual
            "<=" -> Expression.Relation.LessEqual
            else -> throw IllegalArgumentException("Unrecognized operator: \"$relation\"")
        }

        val rhs = rightHand.toDouble()

        return Expression(terms, op, rhs)
    }

    private fun parseTerms(string: String): List<Expression.Term> {
        return Regex("(-?+\\d*)([a-zA-Z]+)").findAll(string)
            .map {
                val (coefficient, variable) = it.destructured

                val value = when (coefficient) {
                    "-" -> -1.0
                    "+" -> 1.0
                    "" -> 1.0
                    else -> coefficient.toDouble()
                }

                Expression.Term(value, variable)
            }
            .toList()
    }
}