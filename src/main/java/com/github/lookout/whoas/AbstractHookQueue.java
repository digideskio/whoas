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
     * @return Size of the queue, if not implemented by the provider, returns -1
     * @throws Exception implementors may throw Exceptions
     */
    public abstract int getSize() throws Exception;

    /**
     * @param action a {@code QueueAction} to invoke
     * @throws Exception the underlying {@code QueueAction} may throw any form of exception
     */
    public abstract void pop(QueueAction action) throws Exception;

    /**
     * @param request A valid {@code HookRequest}
     * @return true if the {@code HookRequest} was successfully added to the queue
     * @throws Exception implementors may throw Exceptions
     */
    public abstract Boolean push(HookRequest request) throws Exception;
}
