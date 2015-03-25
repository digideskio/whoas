package com.github.lookout.whoas

import java.util.concurrent.ArrayBlockingQueue
import spock.lang.*

class InMemoryQueueSpec extends Specification {

    def "getSize() should return 0 by default"() {
        given:
        InMemoryQueue queue = new InMemoryQueue()

        expect:
        queue.size == 0
    }

    def "pop()ing without a closure should throw"() {
        given:
        InMemoryQueue q = new InMemoryQueue()

        when:
        q.pop()

        then:
        thrown Exception
    }

    def "push() should put onto the internal queue"() {
        given:
        InMemoryQueue queue = new InMemoryQueue()

        when:
        queue.push(new HookRequest())

        then:
        queue.size == 1
    }
}


class InMemoryQueueWithGivenQueueSpec extends Specification {

    protected ArrayBlockingQueue internal
    protected InMemoryQueue queue

    def setup() {
        internal = new ArrayBlockingQueue(1)
        queue = new InMemoryQueue(internal)
    }

    def "getSize() should return the size of the intenral queue"() {
        expect:
        queue.size == internal.size()
    }
}

class InMemoryQueueSpecWithMessage extends InMemoryQueueWithGivenQueueSpec {

    private HookRequest request

    def setup() {
        request = new HookRequest()
        /* Throw our req on the internal queue */
        internal.put(request)
    }

    def "pop() should call the closure"() {
        given:
        Boolean executedClosure = false
        Boolean receivedMessage = false

        when:
        queue.pop {
            executedClosure = true
            receivedMessage = (it == request)
        }

        then:
        executedClosure
        receivedMessage
    }

    def "pop() should requeue on exceptions"() {
        when:
        queue.pop {
            throw new Exception("Spock'd!")
        }

        then:
        queue.size == 1
    }

    def "push()ing more than the internal queue can handle should return false"() {
        setup:
        queue.push(request)

        expect:
        queue.size == 1
        queue.push(request) == false
    }
}
