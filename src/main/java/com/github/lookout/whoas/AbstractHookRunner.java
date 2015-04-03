package com.github.lookout.whoas;


/**
 *
 */
public abstract class AbstractHookRunner {
    protected AbstractHookQueue queue;
    protected Publisher publisher;
    protected Boolean keepGoing = true;

    public AbstractHookRunner(AbstractHookQueue hookQueue) {
        this(hookQueue, new Publisher());
    }

    public AbstractHookRunner(AbstractHookQueue hookQueue,
                              Publisher hookPublisher) {
        this.publisher = hookPublisher;
        this.queue = hookQueue;
    }

    public Publisher getPublisher() {
        return this.publisher;
    }

    /** Block forever and run the runner's runloop. */
    public abstract void run() throws Exception;

    /**
     * Tell the runloop to stop
     *
     * This will only come into effect after the runner has completed it's
     * currently executing work
     */
    public void stop() {
        this.keepGoing = false;
    }
}
