@file:Suppress("NOTHING_TO_INLINE")

package io.github.darvld.simplex

private const val TABLEAU_COLUMN_PADDING = 4

private fun formatValue(value: Double): String {
    return String.format("%.2f", value)
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