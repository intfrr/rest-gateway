package com.example.restgateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.HeaderEnricherSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@EnableBinding({RestGatewayApplication.GatewayChannels.class})
@SpringBootApplication
public class RestGatewayApplication {

  interface GatewayChannels {

    String TO_UPPERCASE_REPLY = "to-uppercase-reply";
    String TO_UPPERCASE_REQUEST = "to-uppercase-request";

    @Input(TO_UPPERCASE_REPLY)
    SubscribableChannel toUppercaseReply();

    @Output(TO_UPPERCASE_REQUEST)
    MessageChannel toUppercaseRequest();
  }

  @MessagingGateway
  public interface StreamGateway {
    @Gateway(requestChannel = ENRICH, replyChannel = GatewayChannels.TO_UPPERCASE_REPLY)
    String process(String payload);
  }

  private static final String ENRICH = "enrich";

  public static void main(String[] args) {
    SpringApplication.run(RestGatewayApplication.class, args);
  }

  @Bean
  public IntegrationFlow headerEnricherFlow() {
    return IntegrationFlows.from(ENRICH).enrichHeaders(HeaderEnricherSpec::headerChannelsToString)
        .channel(GatewayChannels.TO_UPPERCASE_REQUEST).get();
  }

  @RestController
  public class UppercaseController {
    @Autowired
    StreamGateway gateway;

    @GetMapping(value = "/string/{string}",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<String> getUser(@PathVariable("string") String string) {
      return new ResponseEntity<String>(gateway.process(string), HttpStatus.OK);
    }
  }
  
 
  @StreamListener(GatewayChannels.TO_UPPERCASE_REQUEST)
  @SendTo(GatewayChannels.TO_UPPERCASE_REPLY)
  public Message<?> process(Message<String> request) {
    return MessageBuilder.withPayload(request.getPayload().toUpperCase())
        .copyHeaders(request.getHeaders()).build();
  }

}