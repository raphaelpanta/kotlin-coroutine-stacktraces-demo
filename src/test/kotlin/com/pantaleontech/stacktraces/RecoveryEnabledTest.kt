package com.pantaleontech.stacktraces

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Runs in a JVM with `-Dkotlinx.coroutines.debug=on` (see the `test` task).
 */
class RecoveryEnabledTest {

    @Test
    fun `plain exception is recovered`() {
        val trace = captureStackTrace { PlainPaymentFailed("card declined") }

        assertContains(
            trace,
            COROUTINE_BOUNDARY_FRAME,
            "expected the coroutine boundary frame in a recovered trace",
        )
        assertContains(trace, "Caused by", "the untouched original is attached as the cause")
    }

    @Test
    fun `exception with its own field is silently not recovered`() {
        val trace = captureStackTrace { CodedPaymentFailed("CARD_DECLINED", "card declined") }

        assertFalse(
            COROUTINE_BOUNDARY_FRAME in trace,
            "an exception with class-specific fields cannot be copied, so no frames are added",
        )
    }

    @Test
    fun `StackTraceRecoverable opts the same exception back in`() {
        val trace = captureStackTrace {
            RecoverablePaymentFailed("CARD_DECLINED", "card declined")
        }

        assertContains(
            trace,
            COROUTINE_BOUNDARY_FRAME,
            "copyForStackTraceRecovery should let recovery run again",
        )
        assertContains(trace, "card declined", "the message must survive the copy")
    }

    @Test
    fun `recovery adds frames rather than replacing them`() {
        val recovered = captureStackTrace { PlainPaymentFailed("card declined") }
        val notRecovered = captureStackTrace {
            CodedPaymentFailed("CARD_DECLINED", "card declined")
        }

        assertTrue(
            recovered.lines().size > notRecovered.lines().size,
            "the recovered trace should be strictly longer",
        )
    }
}
