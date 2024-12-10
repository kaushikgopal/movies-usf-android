package co.kaush.usf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Primary rule for Coroutine Tests.
 *
 * @constructor Create empty Test coroutine rule
 *
 * Use it as follows
 *
 * ```kotlin
 * class MyViewModelTest {
 *    @RegisterExtension val testRule = CoroutineTestRule()
 * }
 * ```
 */
@ExperimentalCoroutinesApi
class CoroutineTestRule(
    private val scheduler: TestCoroutineScheduler? =
        null, // if you want multiple types of dispatchers
    private val dispatcher: TestDispatcher? = null,
) : BeforeEachCallback,
    AfterEachCallback {

  val testDispatcher by lazy {
    when {
      dispatcher != null -> dispatcher
      scheduler != null -> StandardTestDispatcher(scheduler)
      else -> StandardTestDispatcher()
    }
  }

  override fun beforeEach(p0: ExtensionContext?) {
    // ⚠️ Calling this with a TestDispatcher has special behavior:
    // subsequently-called runTest, as well as TestScope and test dispatcher constructors,
    // will use the TestCoroutineScheduler of the provided dispatcher.

    // This means in runTest you don't have to
    Dispatchers.setMain(testDispatcher)
  }

  override fun afterEach(p0: ExtensionContext?) {
    Dispatchers.resetMain()
  }

  fun currentTestTime(): Long {
    return testDispatcher.scheduler.currentTime
  }
}