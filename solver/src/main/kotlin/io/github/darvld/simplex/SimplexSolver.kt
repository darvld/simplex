@file:Suppress("unused")

package io.github.darvld.simplex

public object SimplexSolver {
    public val Matrix.objectiveRow: Int
        get() = height - 1

    public val Matrix.rhsColumn: Int
        get() = width - 1

    public val Matrix.objectiveColumn: Int
        get() = 0

    /**Optimizes the Simplex tableau represented by the given [matrix].*/
    public fun optimize(matrix: Matrix) {
        while (!isOptimal(matrix)) {
            val pivotColumn = selectPivotColumn(matrix)
                ?: error("Internal error: unable to select pivot column.")

            val pivotRow = selectPivotRow(matrix, pivotColumn)
                ?: throw IllegalStateException("Unable to select a pivot row: the problem is unbounded.")

            performPivot(matrix, pivotRow, pivotColumn)
        }
    }

    private fun isOptimal(matrix: Matrix): Boolean {
        // Check for positive elements in the objective row (excluding the RHS value and the objective column)
        return matrix.getRow(matrix.objectiveRow)
            .withIndex()
            .none { it.index != matrix.rhsColumn && it.index != matrix.objectiveColumn && it.value > 0.0 }
    }

    private fun selectPivotColumn(matrix: Matrix): Int? {
        // Get the index of the largest positive element in the objective row (if such value exists)
        return matrix.getRow(matrix.objectiveRow)
            .withIndex()
            .filter { it.index != matrix.rhsColumn && it.index != matrix.objectiveColumn && it.value > 0.0 }
            .maxByOrNull { it.value }
            ?.index
    }

    private fun selectPivotRow(matrix: Matrix, pivotColumn: Int): Int? {
        val rhsColumn = with(matrix) { getColumn(rhsColumn) }.toList()

        // Select a pivot row using the minimum ratio test
        return matrix.getColumn(pivotColumn)
            .withIndex()
            .filter { rhsColumn[it.index] != 0.0 }
            .filter { it.index != matrix.objectiveRow }
            .filter { it.value > 0.0 }
            .map { IndexedValue(it.index, rhsColumn[it.index] / it.value) }
            .minByOrNull { it.value }
            ?.index
    }

    private fun performPivot(matrix: Matrix, pivotRow: Int, pivotColumn: Int) {
        val pivotValue = matrix[pivotRow, pivotColumn]

        // Set the pivot value to one by multiplying the entire row by its reciprocal
        matrix.walkRow(pivotRow) { column, value ->
            matrix[pivotRow, column] = value / pivotValue
        }

        // Set the other values in the pivot column to zero
        matrix.walkColumn(pivotColumn) { row, factor ->
            // Skip the pivot row
            if (row == pivotRow) return@walkColumn

            // Subtract the corresponding value in the pivot row multiplied by the element in the pivot column
            // This will set the value in the pivot column to zero
            matrix.walkRow(row) { column, value ->
                matrix[row, column] = value - matrix[pivotRow, column] * (factor)
            }
        }
    }
}
