package com.github.lookout.whoas

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.Response
import javax.ws.rs.ProcessingException
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Publisher is the class responsible for implementing the *actual* HTTP
 * request logic for Whoas
 */
class Publisher {
    private final String DEFAULT_CONTENT_TYPE = 'application/json'
    /** Maximum number of failures we will retry on */
    private final int DEFAULT_MAX_RETRIES = 5

    private final int DEFAULT_BACKOFF_MILLIS = 50
    private final int DEFAULT_BACKOFF_MAX_MILLIS = (10 * 1000)

    private Client jerseyClient
    private int maxRetries
    private Logger logger = LoggerFactory.getLogger(Publisher.class)

    Publisher() {
        this.jerseyClient = ClientBuilder.newClient()
        this.maxRetries = DEFAULT_MAX_RETRIES
    }

    /**
     * Publish the request using the appropriate backoff and retry logic
     * defined in the Whoas documentation
     */
    Boolean publish(HookRequest request) {
        Response response
        Boolean retryableExc = false
        Invocation inv = buildInvocationFrom(request)
        try {
            response = inv.invoke()
            String responseBody = response.readEntity(String.class)
        }
        catch (ProcessingException exc) {
            logger.warn("\"POST\" to url: \"${request.url}\" " +
                    "has failed with exception: " + exc.getMessage())
            retryableExc = true
        }

        if ((retryableExc) || (shouldRetry(response))) {
            if (request.retries >= this.maxRetries) {
                logger.error("Giving up on \"POST\" to url: \"${request.url}\" " +
                        "after ${request.retries} retries")
                return false
            }
            request.retries = (request.retries + 1)
            backoffSleep(request.retries)
            return this.publish(request)
        }

        logger.debug("\"POST\" to url: \"${request.url}\" succeeded, ${logger.name}, ${logger.debugEnabled}")
        return true
    }

    /**
     * Determine whether this response meets our criteria for retry
     */
    Boolean shouldRetry(Response response) {
        if (response == null) {
            return true
        }

        /* Enhance your calm and try again */
        if (response.status == 420) {
            return true
        }

        /* All server side errors we'll attempt to retry */
        if ((response.status >= 500) &&
            (response.status < 600)) {
            return true
        }

        return false
    }

    /**
     * Sleep the current thread the appropriate amount of time for the
     * attemptNumber
     */
    void backoffSleep(Long attemptNumber) {
        int naptime = DEFAULT_BACKOFF_MILLIS ** attemptNumber
        if (naptime > DEFAULT_BACKOFF_MAX_MILLIS) {
            naptime = DEFAULT_BACKOFF_MAX_MILLIS
        }
        Thread.sleep(naptime)
    }

    /**
     * Build the JerseyInvocation instance needed to execute the webhook
     */
    private Invocation buildInvocationFrom(HookRequest request) {
        return jerseyClient.target(request.url)
                    .request()
                    .buildPost(Entity.entity(request.postData,
                                             request.contentType? request.contentType: DEFAULT_CONTENT_TYPE))
    }

}
