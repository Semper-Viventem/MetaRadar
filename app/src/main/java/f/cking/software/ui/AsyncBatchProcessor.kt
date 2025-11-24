package f.cking.software.ui

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * The helper class splits large rendering operations into small subtasks in order not to freeze the main thread.
 *
 * @param frameRate frames per second
 * @param renderLoad percentage of one frame rendering time that may be busy bu batch processor's task
 * @param provideIsCancelled provide [true] if task should be canceled
 * @param processItem function will be invoked for each item
 * @param onBatchCompleted function will be invoked after each batch
 */
class AsyncBatchProcessor<Item, Payload>(
    frameRate: Float,
    renderLoad: Float = DEFAULT_RENDER_LOAD,
    private val processItem: (item: Item, payload: Payload) -> Unit,
    private val provideIsCancelled: () -> Boolean = { true },
    private val onBatchCompleted: (batchId: Int, payload: Payload) -> Unit = { _, _ -> },
    private val onStart: (payload: Payload) -> Unit = {},
    private val onComplete: (payload: Payload) -> Unit = {},
    private val onCancelled: (payload: Payload?) -> Unit = {},
) {

    private val batchTimeOut =  (1000 / frameRate * renderLoad).toLong()
    private var payload: Payload? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isActive: Boolean = false

    fun process(iterable: Iterable<Item>, payload: Payload) {
        cancel()
        isActive = true
        this.payload = payload
        onStart.invoke(payload)
        processInternal(iterable.iterator(), payload, 0)
    }

    fun cancel() {
        handler.removeCallbacksAndMessages(null)
        isActive = false
        onCancelled.invoke(payload)
        payload = null
    }

    private fun processInternal(
        iterator: Iterator<Item>,
        payload: Payload,
        batchId: Int,
    ) {

        val drawStartTime = SystemClock.elapsedRealtime()

        var shouldWaitNextFrame = false
        while (iterator.hasNext() && !shouldWaitNextFrame && checkTaskIsActive()) {
            val next = iterator.next()
            processItem.invoke(next, payload)
            shouldWaitNextFrame = shouldWaitForNextFrame(drawStartTime)
        }

        if (shouldWaitNextFrame) {
            onBatchCompleted.invoke(batchId, payload)
            handler.post { processInternal(iterator, payload, batchId + 1) }
        } else if (!iterator.hasNext() && isActive) {
            onComplete.invoke(payload)
        }
    }

    private fun checkTaskIsActive(): Boolean {
        return !provideIsCancelled.invoke() && isActive
    }

    private fun shouldWaitForNextFrame(drawStartTime: Long): Boolean {
        return SystemClock.elapsedRealtime() - drawStartTime > batchTimeOut
    }

    companion object {
        private const val DEFAULT_RENDER_LOAD = 0.5f
    }
}