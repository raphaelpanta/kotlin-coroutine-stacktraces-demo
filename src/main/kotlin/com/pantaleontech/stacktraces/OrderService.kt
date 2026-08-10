package com.pantaleontech.stacktraces

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * The marker frame kotlinx.coroutines inserts into a recovered stack trace.
 * A full line reads:
 *
 *     at _COROUTINE._BOUNDARY._(CoroutineDebugging.kt)
 *
 * Frames above it belong to the coroutine that failed; frames below it belong
 * to the coroutine that received the exception. The untouched original is
 * attached as the cause.
 *
 * Do not assert on the line number — it moves between library versions.
 */
const val COROUTINE_BOUNDARY_FRAME: String = "_COROUTINE._BOUNDARY._"

/**
 * A deliberately boring two-hop call chain. The suspension point between
 * [placeOrder] and [chargeCard] is the whole point: without recovery, the
 * frames on the far side of it are not in the trace you get.
 */
class OrderService {

    suspend fun placeOrder(newFailure: () -> Throwable) {
        withContext(Dispatchers.Default) {
            chargeCard(newFailure)
        }
    }

    private suspend fun chargeCard(newFailure: () -> Throwable) {
        delay(1)
        throw newFailure()
    }
}

/**
 * Runs [OrderService.placeOrder], catches the failure and returns its printed
 * stack trace.
 *
 * Note the explicit rethrow of [CancellationException]. Nothing here cancels,
 * but a bare `catch (e: Throwable)` around a suspend call is how structured
 * concurrency quietly breaks, and this file is not the place to model that.
 */
fun captureStackTrace(newFailure: () -> Throwable): String = runBlocking {
    var caught: Throwable? = null
    try {
        OrderService().placeOrder(newFailure)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        caught = e
    }
    checkNotNull(caught) { "expected placeOrder to fail" }.stackTraceToString()
}

/**
 * Whether coroutine debug mode is on in THIS JVM.
 *
 * kotlinx.coroutines reads `kotlinx.coroutines.debug` once, at class-load, and
 * resolves it as: "on"/"" -> true, "off" -> false, "auto"/absent -> follows
 * whether assertions are enabled. Stack trace recovery is gated on the result:
 * internally it is `DEBUG && kotlinx.coroutines.stacktrace.recovery`, where the
 * second property defaults to true. Turning recovery on without debug mode
 * does nothing.
 */
fun debugModeSummary(): String {
    val property = System.getProperty("kotlinx.coroutines.debug")
    val assertions = OrderService::class.java.desiredAssertionStatus()
    val effective = when (property) {
        "on", "" -> true
        "off" -> false
        else -> assertions // "auto", or absent
    }
    return buildString {
        append("kotlinx.coroutines.debug=").append(property ?: "<absent, so auto>")
        append(" | assertions(-ea)=").append(assertions)
        append(" | debug mode effectively ").append(if (effective) "ON" else "OFF")
        append(" | stack trace recovery ").append(if (effective) "ACTIVE" else "DISABLED")
    }
}
