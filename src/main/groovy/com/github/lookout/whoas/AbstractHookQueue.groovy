package com.github.lookout.whoas


/**
 * Interface defining how 'HookQueue' providers should behave
 *
 * This allows for different queueing implementations behind whoas
 */
abstract class AbstractHookQueue {
    protected Boolean started = false

    void start() {
        if (started) {
            throw new IllegalStateException()
        }
        started = true
    }

    void stop() {
        if (!started) {
            throw new IllegalStateException()
            return
        }
        started = false
    }

    /**
     * Return the size of the queue, may not be implemented by some providers
     * in which case it will return -1
     */
    abstract int getSize()

    /**
     *
     */
    abstract void pop(QueueAction action) throws Exception

    /**
     *
     */
    abstract Boolean push(HookRequest request)
}
