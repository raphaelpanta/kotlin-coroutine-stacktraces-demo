package com.pantaleontech.stacktraces

import kotlin.coroutines.debug.StackTraceRecoverable

/**
 * Three shapes of the same domain failure. The only thing that changes between
 * them is whether kotlinx.coroutines is able to copy the exception — and that
 * is what decides whether you get your coroutine frames back.
 *
 * The copy rules (docs/topics/debugging.md in kotlinx.coroutines), in order:
 *
 *  1. If the class implements a recovery hook, use it. Returning null opts out.
 *  2. If the class has fields of its own, not inherited from Throwable,
 *     it is NOT copied.
 *  3. Otherwise a public constructor is invoked reflectively.
 *  4. If the reflective copy would change the message, it is NOT copied.
 */

/**
 * Case A — no fields of its own, message passed through untouched.
 * Recovery copies this one reflectively. You get the coroutine frames.
 */
class PlainPaymentFailed(message: String) : IllegalStateException(message)

/**
 * Case B — the version most services end up with. One extra field is all it
 * takes: rule 2 applies, recovery gives up, and it does so silently. No log
 * line, no warning. The trace is just shorter from now on.
 */
class CodedPaymentFailed(
    val errorCode: String,
    message: String,
) : IllegalStateException(message)

/**
 * Case C — same field, opted back in.
 *
 * [StackTraceRecoverable] arrived in the Kotlin 2.4.20 standard library. The
 * point of putting it in the stdlib rather than in kotlinx.coroutines is that
 * your domain exceptions no longer need a coroutines dependency to participate
 * — which was the standing objection to kotlinx.coroutines' CopyableThrowable.
 *
 * Returning null from [copyForStackTraceRecovery] is a deliberate opt-out.
 */
class RecoverablePaymentFailed private constructor(
    val errorCode: String,
    private val detail: String,
    cause: Throwable?,
) : IllegalStateException(detail, cause),
    StackTraceRecoverable<RecoverablePaymentFailed> {

    constructor(errorCode: String, detail: String) : this(errorCode, detail, null)

    override fun copyForStackTraceRecovery(): RecoverablePaymentFailed =
        RecoverablePaymentFailed(errorCode, detail, this)
}
