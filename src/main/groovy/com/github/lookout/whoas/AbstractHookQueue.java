package com.github.lookout.whoas;

/**
 * Interface defining how 'HookQueue' providers should behave
 *
 * This allows for different queueing implementations behind whoas
 */
public abstract class AbstractHookQueue {
    protected Boolean started = false;

    public void start() {
        if (started) {
            throw new IllegalStateException();
        }
        started = true;
    }

    public void stop() {
        if (!started) {
            throw new IllegalStateException();
        }
        started = false;
    }

    /**
     * Return the size of the queue, may not be implemented by some providers
     * in which case it will return -1
     */
    public abstract int getSize();

    /**
     *
     */
    public abstract void pop(QueueAction action) throws Exception;

    /**
     *
     */
    public abstract Boolean push(HookRequest request);
}
