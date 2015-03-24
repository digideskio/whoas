package com.github.lookout.whoas

import com.fasterxml.jackson.databind.ObjectMapper
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * A redis queue that offers distributed and persistent queue
 */
class RedisQueue extends AbstractHookQueue {
    private WhoasQueueConfig queueConfig
    private JedisPool pool = null
    private static Integer maxActiveConnections = 10
    private static Integer maxIdleConnections = 5
    private static Integer minIdleConnections = 1
    private Logger logger = LoggerFactory.getLogger(RedisQueue.class)

    /**
     * Create the RedisQueue with valid config
     */
    RedisQueue(WhoasQueueConfig queueConfig) {
        this.queueConfig = queueConfig
    }

    /**
     * Default constructor
     */
    RedisQueue() {
        queueConfig = new WhoasQueueConfig()
    }

    /**
     * Return the number of elements in the queue
     */
    Long getSize() {
        if (!this.started) {
            throw new Exception("Queue must be started before this operation is invoked")
        }
        return withRedis() { Jedis redisClient ->
            return redisClient.llen(this.queueConfig.key)
        }
    }

    /**
     * Setup the Redis client
     */
    @Override
    void start() {
        super.start()

        logger.debug("Setting up redis queue \"${this.queueConfig.key}\" on the server " +
                "\"${this.queueConfig.hostname}:${this.queueConfig.port}")

        /**
         * Setup jedis pool
         *
         * A single jedis instance is NOT thread-safe. JedisPool maintains a thread-safe
         * pool of network connections. The pool will allow us to maintain a pool of
         * multiple jedis instances and use them reliably and efficiently across different
         * threads
         */
        JedisPoolConfig poolConfig = new JedisPoolConfig()
        poolConfig.setMaxTotal(maxActiveConnections)
        poolConfig.setTestOnBorrow(true)
        poolConfig.setTestOnReturn(true)
        poolConfig.setMaxIdle(maxIdleConnections)
        poolConfig.setMinIdle(minIdleConnections)
        poolConfig.setTestWhileIdle(true)

        /* Create the pool */
        pool = new JedisPool(poolConfig, this.queueConfig.hostname, this.queueConfig.port)
    }

    /**
     * Stop the Redis client
     */
    @Override
    void stop() {
        super.stop()
        pool.destroy()
        pool = null
    }

    /** Allocate redis client from the pool */
    Object withRedis(Closure closure) {
        Jedis redisClient = pool.resource
        try {
            return closure.call(redisClient)
        }
        finally {
            redisClient.close()
        }
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
            throw new Exception("Must provide a Closure to RedisQueue.pop()")
        }

        if (!this.started) {
            throw new Exception("Queue must be started before this operation is invoked")
        }

        withRedis() { Jedis redisClient ->

            /**
             * The blpop returns list of strings (key and value)
             */
            List<String> messages = redisClient.blpop(0, this.queueConfig.key)

            /* If valid, decode message */
            if (messages) {
                ObjectMapper mapper = new ObjectMapper()
                HookRequest request = mapper.readValue(messages.get(1), HookRequest.class)
                try {
                    action.call(request)
                } catch (Exception ex) {
                    /* Put this back on the front of the queue */
                    logger.info("\"Pop\" on redis queue failed, pushing it back on front of the queue", ex)
                    redisClient.lpush(this.queueConfig.key, messages.get(1))
                    throw ex
                }
            }
        }
    }

    /**
     * Attempt to insert the request into the queue
     *
     * If the request cannot be inserted, this method will return false,
     * otherwise true.
     */
    Boolean push(HookRequest request) {
        if (!this.started) {
            throw new Exception("Queue must be started before this operation is invoked")
        }

        ObjectMapper mapper = new ObjectMapper()
        String jsonPayload = mapper.writeValueAsString(request)
        return withRedis() { Jedis redisClient ->
            return redisClient.rpush(this.queueConfig.key, jsonPayload) != 0
        }
    }
}
