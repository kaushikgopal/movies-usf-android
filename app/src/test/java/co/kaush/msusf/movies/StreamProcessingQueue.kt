package co.kaush.msusf.movies

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class StreamActionHolder(val action: suspend () -> Unit)

class StreamProcessingQueue(private val scope: CoroutineScope) {
  private val queue = ConcurrentLinkedQueue<StreamActionHolder>()
  private val isRunning = AtomicBoolean(false)

  fun addToQueue(action: suspend () -> Unit) {
    queue.add(StreamActionHolder(action = action))
    run()
  }

  private fun run() {
    if (queue.size > 0 && !isRunning.get()) {
      println("launch viewModel.viewModelScope.launch")
      scope.launch {
        isRunning.getAndSet(true)
        while (queue.size > 0) {
          runBlocking {
            println("${System.currentTimeMillis()} processInput")
            val actionHolder = queue.remove()
            delay(2)
            actionHolder.action()
            delay(2)
            println("${System.currentTimeMillis()} processedInput")
          }
        }
        isRunning.getAndSet(false)
      }
    }
  }
}
