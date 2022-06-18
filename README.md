# Simplex Solver

A very basic Simplex implementation written in Kotlin. This project is meant to be used to demonstrate how to create a
Simplex implementation in an educational environment.

## Demo

Each release contains a binary distribution of the demo app, which can be used to showcase the library.

## Library

There are two main components in the library: the `SimplexSolver`, which performs phase II optimization, and
the `SimplexProblem`, a data structure containing the Simplex tableau and metadata.

Phase I transformations are performed by the `simplexProblem` factory method:

```kotlin
// Phase I transformations are automatically performed if necessary
val problem = simplexProblem(
    objective = parseObjective("Z = 7x + 8y + 10z")!!,
    constraints = listOf(parseConstraint(" 3x + 2y + z <= 10")!!),
    goal = Goal.Maximize
)

// Run the phase II to optimize the problem
SimplexSolver.optimize(problem.tableau)

// Get the results
val solution = getSolution(problem)
```

Expressions (constraints and objectives) can also be constructed manually:

```kotlin
// Declare the variables using delegates
val x by Variable
val y by Variable

// Variables may also be constructed manually
val z = Variable("z")

// Z = -2x - 3y - 4z
val manualObjective = objective((-2 * x), (-3 * y), (-4 * z))

// 3x + 2y + z <= 10
val manualConstraint = constraint(
    (3 * x), (2 * y), (1 * z),
    relation = Expression.Relation.LessEqual,
    rightHandValue = 10.0
)
```