package io.github.darvld.simplex

public fun main() {
    // Z = 7x + 8y + 10z
    val objective = Expression(
        leftHand = listOf(
            Expression.Term(7.0, "x"),
            Expression.Term(8.0, "y"),
            Expression.Term(10.0, "z"),
        ),
        relation = Expression.Relation.Equal,
        rightHandValue = 1.0,
    )

    // 2x + 3y + 2z <= 1_000
    val firstConstraint = Expression(
        leftHand = listOf(
            Expression.Term(2.0, "x"),
            Expression.Term(3.0, "y"),
            Expression.Term(2.0, "z"),
        ),
        relation = Expression.Relation.LessEqual,
        rightHandValue = 1_000.0,
    )

    // x + y + 2z <= 800
    val secondConstraint = Expression(
        leftHand = listOf(
            Expression.Term(1.0, "x"),
            Expression.Term(1.0, "y"),
            Expression.Term(2.0, "z"),
        ),
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