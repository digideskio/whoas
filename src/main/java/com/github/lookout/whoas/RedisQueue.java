package com.github.lookout.whoas;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A redis queue that offers distributed and persistent queue
 */
public class RedisQueue extends AbstractHookQueue {
    private WhoasQueueConfig queueConfig;
    private JedisPool pool = null;
    private static Integer maxActiveConnections = 10;
    private static Integer maxIdleConnections = 5;
    private static Integer minIdleConnections = 1;
    private Logger logger = LoggerFactory.getLogger(RedisQueue.class);

    /**
     * Create the RedisQueue with valid config
     *
     * @param queueConfig necessary configuration to connect to Redis
     */
    public RedisQueue(WhoasQueueConfig queueConfig) {
        this.queueConfig = queueConfig;
    }

    /**
     * Default constructor
     */
    public RedisQueue() {
        queueConfig = new WhoasQueueConfig();
    }

    /**
     * Allow users to provide their own {@code JedisPool} instance
     *
     * @param pool an already set up pool
     */
    public RedisQueue(JedisPool pool) {
        this();
        this.pool = pool;
    }

    /**
     * Return the number of elements in the queue
     */
    public int getSize() throws Exception {
        if (!this.started) {
            throw new Exception("Queue must be started before this operation is invoked");
        }
        return ((Integer)withRedis(new RedisQueueAction<Integer>() {
            @Override
            public Integer call(Jedis redisClient) {
                Long size = redisClient.llen(queueConfig.key);
                return size.intValue();
            }
        })).intValue();
    }

    /**
     * Setup the Redis client
     */
    @Override
    public void start() {
        super.start();

        /* Bail early if we already have a valid pool */
        if (this.pool instanceof JedisPool) {
            return;
        }

        logger.debug("Setting up redis queue \"${this.queueConfig.key}\" on the server " +
                "\"${this.queueConfig.hostname}:${this.queueConfig.port}");


        /**
         * Setup jedis pool
         *
         * A single jedis instance is NOT thread-safe. JedisPool maintains a thread-safe
         * pool of network connections. The pool will allow us to maintain a pool of
         * multiple jedis instances and use them reliably and efficiently across different
         * threads
         */
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxActiveConnections);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setMaxIdle(maxIdleConnections);
        poolConfig.setMinIdle(minIdleConnections);
        poolConfig.setTestWhileIdle(true);

        /* Create the pool */
        pool = new JedisPool(poolConfig, this.queueConfig.hostname, this.queueConfig.port);
    }

    /**
     * Stop the Redis client
     */
    @Override
    public void stop() {
        super.stop();
        pool.destroy();
        pool = null;
    }

    /**
     * Performs a blocking pop on the queue and invokes the closure with the
     * item popped from the queue
     *
     * If the Closure throws an exception, the dequeued item will be returned
     * to the tail end of the queue
     */
    public void pop(final QueueAction action) throws Exception {
        if (action == null) {
            throw new Exception("Must provide a Closure to RedisQueue.pop()");
        }

        if (!this.started) {
            throw new Exception("Queue must be started before this operation is invoked");
        }

        withRedis(new RedisQueueAction<Long>() {
            @Override
            public Long call(Jedis redisClient) throws Exception {
                /**
                * The blpop returns list of strings (key and value)
                */
                List<String> messages = redisClient.blpop(0, queueConfig.key);

                /* If valid, decode message */
                if ((messages != null) && (!messages.isEmpty())) {
                    ObjectMapper mapper = new ObjectMapper();
                    HookRequest request = mapper.readValue(messages.get(1), HookRequest.class);
                    try {
                        action.call(request);
                    } catch (Exception ex) {
                        /* Put this back on the front of the queue */
                        logger.info("\"Pop\" on redis queue failed, pushing it back on front of the queue", ex);
                        return redisClient.lpush(queueConfig.key, messages.get(1));
                    }
                }
                return new Long(-1);
            }
        });
    }

    /**
     * Attempt to insert the request into the queue
     *
     * If the request cannot be inserted, this method will return false,
     * otherwise true.
     *
     * @param request A {@code HookRequest} to enqueue
     */
    @Override
    public Boolean push(HookRequest request) throws JsonProcessingException, Exception {
        if (!this.started) {
            throw new Exception("Queue must be started before this operation is invoked");
        }

        ObjectMapper mapper = new ObjectMapper();
        final String jsonPayload = mapper.writeValueAsString(request);
        return (Boolean)withRedis(new RedisQueueAction<Boolean>() {
            @Override
            public Boolean call(Jedis redisClient) {
                System.out.println(String.format("%s %s", queueConfig.key, jsonPayload));
                redisClient.rpush(queueConfig.key, jsonPayload);
                return true;
            }
        });
    }

    /** Allocate redis client from the pool
     *
     * @param action callback to invoke with a {@code Jedis} object from the
     *  pool
     * @throws Exception propogates underlying Jedis exceptions
     * @return propogates a generic {@code Object} up from the {@code RedisQueueAction}
     */
    protected Object withRedis(RedisQueueAction action) throws Exception {
        Jedis redisClient = this.pool.getResource();
        System.out.println(redisClient.toString());
        try {
            return action.call(redisClient);
        }
        finally {
            redisClient.close();
        }
    }

}
