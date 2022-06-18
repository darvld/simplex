@file:Suppress("DuplicatedCode")

package io.github.darvld.simplex

public fun main() {
    val problem = nonCanonicalProblem()

    println("Solving problem:")
    printTableau(problem)

    SimplexSolver.optimize(problem.tableau)

    println("\nSolution:\n")

    printTableau(problem)

    println("\nResult:")
    getSolution(problem).forEach { (variable, value) ->
        println("$variable = $value")
    }
}

private fun nonCanonicalProblem(): SimplexProblem {
    val x by Variable
    val y by Variable

    // Z = 6x + 3y
    val objective = objective((6 * x), (3 * y))

    // x + y >= 1
    val firstConstraint = constraint(
        (1 * x), (1 * y),
        relation = Expression.Relation.GreaterEqual,
        rightHandValue = 1.0,
    )

    // 2x - y >= 1
    val secondConstraint = constraint(
        (2 * x), (-1 * y),
        relation = Expression.Relation.GreaterEqual,
        rightHandValue = 1.0,
    )

    // 3y <= 2
    val thirdConstraint = constraint(
        (3 * y),
        relation = Expression.Relation.LessEqual,
        rightHandValue = 2.0
    )

    return simplexProblem(
        objective,
        firstConstraint,
        secondConstraint,
        thirdConstraint,
        goal = SimplexProblem.Goal.Minimize
    )
}

private fun maximizationProblem(): SimplexProblem {
    val x by Variable
    val y by Variable
    val z by Variable

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

    return simplexProblem(
        objective,
        firstConstraint,
        secondConstraint,
    )
}

private fun minimizationProblem(): SimplexProblem {
    val x by Variable
    val y by Variable
    val z by Variable

    // Z = -2x - 3y - 4z
    val objective = objective((-2 * x), (-3 * y), (-4 * z))

    // 3x + 2y + z <= 10
    val firstConstraint = constraint(
        (3 * x), (2 * y), (1 * z),
        relation = Expression.Relation.LessEqual,
        rightHandValue = 10.0
    )

    // 2x + 5y + 3z <= 15
    val secondConstraint = constraint(
        (2 * x), (5 * y), (3 * z),
        relation = Expression.Relation.LessEqual,
        rightHandValue = 15.0
    )

    return simplexProblem(
        objective,
        firstConstraint,
        secondConstraint,
        goal = SimplexProblem.Goal.Minimize
    )
}