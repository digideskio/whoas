package com.github.lookout.whoas

import spock.lang.*

import java.util.concurrent.ArrayBlockingQueue

class SequentialHookRunnerSpec extends Specification {
    private InMemoryQueue queue = new InMemoryQueue(new ArrayBlockingQueue(1))

    def "the construction should create a publisher"() {
        given:
        SequentialHookRunner runner = new SequentialHookRunner(queue)

        expect:
        runner.publisher instanceof Publisher
    }

    def "ensure the run() method dispatches queued requests"() {
        given:
        Publisher p = Mock(Publisher)
        SequentialHookRunner runner = new SequentialHookRunner(queue, p)
        HookRequest request = new HookRequest('http://spock.invalid', '{}', '')
        1 * p.publish(request) >> {
            /* when the publisher is called, let's disable our runner's runloop
             * so we actually exit the test!
             */
             runner.stop()
        }
        queue.push(request)

        when:
        runner.run()

        then:
        queue.size == 0
    }
}
