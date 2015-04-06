package com.github.lookout.whoas;

import redis.clients.jedis.Jedis;

public interface RedisQueueAction<T> {
    public T call(Jedis jedis) throws Exception;
}
