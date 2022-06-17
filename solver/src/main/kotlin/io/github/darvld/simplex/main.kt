package io.github.darvld.simplex

public fun main() {
    val x by Expression.Variable
    val y by Expression.Variable
    val z by Expression.Variable

    // Z = 7x + 8y + 10z
    val objective = objective((7 * x), (8 * y), (10 * z))

    // 2x + 3y + 2z <= 1_000
    val firstConstraint = constraint(
        (2 * x), (3 * y), (2 * z),
        relation = Expression.Relation.LessEqual,
        rightHandValue = 1_000.0,
    )

    // x + y + 2z <= 800
    val secondConstraint = constraint(
        (1 * x), (1 * y), (2 * z),
        relation = Expression.Relation.LessEqual,
        rightHandValue = 800.0,
    )

    val problem = simplexProblem(
        objective,
        firstConstraint,
        secondConstraint,
    )

    printTableau(problem)

    SimplexSolver.optimize(problem.tableau)

    println("\nSolution:\n")

    printTableau(problem)

    println("\nResult:")
    getSolution(problem).forEach { (variable, value) ->
        println("$variable = $value")
    }
}