package com.github.lookout.whoas;

/**
 * The SequentialHookRunner is will dequeue HookRequest items from the
 * configured AbstractHookQueue and publish those webhooks sequentially.
 *
 * This is the simplest and slowest hook runner
 */
public class SequentialHookRunner extends AbstractHookRunner {
    public SequentialHookRunner(AbstractHookQueue hookQueue) {
        super(hookQueue);
    }

    public SequentialHookRunner(AbstractHookQueue hookQueue,
                              Publisher hookPublisher) {
        super(hookQueue, hookPublisher);
    }

    /** Execute an infinitely blocking single-threaded runloop */
    public void run() throws Exception {
        while (this.keepGoing) {
            this.queue.pop(new QueueAction() {
                @Override
                public void call(HookRequest request) throws InterruptedException {
                    publisher.publish(request);
                }
            });
        }
    }
}
