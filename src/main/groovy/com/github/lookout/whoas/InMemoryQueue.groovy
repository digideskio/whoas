package com.github.lookout.whoas

import java.util.Queue
import java.util.concurrent.LinkedBlockingQueue
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * A simple in-memory queue that offers no persistence between process restarts
 */
class InMemoryQueue extends AbstractHookQueue {
    private Queue<HookRequest> internalQueue
    private Logger logger = LoggerFactory.getLogger(InMemoryQueue.class)

    /**
     * Create the InMemoryQueue from configuration
     */
    InMemoryQueue(WhoasQueueConfig queueConfig) {
        this.internalQueue = new LinkedBlockingQueue<HookRequest>()
    }

    /**
     * Default constructor
     */
    InMemoryQueue() {
        this.internalQueue = new LinkedBlockingQueue<HookRequest>()
    }

    /**
     * Create the InMemoryQueue with the given Queue object
     */
    InMemoryQueue(Queue<HookRequest> queue) {
        this.internalQueue = queue
    }

    /**
     * Return the number of elements in the queue
     */
    Long getSize() {
        return this.internalQueue.size()
    }

    /**
     * Performs a blocking pop on the queue and invokes the closure with the
     * item popped from the queue
     *
     * If the Closure throws an exception, the dequeued item will be returned
     * to the tail end of the queue
     */
    void pop(Closure action) {
        if (action == null) {
            throw new Exception("Must provide a Closure to InMemoryQueue.pop()")
        }

        Object item = this.internalQueue.take()

        try {
            action.call(item)
        }
        catch (Exception ex) {

            /* Put this back on the tail end of the queue */
            logger.info("\"Pop\" on in-memory queue failed, putting it back on the tail-end", ex)
            this.internalQueue.put(item)
        }
        finally {
        }
    }

    /**
     * Attempt to insert the request into the queue
     *
     * If the request cannot be inserted, this method will return false,
     * otherwise true.
     */
    Boolean push(HookRequest request) {
        return this.internalQueue.offer(request)
    }
}
