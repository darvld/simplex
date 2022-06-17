@file:Suppress("NOTHING_TO_INLINE")

package io.github.darvld.simplex

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**Creates a new [Matrix] with the given number of [rows] and [columns], populating the matrix with the results of
 * the [init] function.*/
public inline fun Matrix(
    rows: Int,
    columns: Int,
    init: (row: Int, column: Int) -> Double = { _, _ -> 0.0 },
): Matrix {
    val data = Array(rows) { row ->
        DoubleArray(columns) { column ->
            init(row, column)
        }
    }

    return Matrix(data)
}

/**A simple, no-allocation wrapper around a two-dimensional [Double] array.*/
@JvmInline
public value class Matrix(public val data: Array<DoubleArray>) {
    /**The number of columns in this matrix.*/
    public inline val width: Int
        get() = data[0].size

    /**The number of rows in this matrix.*/
    public inline val height: Int
        get() = data.size

    /**Returns an iterable [Sequence] containing all rows in the matrix.*/
    public inline val rows: Sequence<DoubleArray>
        get() = data.asSequence()

    /**Returns an iterable [Sequence] containing all columns in the matrix.*/
    public inline val columns: Sequence<Sequence<Double>>
        get() = sequence {
            for (column in 0 until width) yield(getColumn(column))
        }

    /**Returns the value at the given [row] and [column].
     *
     * @throws IndexOutOfBoundsException if the [row] or the [column] are out of the bounds of the matrix.*/
    public inline operator fun get(row: Int, column: Int): Double {
        return data[row][column]
    }

    /**Sets the value at the given [row] and [column].
     *
     * @throws IndexOutOfBoundsException if the [row] or the [column] are out of the bounds of the matrix.*/
    public inline operator fun set(row: Int, column: Int, value: Double) {
        data[row][column] = value
    }

    /**Returns a sequence yielding every value in the given [column].
     *
     * @throws IndexOutOfBoundsException if the [column] is out of the bounds of the matrix.*/
    public inline fun getColumn(column: Int): Sequence<Double> = sequence {
        for (row in 0 until height) yield(this@Matrix[row, column])
    }

    /**Returns a given [row] from this matrix.
     *
     * @throws IndexOutOfBoundsException if the [row] is out of the bounds of the matrix.*/
    public inline fun getRow(row: Int): Sequence<Double> {
        return data[row].asSequence()
    }
}

/**Calls the given [block] on each value of the given [row].*/
@OptIn(ExperimentalContracts::class)
public inline fun Matrix.walkRow(row: Int, block: (column: Int, value: Double) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }

    for (column in 0 until width) {
        block(column, this[row, column])
    }
}

/**Calls the given [block] on each value of the given [column].*/
@OptIn(ExperimentalContracts::class)
public inline fun Matrix.walkColumn(column: Int, block: (row: Int, value: Double) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }

    for (row in 0 until height) {
        block(row, this[row, column])
    }
}
