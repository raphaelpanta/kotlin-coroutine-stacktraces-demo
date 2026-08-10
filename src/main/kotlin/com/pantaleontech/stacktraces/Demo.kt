package com.pantaleontech.stacktraces

/**
 * Prints the same failure four ways.
 *
 *   ./gradlew run            -> debug mode OFF (what production looks like)
 *   ./gradlew runWithDebug   -> debug mode ON  (what your tests look like)
 *
 * Run both and diff the output. That difference is the entire article.
 */
fun main() {
    println("=".repeat(78))
    println(debugModeSummary())
    println("=".repeat(78))

    section(
        "A. Plain exception, no fields of its own",
        "Recovery can copy this reflectively, so the coroutine frames survive.",
    ) { PlainPaymentFailed("card declined") }

    section(
        "B. Same failure, one extra field (errorCode)",
        "Recovery refuses to copy an exception with its own fields, and says nothing.",
    ) { CodedPaymentFailed("CARD_DECLINED", "card declined") }

    section(
        "C. Same field, implementing StackTraceRecoverable",
        "You define the copy, so the frames come back without dropping the field.",
    ) { RecoverablePaymentFailed("CARD_DECLINED", "card declined") }

    section(
        "D. A failure with no coroutine boundary crossed",
        "The control: a plain synchronous throw is unaffected by any of this.",
        boundaryExpected = false,
    ) { PlainPaymentFailed("card declined") }
}

private fun section(
    title: String,
    note: String,
    boundaryExpected: Boolean = true,
    newFailure: () -> Throwable,
) {
    val trace = if (boundaryExpected) {
        captureStackTrace(newFailure)
    } else {
        // No coroutines involved, so a plain try/catch is all this needs.
        try {
            throw newFailure()
        } catch (e: Throwable) {
            e.stackTraceToString()
        }
    }
    val recovered = COROUTINE_BOUNDARY_FRAME in trace

    println()
    println("--- $title ".padEnd(78, '-'))
    println(note)
    println("coroutine boundary frame present: $recovered")
    println()
    println(trace.lines().joinToString("\n") { "    $it" })
}
