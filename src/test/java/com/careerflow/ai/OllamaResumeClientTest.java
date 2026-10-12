package com.careerflow.ai;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class OllamaResumeClientTest {
    HttpServer server;
    ValidatorFactory factory;
    ObjectMapper mapper = new ObjectMapper();
    int responseCode=200;
    long delayMillis=0;
    String response;
    AtomicReference<String> sent = new AtomicReference<>();
    OllamaResumeClient client;
    @BeforeEach void setup() throws Exception {
        factory=Validation.buildDefaultValidatorFactory();
        response=envelope(Map.of("summary","Java developer", "skills",List.of("Java"), "strengths",List.of(), "improvements",List.of("Add results")));
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/chat",exchange->{
            if(delayMillis>0) try { Thread.sleep(delayMillis); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
            sent.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes=response.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(responseCode,bytes.length);
            try(var out=exchange.getResponseBody()){out.write(bytes);}
        });server.start();
        client=new OllamaResumeClient(mapper,factory.getValidator(),"http://127.0.0.1:"+server.getAddress().getPort(),"local-model",2);
    }
    String envelope(Object feedback) throws Exception {
        return mapper.writeValueAsString(Map.of("done",true,"done_reason","stop","message",Map.of("content",mapper.writeValueAsString(feedback))));
    }
    @AfterEach void close(){server.stop(0);factory.close();}
    @Test void sendsStructuredRequestAndReadsValidatedFeedback() throws Exception {
        assertThat(client.analyze("Java resume").skills()).containsExactly("Java");
        var request=mapper.readTree(sent.get());
        assertThat(request.path("stream").asBoolean()).isFalse();
        assertThat(request.path("format").path("required").size()).isEqualTo(4);
        assertThat(request.path("messages").get(1).path("content").asText()).isEqualTo("Java resume");
        assertThat(request.path("messages").get(0).path("content").asText()).contains("untrusted");
    }
    @Test void mapsMissingModelAndServerErrors() {
        responseCode=404;
        assertThatThrownBy(()->client.analyze("text")).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(503));
        responseCode=500;
        assertThatThrownBy(()->client.analyze("text")).hasMessageContaining("could not complete");
    }
    @Test void rejectsMalformedTruncatedAndUnexpectedFeedback() throws Exception {
        for(String invalid:List.of("not json", "{}", "{\"done\":false}", envelope(Map.of("summary","only field")),
                envelope(Map.of("summary","", "skills",List.of(), "strengths",List.of(), "improvements",List.of())))) {
            response=invalid;
            assertThatThrownBy(()->client.analyze("text")).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(502));
        }
    }
    @Test void rejectsNonLocalConfiguration() {
        assertThatThrownBy(()->new OllamaResumeClient(mapper,factory.getValidator(),"https://example.com","model",2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new OllamaResumeClient(mapper,factory.getValidator(),"http://127.0.0.1:11434","model:cloud",2)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void timeoutReturns504() {
        delayMillis=1800;
        client=new OllamaResumeClient(mapper,factory.getValidator(),"http://127.0.0.1:"+server.getAddress().getPort(),"local-model",1);
        assertThatThrownBy(()->client.analyze("text")).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(504));
    }
    @Test void unavailableServiceReturns503() {
        server.stop(0);
        assertThatThrownBy(()->client.analyze("text")).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(503));
    }
}
