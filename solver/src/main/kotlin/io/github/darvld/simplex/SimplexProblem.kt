package io.github.darvld.simplex

import io.github.darvld.simplex.Expression.Relation
import io.github.darvld.simplex.SimplexProblem.Goal
import io.github.darvld.simplex.SimplexProblem.Goal.Maximize
import io.github.darvld.simplex.SimplexProblem.Goal.Minimize
import io.github.darvld.simplex.SimplexSolver.objectiveColumn
import io.github.darvld.simplex.SimplexSolver.objectiveRow
import io.github.darvld.simplex.SimplexSolver.rhsColumn

private const val SLACK_VARIABLE_PREFIX = "s"
private const val OBJECTIVE_COLUMN = "Z"
private const val RHS_COLUMN = "RHS"

public data class SimplexProblem(
    val variables: List<String>,
    val tableau: Matrix,
    val goal: Goal,
) {
    public enum class Goal {
        Maximize,
        Minimize,
    }
}

/**Interprets a Simplex tableau, returning a map linking each variable with the corresponding result value.*/
public fun getSolution(problem: SimplexProblem): Map<String, Double> {
    val matrix = problem.tableau

    return buildMap {
        for (column in 0 until matrix.width) {
            // Only interpret columns that represent variables
            if (column == matrix.rhsColumn)
                continue

            // For basic variables, simply retrieve the RHS value.
            // When interpreting the objective function, divide the RHS value by the objective variable's coefficient
            val value = when (column) {
                matrix.objectiveColumn -> with(matrix) { matrix[objectiveRow, rhsColumn] / matrix[objectiveRow, objectiveColumn] }
                else -> matrix.basicRow(column)?.let { row -> matrix[row, matrix.rhsColumn] } ?: 0.0
            }

            // Associate the variable and its value in the solution map
            set(problem.variables[column], value)
        }
    }
}

/**Create a [problem definition][SimplexProblem] from the given [objective] function and [constraints].*/
public fun simplexProblem(
    objective: Expression,
    vararg constraints: Expression,
    goal: Goal = Maximize,
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
    val requiredSlackCount = constraints.count { it.relation == Relation.LessEqual }
    val requiredExcessCount = constraints.count { it.relation == Relation.GreaterEqual }

    val slackVariables = List(requiredSlackCount + requiredExcessCount) {
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

        add(mapObjective(objective, variables, goal))
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
        tableau[constraint, offset + nextSlackColumn] = if (nextSlackColumn < requiredSlackCount) 1.0 else -1.0
    }

    require(isCanonical(tableau)) {
        "Non-canonical problems are not supported yet"
    }

    return SimplexProblem(variables, tableau, goal)
}

private fun isCanonical(matrix: Matrix): Boolean {
    // If we can rearrange the columns in the matrix so that they form
    // the identity matrix of order p (the number of constraints), then the tableau is in canonical form

    // Determine the order of the identity matrix (the height of the tableau excluding the objective row)
    val basicColumns = IntArray(matrix.height - 1) { -1 }

    // Exclude the objective and RHS columns
    for (column in 1 until matrix.width - 1) matrix.basicRow(column)?.let { row ->
        // Only one basic column should be present for each row
        if (basicColumns[row] != -1) return false
        basicColumns[row] = column
    }

    // If we were able to locate a basic column for each row, the tableau is in canonical form
    return basicColumns.none { it == -1 }
}

private fun Matrix.basicRow(column: Int): Int? {
    var basicRow: Int? = null

    for ((row, value) in getColumn(column).withIndex()) {
        if (value == 0.0) continue
        if (value != 1.0) return null

        if (basicRow != null) return null

        basicRow = row
    }

    return basicRow
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

private fun mapObjective(
    objective: Expression,
    variables: List<String>,
    goal: Goal,
): DoubleArray {
    return DoubleArray(variables.size) { column ->
        val value = when (variables[column]) {
            OBJECTIVE_COLUMN -> 1.0
            RHS_COLUMN -> objective.rightHandValue
            else -> objective.leftHand.find { it.label == variables[column] }?.coefficient ?: 0.0
        }

        when (goal) {
            Maximize -> value * -1
            Minimize -> value
        }
    }
}
