package com.github.lookout.whoas

import com.fiftyonred.mock_jedis.MockJedis
import com.fiftyonred.mock_jedis.MockJedisPool
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

import spock.lang.*


class RedisQueueSpec extends Specification {
    protected MockJedisPool mockPool() {
        return new MockJedisPool(new JedisPoolConfig(), 'example.com')
    }

    def "getSize()ing without a start should throw"() {
        given:
        RedisQueue q = new RedisQueue()

        when:
        q.getSize()

        then:
        thrown Exception
    }

    def "getSize() should return 0 by default"() {
        given:
        RedisQueue queue = new RedisQueue(mockPool())

        when:
        queue.start()

        then:
        queue.size == 0
    }

    def "pop()ing without a closure should throw"() {
        given:
        RedisQueue q = new RedisQueue()

        when:
        q.pop()

        then:
        thrown Exception
    }

    def "pop()ing without a start should throw"() {
        given:
        RedisQueue q = new RedisQueue()

        when:
        q.pop()

        then:
        thrown Exception
    }

    def "push()ing without a start should throw"() {
        given:
        RedisQueue q = new RedisQueue()

        when:
        queue.push(new HookRequest())

        then:
        thrown Exception
    }

}

/** Spec for testing Jedis interactions with mock-jedis */
class RedisQueueMockIntegrationSpec extends RedisQueueSpec {
    protected RedisQueue queue
    protected Jedis client
    protected List<String> store = []

    def setup() {
        this.queue = new RedisQueue(mockPool())
        this.client = Mock(MockJedis, constructorArgs: ['example.com'])
        this.queue.pool.client = this.client

        _ * client.llen(_) >> { this.store.size }
    }

    def "push() should put onto the internal queue"() {
        given:
        2 * client.rpush(_, _) >> { key, payload -> store << payload[]; return 1 }

        when:
        queue.start()
        queue.push(new HookRequest())
        queue.push(new HookRequest())

        then:
        queue.size == 2
    }

    def "pop() after push should receive a request"() {
        given:
        /* Mock rpush() and just say it pushed one element
         * Due to some weirdness in Spock's mocking, `payload` comes in as a
         * list of strings
         */
        1 * client.rpush(_, _) >> { key, payload -> store << payload[0]; return 1 }
        /* Need to return a stupid List since jedis */
        1 * client.blpop(_, _) >> { [null, store.pop()] }

        when:
        queue.start()
        HookRequest test = new HookRequest()
        queue.push(test)

        then:
        store.size == 1
        queue.pop { HookRequest fetched -> fetched == test}
        store.size == 0
    }

    def "push() on rpush exception should return false"() {
        given:
        1 * client.rpush(*_) >> { throw new Exception('Spockd!') }

        when:
        queue.start()
        queue.push(new HookRequest())

        then:
        thrown Exception
    }

    def "pop() on blpop exception simple return, nothing to requeue "() {
        given:
        1 * client.blpop(*_) >> { throw new Exception('Spockd!') }

        when:
        queue.start()
        queue.pop() { }

        then:
        thrown Exception
    }

    def "pop() on exception while executing closure should requeue"() {
        given:
        1 * client.rpush(_, _) >> { key, payload -> this.store << payload[0]; return 1 }
        1 * client.blpop(*_) >> { [null, this.store.pop()] }
        1 * client.lpush(_, _) >> { key, payload -> this.store << payload[0]; return 1 }

        when:
        queue.start()
        queue.push(new HookRequest())
        queue.pop() { throw new Exception("Test Exception") }

        then:
        queue.size == 1
    }
}
