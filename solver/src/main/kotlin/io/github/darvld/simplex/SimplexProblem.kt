package io.github.darvld.simplex

import io.github.darvld.simplex.Expression.Relation
import io.github.darvld.simplex.SimplexProblem.*
import io.github.darvld.simplex.SimplexProblem.Goal.Maximize
import io.github.darvld.simplex.SimplexProblem.Goal.Minimize
import io.github.darvld.simplex.SimplexSolver.objectiveColumn
import io.github.darvld.simplex.SimplexSolver.objectiveRow
import io.github.darvld.simplex.SimplexSolver.rhsColumn

private const val SLACK_VARIABLE_PREFIX = "s"
private const val ARTIFICIAL_VARIABLE_PREFIX = "a"
private const val OBJECTIVE_COLUMN = "Z"
private const val ARTIFICIAL_OBJECTIVE_COLUMN = "W"
private const val RHS_COLUMN = "RHS"

public data class SimplexProblem(
    val columns: List<Column>,
    val tableau: Matrix,
    val goal: Goal,
) {
    public enum class Goal {
        Maximize,
        Minimize,
    }

    public data class Column(
        val label: String,
        val type: ColumnType,
    )

    public enum class ColumnType {
        Objective,
        Decision,
        Slack,
        Artificial,
        Value
    }
}

/**Interprets a Simplex tableau, returning a map linking each variable with the corresponding result value.*/
public fun getSolution(problem: SimplexProblem): Map<String, Double> {
    val matrix = problem.tableau

    return buildMap {
        for ((index, column) in problem.columns.withIndex()) {
            // Only interpret columns that represent variables
            if (column.type == ColumnType.Value)
                continue

            // For basic variables, simply retrieve the RHS value.
            // When interpreting the objective function, divide the RHS value by the objective variable's coefficient
            val value = when (index) {
                matrix.objectiveColumn -> with(matrix) { matrix[objectiveRow, rhsColumn] / matrix[objectiveRow, objectiveColumn] }
                else -> matrix.basicRow(index)?.let { row -> matrix[row, matrix.rhsColumn] } ?: 0.0
            }

            // Associate the variable and its value in the solution map
            set(column.label, value)
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
        .map { label -> Column(label, ColumnType.Decision) }
        .toList()

    // Generate all necessary slack variables
    val requiredSlackCount = constraints.count { it.relation == Relation.LessEqual }
    val requiredExcessCount = constraints.count { it.relation == Relation.GreaterEqual }

    val slackVariables = List(requiredSlackCount + requiredExcessCount) {
        Column("$SLACK_VARIABLE_PREFIX${it + 1}", ColumnType.Slack)
    }

    // Combine decision and slack variables with value columns
    val variables = buildList {
        add(Column(OBJECTIVE_COLUMN, ColumnType.Objective))

        addAll(decisionVariables)
        addAll(slackVariables)

        add(Column(RHS_COLUMN, ColumnType.Value))
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
    val offset =  decisionVariables.size + 1
    var nextSlackVariable = 0

    for ((row, constraint) in constraints.withIndex()) {
        tableau[row, offset + nextSlackVariable] = when(constraint.relation) {
            Relation.GreaterEqual -> -1.0
            Relation.LessEqual -> 1.0
            else -> continue
        }

        nextSlackVariable++
    }

    val problem = SimplexProblem(variables, tableau, goal)

    // Problem is already in canonical form, no Phase I required
    if (isCanonical(problem.tableau))
        return problem

    // The problem is not in canonical form, we need to convert it first
    val canonical = toCanonicalForm(problem)
    SimplexSolver.optimize(canonical.tableau)

    val solution = getSolution(canonical)
    check(solution[ARTIFICIAL_OBJECTIVE_COLUMN] == 0.0) { "Problem has no basic feasible solution" }

    // If the optimization was successful, we can now drop the artificial variables
    return dropArtificialVariables(canonical)
}

private fun dropArtificialVariables(problem: SimplexProblem): SimplexProblem {
    val oldMatrix = problem.tableau

    val toDrop = problem.columns.asSequence()
        .withIndex()
        .filter { it.index == 0 || it.value.type == ColumnType.Artificial }
        .toList()

    val newMatrix = Matrix(
        // Remove the first row, containing the artificial objective function
        rows = oldMatrix.height - 1,
        // Remove all columns containing artificial variables
        columns = oldMatrix.width - toDrop.size
    )

    // Migrate values from the old to the new matrix
    for (row in 0 until oldMatrix.height - 1) {
        var columnOffset = 0

        for (column in 0 until oldMatrix.width) {
            if (column == 0 || toDrop.any { it.index == column }) {
                columnOffset++
                continue
            }

            newMatrix[row, column - columnOffset] = oldMatrix[row, column]
        }
    }

    val newColumns = problem.columns.filter { column ->
        toDrop.none { it.value == column }
    }

    return problem.copy(
        columns = newColumns,
        tableau = newMatrix,
    )
}

private fun toCanonicalForm(problem: SimplexProblem): SimplexProblem {
    val matrix = problem.tableau

    // Find which columns are basic and in which row
    val basicMapping = matrix.columns
        .withIndex()
        .drop(1) // Ignore the objective column
        .take(matrix.width - 2) // Ignore the RHS column
        .mapNotNull { matrix.basicRow(it.index) }
        .toList()

    val artificialVariableRows = buildList {
        for (row in 0 until matrix.height - 1) {
            if (row in basicMapping) continue

            // Generate a new artificial variable to cover this row
            add(row)
        }
    }

    val canonicalMatrix = Matrix(
        // Allocate a new row for the artificial objective function
        rows = matrix.height + 1,
        // Add a column for each artificial variable, plus one for the artificial objective column
        columns = matrix.width + artificialVariableRows.size + 1,
    )

    // Calculate the position at which artificial variables will be inserted
    val artificialVariableOffset = canonicalMatrix.width - artificialVariableRows.size - 1

    // Fill in the artificial objective function column
    canonicalMatrix[canonicalMatrix.objectiveRow, canonicalMatrix.objectiveColumn] = 1.0

    // Add the artificial basic columns
    for (variable in 0 until artificialVariableRows.size) {
        canonicalMatrix[canonicalMatrix.objectiveRow, artificialVariableOffset + variable] = -1.0

        val basicRow = artificialVariableRows[variable]
        canonicalMatrix[basicRow, artificialVariableOffset + variable] = 1.0
    }

    // Add the original tableau
    for (row in 0 until matrix.height) {
        for (column in 0 until matrix.width - 1) {
            canonicalMatrix[row, column + 1] = matrix[row, column]
        }

        canonicalMatrix[row, canonicalMatrix.rhsColumn] = matrix[row, matrix.rhsColumn]
    }


    // Adjust the artificial objective row to the actual values
    for (row in artificialVariableRows) canonicalMatrix.walkRow(row) { column, value ->
        canonicalMatrix[canonicalMatrix.objectiveRow, column] += value
    }

    val newVariables = buildList(problem.columns.size + artificialVariableRows.size) {
        // Add the new artificial objective
        add(Column(ARTIFICIAL_OBJECTIVE_COLUMN, ColumnType.Objective))

        // Add all previous variables *except* for the RHS column
        addAll(problem.columns.filter { it.type != ColumnType.Value })

        // Add columns for the new, artificial variables
        for (variable in 1..artificialVariableRows.size) {
            add(Column("$ARTIFICIAL_VARIABLE_PREFIX$variable", ColumnType.Artificial))
        }

        // Re-add the RHS column
        add(Column(RHS_COLUMN, ColumnType.Value))
    }

    return SimplexProblem(
        columns = newVariables,
        tableau = canonicalMatrix,
        goal = problem.goal
    )
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
    variables: List<Column>,
): DoubleArray {
    return DoubleArray(variables.size) { column ->
        when (variables[column].type) {
            ColumnType.Objective -> 0.0
            ColumnType.Value -> constraint.rightHandValue
            else -> constraint.leftHand.find { it.label == variables[column].label }?.coefficient ?: 0.0
        }
    }
}

private fun mapObjective(
    objective: Expression,
    variables: List<Column>,
    goal: Goal,
): DoubleArray {
    return DoubleArray(variables.size) { column ->
        val value = when (variables[column].type) {
            ColumnType.Objective -> -1.0
            ColumnType.Value -> objective.rightHandValue
            else -> objective.leftHand.find { it.label == variables[column].label }?.coefficient ?: 0.0
        }

        when (goal) {
            Maximize -> value
            Minimize -> value * -1
        }
    }
}
