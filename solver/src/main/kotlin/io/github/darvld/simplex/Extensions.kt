@file:Suppress("NOTHING_TO_INLINE")

package io.github.darvld.simplex

import io.github.darvld.simplex.Expression.Term

private const val TABLEAU_COLUMN_PADDING = 4

private fun formatValue(value: Double): String {
    return String.format("%.2f", value)
}

public inline fun String.toExpressionOrNull(): Expression? {
    return parseExpression(this)
}

public fun parseExpression(string: String): Expression? {
    val trimmed = string.replace(" ", "")

    val equation = Regex("([^<>=]+)([><]?=)(.+)").find(trimmed) ?: return null
    val (leftHand, relation, rightHand) = equation.destructured

    val terms = Regex("(-?+\\d*)([a-zA-Z]+)").findAll(leftHand)
        .map {
            val (coefficient, variable) = it.destructured

            val value = when(coefficient) {
                "-" -> -1.0
                "+" -> 1.0
                else -> coefficient.toDouble()
            }

            Term(value, variable)
        }
        .toList()

    val op = when(relation) {
        "=" -> Expression.Relation.Equal
        ">=" -> Expression.Relation.GreaterEqual
        "<=" -> Expression.Relation.LessEqual
        else -> throw IllegalArgumentException("Unrecognized operator: \"$relation\"")
    }

    val rhs = rightHand.toDouble()

    return Expression(terms, op, rhs)
}

public fun printTableau(problem: SimplexProblem) {
    printMatrix(problem.tableau) { widths ->
        val header = StringBuilder()
        for ((index, column) in problem.columns.withIndex()) {
            header.append(column.label)

            val padding = (widths[index] - column.label.length).coerceAtLeast(0)
            if (padding > 0) header.append(" ".repeat(padding))
        }

        println(header)
        println("-".repeat(header.length))
    }
}

public fun printMatrix(
    matrix: Matrix,
    header: ((widths: IntArray) -> Unit)? = null,
) {
    val widths = IntArray(matrix.width) { column ->
        matrix.getColumn(column).maxOf {
            // Add a space before for alignment in case a value is negative
            formatValue(it).length + TABLEAU_COLUMN_PADDING
        }
    }

    header?.invoke(widths)

    val totalWidth = widths.sum()
    for ((rowIndex, row) in matrix.rows.withIndex()) {
        val line = StringBuilder()

        for ((column, width) in widths.withIndex()) {
            if (column == matrix.width - 1) line.append("| ")

            val string = formatValue(row[column])
            line.append(string)

            val padding = (width - string.length).coerceAtLeast(0)

            if (padding > 0)
                line.append(" ".repeat(padding))
        }

        if (rowIndex == matrix.height - 1) println("-".repeat(totalWidth))
        println(line)
    }
}