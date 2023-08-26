package co.kaush.msusf.movies

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

data class ActionHolder(val action: suspend () -> Unit)

class EventProcessor(val scope: CoroutineScope) {
    private val queue = ConcurrentLinkedQueue<ActionHolder>()
    private val isRunning = AtomicBoolean(false)


    fun addToQueue(action: suspend () -> Unit) {
        queue.add(ActionHolder(action = action))
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
                        actionHolder.action()
                        delay(1)
                        println("${System.currentTimeMillis()} processedInput")
                    }
                }
                isRunning.getAndSet(false)
            }
        }
    }
}