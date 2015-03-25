package com.github.lookout.whoas

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime


class HookRequest {

    @JsonProperty
    private Long retries

    @JsonProperty
    private String url

    @JsonProperty
    private String postData

    @JsonProperty
    private DateTime deliverAfter

    @JsonProperty
    private String contentType

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
