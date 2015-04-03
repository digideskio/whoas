package com.github.lookout.whoas;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;;


/**
 * A simple in-memory queue that offers no persistence between process restarts
 */
public class InMemoryQueue extends AbstractHookQueue {
    private BlockingQueue<HookRequest> internalQueue;
    private Logger logger = LoggerFactory.getLogger(InMemoryQueue.class);

    /**
     * Create the InMemoryQueue from configuration
     *
     * @param queueConfig not used
     */
    public InMemoryQueue(WhoasQueueConfig queueConfig) {
        this.internalQueue = new LinkedBlockingQueue<HookRequest>();
    }

    /**
     * Default constructor
     */
    public InMemoryQueue() {
        this.internalQueue = new LinkedBlockingQueue<HookRequest>();
    }

    /**
     * Create the InMemoryQueue with the given Queue object
     *
     * @param queue aubclass of {@code BlockingQueue} which we will use instead of the default internal memory queue
     */
    public InMemoryQueue(BlockingQueue<HookRequest> queue) {
        this.internalQueue = queue;
    }

    /**
     * Return the number of elements in the queue
     *
     * @return number of elements in the queue
     */
    public int getSize() {
        return this.internalQueue.size();
    }

    /**
     * Performs a blocking pop on the queue and invokes the closure with the
     * item popped from the queue
     *
     * If the Closure throws an exception, the dequeued item will be returned
     * to the tail end of the queue
     */
    public void pop(QueueAction action) throws InterruptedException, Exception {
        if (action == null) {
            throw new Exception("Must provide a Closure to InMemoryQueue.pop()");
        }

        HookRequest item = this.internalQueue.take();

        try {
            action.call(item);
        }
        catch (Exception ex) {
            /* Put this back on the tail end of the queue */
            logger.info("\"Pop\" on in-memory queue failed, putting it back on the tail-end", ex);
            this.internalQueue.put(item);
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
    public Boolean push(HookRequest request) {
        return this.internalQueue.offer(request);
    }
}
