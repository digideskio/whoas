package com.github.lookout.whoas

import spock.lang.*

import javax.ws.rs.client.Invocation
import javax.ws.rs.core.Response

class PublisherSpec extends Specification {
    private Publisher publisher


    def setup() {
        publisher = new Publisher()
    }

    def "publish() to a invalid host should fail"() {
        given:
        publisher = Spy(Publisher)
        HookRequest req = new HookRequest('http://spock.invalid', '', '')
        /* stub out the original backoffSleep so we don't actually sleep our
         * tests */
        _ * publisher.backoffSleep(_) >> null

        expect:
        ! publisher.publish(req)

    }

    def "shouldRetry() for status codes"() {
        given:
        Response r = Mock(Response)
        _ * r.status >> status

        expect:
        publisher.shouldRetry(r) == expected

        where:
        status | expected
        200    | false
        201    | false
        400    | false
        420    | true
        500    | true
        599    | true
    }


    def "buildInvocationFrom() should create a valid Jersey Invocation"() {
        given:
        Invocation inv
        HookRequest request = new HookRequest('http://example.com',
                                                'magic post data!','application/vnd.appname.event1.v1+json')

        when:
        inv = publisher.buildInvocationFrom(request)

        then:
        inv instanceof Invocation
    }

    def "buildInvocationFrom() should create a valid Jersey Invocation when content type is null"() {
        given:
        Invocation inv
        HookRequest request = new HookRequest('http://example.com',
                'magic post data!', null)

        when:
        inv = publisher.buildInvocationFrom(request)

        then:
        inv instanceof Invocation
    }

    def "buildInvocationFrom() should create a valid Jersey Invocation when content type is empty"() {
        given:
        Invocation inv
        HookRequest request = new HookRequest('http://example.com',
                'magic post data!', '')

        when:
        inv = publisher.buildInvocationFrom(request)

        then:
        inv instanceof Invocation
    }
}
