@file:Suppress("DuplicatedCode")

package io.github.darvld.simplex

import kotlin.system.exitProcess

public fun main() {
    println("==Simplex Solver v0.1==")

    val objective = readObjective()
    val constraints = buildList {
        while (true) {
            add(readConstraint())

            print("Do you wish to add more constraints? (y/[n]): ")
            if(readln().lowercase() != "y") break
        }
    }

    val goal = selectGoal()

    val problem = simplexProblem(
        objective,
        constraints,
        goal,
    )

    println("\nInitial tableau:\n")
    printTableau(problem)

    SimplexSolver.optimize(problem.tableau)

    println("\nFinal tableau:\n")
    printTableau(problem)

    println("\nSolution:\n")
    getSolution(problem).forEach { (variable, value) ->
        println("$variable: $value")
    }
}

private fun selectGoal(): SimplexProblem.Goal {
    print("Enter 'max' or 'min' for maximization or minimization: ")

    return when(readln().lowercase()) {
        "max" -> SimplexProblem.Goal.Maximize
        "min" -> SimplexProblem.Goal.Minimize
        else -> {
            println("Invalid goal selected")
            exitProcess(1)
        }
    }
}

private fun readObjective(): Expression {
    while (true) {
        print("Input the objective function in the form \"Z = ax + by\": ")

        val expression = readln().toObjectiveOrNull()
        if (expression == null || expression.relation != Expression.Relation.Equal) {
            println("Invalid expression, please try again.")
            continue
        }

        return expression
    }
}

private fun readConstraint(): Expression {
    while (true) {
        print("Input a constraint in the form \"ax + by >= c\" ('<=', and '=' may be used): ")

        val expression = readln().toConstraintOrNull()
        if (expression == null) {
            println("Invalid expression, please try again.")
            continue
        }

        return expression
    }
}
