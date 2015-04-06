package com.github.lookout.whoas;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  This factory will allow clients of whoas to build
 *  different queues like in memory, persistent etc and runners
 *  like sequential.
 */
public class WhoasFactory {
    protected Logger logger = LoggerFactory.getLogger(WhoasFactory.class);

    /**
     *  Queue configuration
     */
    @JsonProperty(value = "queue")
    public WhoasQueueConfig queueConfig = new WhoasQueueConfig();

    /**
     * Type of runner to create in whoas.
     *
     * Default runner in whoas is SequentialHookRunner
     */
    @JsonProperty
    public String runnerType = "com.github.lookout.whoas.SequentialHookRunner";

    /**
     * Allocate and return the queue based on stored queue type.
     *
     * @throws ClassNotFoundException - if the class is not found
     * @throws IllegalAccessException - if the class or its nullary constructor is not accessible.
     * @throws InstantiationException - if this Class cannot be instantiaed
     * @throws InvocationTargetException if the constructor can not be invoked
     * @throws NoSuchMethodException if the appropriate constructor cannot be found
     *
     * @return a properly configured {@code AbstractHookQueue}
     */
    public AbstractHookQueue buildQueue() throws ClassNotFoundException,
                                                 NoSuchMethodException,
                                                 InstantiationException,
                                                 IllegalAccessException,
                                                 InvocationTargetException {
        Class queueClass = Class.forName(this.queueConfig.type);
        Constructor<AbstractHookQueue> builder = queueClass.getDeclaredConstructor(WhoasQueueConfig.class);
        return builder.newInstance(this.queueConfig);
    }

    /**
     * Allocate and return runner based on stored runner type
     *
     * @throws ClassNotFoundException - if the class is not found
     * @throws IllegalAccessException - if the class or its nullary constructor is not accessible.
     * @throws InstantiationException - if this Class cannot be instantiaed
     * @throws InvocationTargetException if the constructor can not be invoked
     * @throws NoSuchMethodException if the appropriate constructor cannot be found
     *
     * @param hookQueue queue to associate with allocated runner
     * @return a properly configured {@code AbstractHookRunner} instance
     */
    public AbstractHookRunner buildRunner(AbstractHookQueue hookQueue) throws ClassNotFoundException,
                                                                              NoSuchMethodException,
                                                                              InstantiationException,
                                                                              IllegalAccessException,
                                                                              InvocationTargetException {
        Class runnerClass = Class.forName(this.runnerType);
        Constructor<AbstractHookRunner> builder = runnerClass.getDeclaredConstructor(AbstractHookQueue.class);
        return builder.newInstance(hookQueue);
    }
}
