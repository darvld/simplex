package io.github.darvld.simplex

import io.github.darvld.simplex.SimplexSolver.rhsColumn

private const val SLACK_VARIABLE_PREFIX = "s"
private const val OBJECTIVE_COLUMN = "Z"
private const val RHS_COLUMN = "RHS"

public data class SimplexProblem(
    val variables: List<String>,
    val tableau: Matrix,
)

/**Interprets a Simplex tableau, returning a map linking each variable with the corresponding result value.*/
public fun getSolution(problem: SimplexProblem): Map<String, Double> {
    val matrix = problem.tableau

    val basicColumns = IntArray(matrix.height) { row ->
        matrix.basicColumn(row) ?: throw IllegalStateException(
            "Problem has no basic feasible solution (row $row has no corresponding basic column)"
        )
    }

    return buildMap {
        for (row in 0 until matrix.height) {
            val column = basicColumns[row]
            val variable = problem.variables[column]

            val rhs = matrix[row, matrix.rhsColumn]
            set(variable, rhs)
        }
    }
}

/**Create a [problem definition][SimplexProblem] from the given [objective] function and [constraints].*/
public fun simplexProblem(
    objective: Expression,
    vararg constraints: Expression,
): SimplexProblem {
    // Gather all decision variables used in the problem
    val decisionVariables = listOf(objective, *constraints)
        .asSequence()
        .flatMap { it.leftHand }
        .map { it.label }
        .distinct()
        .sorted()
        .toList()

    // Generate all necessary slack variables
    val slackVariables = List(constraints.size) {
        "$SLACK_VARIABLE_PREFIX${it + 1}"
    }

    // Combine decision and slack variables with value columns
    val variables = buildList {
        add(OBJECTIVE_COLUMN)

        addAll(decisionVariables)
        addAll(slackVariables)

        add(RHS_COLUMN)
    }

    // Reorganize the constraints and objective functions to form the rows of the tableau
    val mappedRows = buildList {
        for (constraint in constraints)
            add(mapExpression(constraint, variables))

        val mappedObjective = mapExpression(objective, variables)

        for (i in 0 until mappedObjective.size) {
            if (mappedObjective[i] != 0.0) mappedObjective[i] *= -1.0
        }

        mappedObjective[0] = 1.0
        mappedObjective[mappedObjective.size - 1] = 0.0

        add(mappedObjective)
    }

    val tableau = Matrix(
        // Use a row for each constraint, and one for the objective function
        rows = constraints.size + 1,
        // Reserve a column for the RHS values, and one for the objective variable
        columns = decisionVariables.size + slackVariables.size + 2,
        // Initialize the tableau from the mapped objective and constraints
        init = { row, column -> mappedRows[row][column] },
    )

    // Set values for slack variables
    val offset = 1 + decisionVariables.size
    for ((nextSlackColumn, constraint) in (0 until constraints.size).withIndex()) {
        tableau[constraint, offset + nextSlackColumn] = 1.0
    }

    return SimplexProblem(variables, tableau)
}

private fun Matrix.basicColumn(row: Int): Int? {
    for ((index, column) in columns.withIndex()) {
        if (isBasicColumn(column, row)) return index
    }

    return null
}

private fun isBasicColumn(column: Sequence<Double>, row: Int): Boolean {
    for ((r, value) in column.withIndex()) {
        if (r == row && value == 1.0)
            continue

        if (value != 0.0)
            return false
    }

    return true
}

private fun mapExpression(
    constraint: Expression,
    variables: List<String>,
): DoubleArray {
    return DoubleArray(variables.size) { column ->
        when (variables[column]) {
            OBJECTIVE_COLUMN -> 0.0
            RHS_COLUMN -> constraint.rightHandValue
            else -> constraint.leftHand.find { it.label == variables[column] }?.coefficient ?: 0.0
        }
    }
}
