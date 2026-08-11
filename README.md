# kotlin-coroutine-stacktraces-demo

Companion code for **"Coroutine stack traces in production: why yours stop at the dispatcher"**
— article link goes here once published.

The same payment failure, printed four ways, so you can see exactly what coroutine stack trace
recovery does, when it silently declines to do it, and what that costs you during an incident.

## Run it

```bash
./gradlew run           # coroutine debug mode OFF — what production looks like
./gradlew runWithDebug  # coroutine debug mode ON  — what your tests look like
```

Run both and diff the output. That difference is the whole article.

```bash
./gradlew check         # runs both test tasks (debug on, and debug off)
```

Requires JDK 21. Kotlin **2.4.20-Beta2**, because `StackTraceRecoverable` is not in a stable
Kotlin release yet — the build pulls it from the JetBrains EAP repository. When 2.4.20 goes GA
(planned September 2026), drop the EAP repository lines from `build.gradle.kts` and bump the
version.

## What the four cases show

| | Exception | Recovery runs? | Why |
|---|---|---|---|
| A | `PlainPaymentFailed` | yes | No fields of its own, so it can be copied reflectively |
| B | `CodedPaymentFailed` | **no** | One `errorCode` field is enough to disqualify it, silently |
| C | `RecoverablePaymentFailed` | yes | Implements `StackTraceRecoverable` and defines its own copy |
| D | control | n/a | No coroutine boundary crossed, nothing to recover |

...and all four print the same short trace under `./gradlew run`, because recovery is gated on
debug mode.

## The two things worth taking away

**1. Recovery is off in production by default.**

kotlinx.coroutines computes it as `DEBUG && kotlinx.coroutines.stacktrace.recovery`. The second
property defaults to `true`, so `DEBUG` is the switch that matters. `DEBUG` comes from the
`kotlinx.coroutines.debug` system property: `on` (or empty) means on, `off` means off, and
`auto` — the default when the property is absent — follows whether assertions are enabled.

Gradle enables assertions on test tasks by default. Your production JVM almost certainly does not
run with `-ea`. So the traces you develop against are richer than the traces you get paged for.

**2. Recovery works by copying your exception, and it gives up quietly.**

In order: a recovery hook if the class has one; otherwise, if the class declares fields not
inherited from `Throwable`, no copy; otherwise a public constructor is invoked reflectively;
and if the copy would change the message, no copy. When no copy is possible the original is
rethrown untouched, with no log line and no warning.

`StackTraceRecoverable` (Kotlin 2.4.20+) is the supported way back in. It lives in the standard
library rather than in kotlinx.coroutines specifically so that domain exceptions can opt in
without your domain module depending on a coroutines library — the standing objection to the
older `kotlinx.coroutines.CopyableThrowable`. Returning `null` from `copyForStackTraceRecovery()`
is a deliberate opt-out.

## Caveats, stated plainly

- The import is `kotlin.coroutines.debug.StackTraceRecoverable` — the `debug` subpackage. The
  sample on Kotlin's What's New page shows `kotlin.coroutines.StackTraceRecoverable`, which does
  not resolve.
- `StackTraceRecoverable` is **experimental** and requires `@OptIn`. The build opts in globally via
  `kotlin.coroutines.ExperimentalStdlibCoroutineSupportApi` — note the marker annotation is in
  `kotlin.coroutines` even though the interface is in `kotlin.coroutines.debug`.
- It needs a kotlinx.coroutines version that knows to look for `copyForStackTraceRecovery`. This
  project pins `1.11.0`, which works. If case C behaves like case B, that lookup is the first thing
  to check.
- Recovery costs an exception copy and a `setStackTrace` per rethrow across a coroutine boundary.
  The kotlinx.coroutines docs call debug-mode overhead negligible; `DebugProbes` is a separate,
  larger decision — single-digit percent of throughput with creation stack traces disabled, which
  has been the default since 1.8.0.
- Under Android R8 release builds, debug mode and recovery are permanently off. This demo is
  JVM-only anyway; recovery is a JVM feature.

## Sources

- [Kotlin 2.4.20-Beta2 — coroutine stack trace recovery](https://kotlinlang.org/docs/whatsnew-eap.html)
- [KEEP-0461 — StackTraceRecoverable](https://github.com/Kotlin/KEEP/blob/main/proposals/stdlib/KEEP-0461-stacktrace-recoverable.md)
- [kotlinx.coroutines — debugging guide](https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/topics/debugging.md)
- [kotlinx.coroutines — Debug.kt (the DEBUG and recovery properties)](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/src/Debug.kt)
- [kotlinx-coroutines-debug — using in production](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-debug/README.md)

---

I post daily about JVM backend engineering on LinkedIn ·
[pantaleontech.com](https://pantaleontech.com)
