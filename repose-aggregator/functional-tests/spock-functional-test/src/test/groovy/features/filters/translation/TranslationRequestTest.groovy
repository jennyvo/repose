package features.filters.translation

import framework.ReposeValveTest
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.Handling
import org.rackspace.deproxy.MessageChain
import org.rackspace.deproxy.Response
import spock.lang.Unroll

class TranslationRequestTest extends ReposeValveTest {

    def static String xmlPayLoad = "<a><remove-me>test</remove-me>somebody</a>"
    def static String rssPayload = "<a>test body</a>"
    def static String xmlPayloadWithEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [   <!ENTITY c SYSTEM  \"/etc/passwd\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&c;</a>"
    def static String xmlPayloadWithExtEntities = "<?xml version=\"1.0\" standalone=\"no\" ?> <!DOCTYPE a [  <!ENTITY license_agreement SYSTEM \"http://www.mydomain.com/license.xml\"> ]>  <a><remove-me>test</remove-me>&quot;somebody&license_agreement;</a>"
    def static String xmlPayloadWithXmlBomb = "<?xml version=\"1.0\"?> <!DOCTYPE lolz [   <!ENTITY lol \"lol\">   <!ENTITY lol2 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">   <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">   <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">   <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">   <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">   <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">   <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">   <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\"> ]> <lolz>&lol9;</lolz>"
    def static String jsonPayload = "{\"field1\": \"value1\", \"field2\": \"value2\"}"
    def static String invalidXml = "<a><remove-me>test</remove-me>somebody"
    def static String invalidJson = "{{'field1': \"value1\", \"field2\": \"value2\"]}"


    def static Map acceptXML = ["accept": "application/xml"]
    def static Map contentXML = ["content-type": "application/xml"]
    def static Map contentJSON = ["content-type": "application/json"]
    def static Map contentXMLHTML = ["content-type": "application/xhtml+xml"]
    def static Map contentOther = ["content-type": "application/other"]
    def static Map contentRss = ["content-type": "application/rss+xml"]

    def static ArrayList<String> xmlJSON = ["<json:string name=\"field1\">value1</json:string>", "<json:string name=\"field2\">value2</json:string>"]
    def static String remove = "remove-me"
    def static String add = "add-me"


    def String convertStreamToString(byte[] input){
        return new Scanner(new ByteArrayInputStream(input)).useDelimiter("\\A").next();
    }


    //Start repose once for this particular translation test
    def setupSpec() {

        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/common", params)
        repose.configurationProvider.applyConfigs("features/filters/translation/request", params)
        repose.start()
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)

    }

    def cleanupSpec() {
        if(deproxy)
            deproxy.shutdown()
        if(repose)
            repose.stop()
    }

    def "Timebomb to deal with saxon-ee dependency for translation filter"() {

        when:
        1

        then:
        assert new Date() < new Date(2014 - 1900, Calendar.MARCH, 17, 9, 0)
    }

    @Unroll("response: xml, request: #reqHeaders - #reqBody")
    def "when translating requests"() {

        given: "Repose is configured to translate requests"
        def xmlResp = { request -> return new Response(200, "OK", contentXML) }


        when: "User passes a request through repose"
        def resp = deproxy.makeRequest(url:(String) reposeEndpoint, method:method, headers:reqHeaders, requestBody:reqBody, defaultHandler: xmlResp)
        def sentRequest = ((MessageChain) resp).handlings[0]

        then: "Request body sent from repose to the origin service should contain"

        resp.receivedResponse.code == responseCode

        if(responseCode != "400"){
            for (String st : shouldContain) {
                if(sentRequest.request.body instanceof byte[])
                    assert(convertStreamToString(sentRequest.request.body).contains(st))
                else
                    assert(sentRequest.request.body.contains(st))
            }
        }


        and: "Request body sent from repose to the origin service should not contain"

        if(responseCode != "400"){
            for (String st : shouldNotContain) {
                if(sentRequest.request.body instanceof byte[])
                    assert(!convertStreamToString(sentRequest.request.body).contains(st))
                else
                    assert(!sentRequest.request.body.contains(st))
            }
        }

        where:
        reqHeaders                 | reqBody                   | shouldContain  | shouldNotContain | method | responseCode
        acceptXML + contentXML     | xmlPayLoad                | ["somebody"]   | [remove]         | "POST" | '200'
        acceptXML + contentXML     | xmlPayloadWithEntities    | ["\"somebody"] | [remove]         | "POST" | '200'
        acceptXML + contentXML     | xmlPayloadWithXmlBomb     | ["\"somebody"] | [remove]         | "POST" | '400'
        acceptXML + contentXMLHTML | xmlPayLoad                | [add]          | []               | "POST" | '200'
        acceptXML + contentXMLHTML | xmlPayLoad                | [xmlPayLoad]   | [add]            | "PUT"  | '200'
        acceptXML + contentJSON    | jsonPayload               | [add]+ xmlJSON | []               | "POST" | '200'
        acceptXML + contentOther   | jsonPayload               | [jsonPayload]  | [add]            | "POST" | '200'
        acceptXML + contentXML     | xmlPayloadWithExtEntities | ["\"somebody"] | [remove]         | "POST" | "200"


    }

    def "when translating application/rss+xml requests with header translations"() {

        given: "Repose is configured to translate request headers"
        def respHeaders = ["content-type": "application/xml"]
        def testHeaders = ['test':'x', 'other': 'y']
        def xmlResp = { request -> return new Response(200, "OK", respHeaders, rssPayload) }


        when: "User sends a request through repose"
        def resp = deproxy.makeRequest(url:(String) reposeEndpoint + "/somepath?testparam=x&otherparam=y", method:"POST", headers:contentRss + acceptXML + testHeaders, requestBody:rssPayload, defaultHandler:xmlResp)
        def sentRequest = ((MessageChain) resp).getHandlings()[0]

        then: "Request body sent from repose to the origin service should contain"
        ((Handling) sentRequest).request.body.contains(rssPayload)
        ((Handling) sentRequest).request.path.contains("otherparam=y")
        resp.receivedResponse.code == "200"
        !((Handling) sentRequest).request.body.contains("add-me")
        !((Handling) sentRequest).request.path.contains("testparam=x")

        and: "Request headers sent from repose to the origin service should contain"
        ((Handling) sentRequest).request.headers.getNames().contains("translation-header")
        ((Handling) sentRequest).request.headers.getNames().contains("other")
        !((Handling) sentRequest).request.headers.getNames().contains("test")

    }

    def "when attempting to translate an invalid xml/json request"() {

        when: "User passes invalid json/xml through repose"
        def resp = deproxy.makeRequest(url:(String) reposeEndpoint, method:"POST", headers:reqHeaders, requestBody:reqBody, defaultHandler:"")

        then: "Repose will send back 400s as the requests are invalid"
        resp.receivedResponse.code.equals(respCode)

        where:
        reqHeaders              | respHeaders | reqBody     | respCode
        acceptXML + contentXML  | contentXML  | invalidXml  | "400"
        acceptXML + contentJSON | contentXML  | invalidJson | "400"
    }

    def "Should not split request headers according to rfc"() {
        given:
        def userAgentValue = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_4) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.65 Safari/537.36"
        def reqHeaders =
            [
                    "user-agent": userAgentValue,
                    "x-pp-user": "usertest1, usertest2, usertest3",
                    "accept": "application/xml;q=1 , application/json;q=0.5"
            ]

        when: "User sends a request through repose"
        MessageChain mc = deproxy.makeRequest(url: reposeEndpoint + "/", method: 'GET', headers: reqHeaders)

        then:
        mc.handlings.size() == 1
        mc.handlings[0].request.getHeaders().findAll("user-agent").size() == 1
        mc.handlings[0].request.headers['user-agent'] == userAgentValue
        mc.handlings[0].request.getHeaders().findAll("x-pp-user").size() == 3
        mc.handlings[0].request.getHeaders().findAll("accept").size() == 2
    }
}
