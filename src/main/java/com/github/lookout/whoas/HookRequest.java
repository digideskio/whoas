package com.github.lookout.whoas;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;


public class HookRequest {
    @JsonProperty
    public int retries;

    @JsonProperty
    public String url;

    @JsonProperty
    public String postData;

    @JsonProperty
    public DateTime deliverAfter;

    @JsonProperty
    public String contentType;

    /** Constructor for Jackson */
    public HookRequest() { }

    /**
     * Default constructor for creating a simple HookRequest with a URL and the
     * POST data to be delivered to that URL
     *
     * @param hookUrl a full URL to deliver the payload to
     * @param hookData Payload for the request
     * @param contentType String-representation of content type, e.g. "application/json"
     */
    public HookRequest(String hookUrl, String hookData, String contentType) {
        this.retries = 0;
        this.url = hookUrl;
        this.postData = hookData;
        this.contentType = contentType;
    }
}
