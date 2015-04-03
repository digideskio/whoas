package com.github.lookout.whoas

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime


class HookRequest {
    @JsonProperty
    public Long retries

    @JsonProperty
    public String url

    @JsonProperty
    public String postData

    @JsonProperty
    public DateTime deliverAfter

    @JsonProperty
    public String contentType

    /** Constructor for Jackson */
    HookRequest() { }

    /**
     * Default constructor for creating a simple HookRequest with a URL and the
     * POST data to be delivered to that URL
     */
    HookRequest(String hookUrl, String hookData, String contentType) {
        this.retries = 0
        this.url = hookUrl
        this.postData = hookData
        this.contentType = contentType
    }
}
