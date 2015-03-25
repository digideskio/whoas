package com.github.lookout.whoas

import com.fiftyonred.mock_jedis.MockJedis
import redis.clients.jedis.Jedis
import spock.lang.*


class RedisQueueSpec extends Specification {

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
        RedisQueue queue = new RedisQueue()
        Jedis redisClient = new MockJedis("test")

        when:
        queue.start()
        queue.pool.metaClass.getResource = {redisClient}

        then:
        queue.getSize() == 0
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

    def "push() should put onto the internal queue"() {
        given:
        RedisQueue queue = new RedisQueue()
        Jedis redisClient = new MockJedis("test")
        redisClient.metaClass.rpush = {String key, String payload -> redisClient.lpush(key, payload)}

        when:
        queue.start()
        queue.pool.metaClass.getResource = {redisClient}
        queue.push(new HookRequest())
        queue.push(new HookRequest())

        then:
        queue.getSize() == 2
    }

    def "pop() after push should receive a request"() {
        given:
        RedisQueue queue = new RedisQueue()
        Jedis redisClient = new MockJedis("test")
        redisClient.metaClass.rpush = {String key, String payload -> redisClient.lpush(key, payload)}
        redisClient.metaClass.blpop = {Integer timeout, String key -> [key, redisClient.lpop(key)]}

        when:
        queue.start()
        queue.pool.metaClass.getResource = {redisClient}
        HookRequest test = new HookRequest()
        queue.push(test)

        then:
        queue.getSize() == 1
        queue.pop() { HookRequest fetched -> fetched == test}
        queue.getSize() == 0
    }

    def "push() on rpush exception should return false"() {
        given:
        RedisQueue queue = new RedisQueue()
        Jedis redisClient = new MockJedis("test")
        redisClient.metaClass.rpush = {String key, String payload -> throw new Exception("Test Exception")}

        when:
        queue.start()
        queue.pool.metaClass.getResource = {redisClient}
        queue.push(new HookRequest())

        then:
        thrown Exception
    }

    def "pop() on blpop exception simple return, nothing to requeue "() {
        given:
        RedisQueue queue = new RedisQueue()
        Jedis redisClient = new MockJedis("test")
        redisClient.metaClass.blpop = {Integer timeout, String key -> throw new Exception("Test Exception")}

        when:
        queue.start()
        queue.pool.metaClass.getResource = {redisClient}
        queue.pop() { }

        then:
        thrown Exception
    }

    def "pop() on exception while executing closure should requeue"() {
        given:
        RedisQueue queue = new RedisQueue()
        Jedis redisClient = new MockJedis("test")
        redisClient.metaClass.rpush = {String key, String payload -> redisClient.lpush(key, payload)}
        redisClient.metaClass.blpop = {Integer timeout, String key -> [key, redisClient.lpop(key)]}

        when:
        queue.start()
        queue.pool.metaClass.getResource = {redisClient}
        queue.push(new HookRequest())
        queue.pop() { throw new Exception("Test Exception") }

        then:
        queue.getSize() == 1
    }
}

