package com.github.lookout.whoas;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import javax.ws.rs.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Publisher is the class responsible for implementing the *actual* HTTP
 * request logic for Whoas
 */
public class Publisher {
    private final String DEFAULT_CONTENT_TYPE = "application/json";
    /** Maximum number of failures we will retry on */
    private final int DEFAULT_MAX_RETRIES = 5;

    private final int DEFAULT_BACKOFF_MILLIS = 50;
    private final int DEFAULT_BACKOFF_MAX_MILLIS = (10 * 1000);

    private Client jerseyClient;
    private int maxRetries;
    private Logger logger = LoggerFactory.getLogger(Publisher.class);

    public Publisher() {
        this.jerseyClient = ClientBuilder.newClient();
        this.maxRetries = DEFAULT_MAX_RETRIES;
    }

    /**
     * Publish the request using the appropriate backoff and retry logic
     * defined in the Whoas documentation
     *
     * @param request a valid {@code HookRequest}
     * @return true if we were able to invoke the {@code HookRequest}
     * @throws InterruptedException thrown if our attempts to backoff were interrupted
     */
    public Boolean publish(HookRequest request) throws InterruptedException {
        Response response = null;
        Boolean retryableExc = false;
        Invocation inv = buildInvocationFrom(request);

        try {
            response = inv.invoke();
            String responseBody = response.readEntity(String.class);
        }
        catch (ProcessingException exc) {
            logger.warn("POST to url \"{}\" failed", request.url, exc);
            retryableExc = true;
        }

        if ((retryableExc) || (shouldRetry(response))) {
            if (request.retries >= this.maxRetries) {
                logger.error("Giving up on POST to url \"{}\" after {} retries",
                             request.url, request.retries);
                return false;
            }
            request.retries = (request.retries + 1);
            backoffSleep(request.retries);
            return this.publish(request);
        }

        logger.debug("POST to url \"{}\" succeeded", request.url);
        return true;
    }

    /**
     * Determine whether this response meets our criteria for retry
     *
     * @param response {@code Response} which has a status to determine our retry
     * @return true if the caller should continue retrying the request
     */
    public Boolean shouldRetry(Response response) {
        if (response == null) {
            return true;
        }

        /* Enhance your calm and try again */
        if ((response.getStatus() == 420) ||
            (response.getStatus() == 429)) {
            return true;
        }

        /* All server side errors we'll attempt to retry */
        if ((response.getStatus() >= 500) &&
            (response.getStatus() < 600)) {
            return true;
        }

        return false;
    }

    /**
     * Sleep the current thread the appropriate amount of time for the
     * attemptNumber
     *
     * @param attemptNumber which attempt we're so we can exponentially sleep
     * @throws InterruptedException thrown if our sleep is interrupted
     */
    void backoffSleep(int attemptNumber) throws InterruptedException  {
        int naptime = (int)(Math.pow(DEFAULT_BACKOFF_MILLIS, attemptNumber));
        if (naptime > DEFAULT_BACKOFF_MAX_MILLIS) {
            naptime = DEFAULT_BACKOFF_MAX_MILLIS;
        }
        Thread.sleep(naptime);
    }

    /**
     * Build the JerseyInvocation instance needed to execute the webhook
     *
     */
    private Invocation buildInvocationFrom(HookRequest request) {
        String contentType = DEFAULT_CONTENT_TYPE;

        if ((request.contentType != null) &&
                (!request.contentType.isEmpty())) {
            contentType = request.contentType;
        }

        return jerseyClient.target(request.url)
                    .request()
                    .buildPost(Entity.entity(request.postData, contentType));
    }
}
