package io.github.darvld.simplex

public object SimplexSolver {
    private val Matrix.objectiveRow: Int
        get() = height - 1

    private val Matrix.rhsColumn: Int
        get() = width - 1

    /**Optimizes the Simplex tableau represented by the given [matrix].*/
    public fun optimize(matrix: Matrix) {
        while (!isOptimal(matrix)) {
            val pivotColumn = selectPivotColumn(matrix) ?: throw IllegalStateException()
            val pivotRow = selectPivotRow(matrix, pivotColumn) ?: throw IllegalStateException()

            performPivot(matrix, pivotRow, pivotColumn)
        }
    }

    private fun isOptimal(matrix: Matrix): Boolean {
        // Check for negative elements in the objective row (excluding the RHS value)
        return matrix.getRow(matrix.objectiveRow)
            .withIndex()
            .none { it.index != matrix.rhsColumn && it.value < 0.0 }
    }

    private fun selectPivotColumn(matrix: Matrix): Int? {
        // Get the index of the most negative element in the objective row (if such value exists)
        return matrix.getRow(matrix.objectiveRow)
            .withIndex()
            .filter { it.index != matrix.rhsColumn }
            .minByOrNull { it.value }
            ?.takeIf { it.value < 0.0 }
            ?.index
    }

    private fun selectPivotRow(matrix: Matrix, pivotColumn: Int): Int? {
        val rhsColumn = with(matrix) { getColumn(rhsColumn) }

        // Select a pivot row using the minimum ratio test
        return matrix.getColumn(pivotColumn)
            .zip(rhsColumn) { rhs, value -> rhs / value }
            .withIndex()
            .minByOrNull { it.value }
            ?.index
    }

    private fun performPivot(matrix: Matrix, pivotRow: Int, pivotColumn: Int) {
        val pivotValue = matrix[pivotRow, pivotColumn]

        // Set the pivot value to one by multiplying the entire row by its reciprocal
        matrix.getRow(pivotRow).forEachIndexed { column, value ->
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
