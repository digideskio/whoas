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
     * If the queue cannot be created, then this throws
     * ClassNotFoundException - if the class is not found
     * IllegalAccessException - if the class or its nullary constructor is not accessible.
     * InstantiationException - if this Class represents an abstract class, an interface,
     *                          an array class, a primitive type, or void
     *                          or if the class has no nullary constructor
     *                          or if the instantiation fails for some other reason.
     * @return allocated queue
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
     * If the runner cannot be created, then this throws
     * ClassNotFoundException - if the class is not found
     * IllegalAccessException - if the class or its nullary constructor is not accessible.
     * InstantiationException - if this Class represents an abstract class, an interface,
     *                          an array class, a primitive type, or void
     *                          or if the class has no nullary constructor
     *                          or if the instantiation fails for some other reason.
     * @param hookQueue queue to associate with allocated runner
     * @return
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
