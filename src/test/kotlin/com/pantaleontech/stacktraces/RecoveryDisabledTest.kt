package com.pantaleontech.stacktraces

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * Runs in a JVM with `-Dkotlinx.coroutines.debug=off` (see the `testDebugOff`
 * task) — the configuration your production service almost certainly has.
 *
 * This is the test that makes the point. The exception here is the *friendly*
 * one: no fields of its own, perfectly copyable. It still gets no coroutine
 * frames, because recovery is gated on debug mode and debug mode is off.
 */
class RecoveryDisabledTest {

    @Test
    fun `nothing is recovered when debug mode is off`() {
        val trace = captureStackTrace { PlainPaymentFailed("card declined") }

        assertFalse(
            COROUTINE_BOUNDARY_FRAME in trace,
            "recovery is DEBUG && stacktrace.recovery — with debug off, it never runs",
        )
        assertContains(trace, "card declined", "the exception itself is untouched")
    }
}
