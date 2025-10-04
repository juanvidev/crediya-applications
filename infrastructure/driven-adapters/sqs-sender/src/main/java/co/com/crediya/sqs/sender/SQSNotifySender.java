package co.com.crediya.sqs.sender;

import co.com.crediya.model.applicationclient.ApplicationClient;
import co.com.crediya.model.notification.gateways.NotificationGateway;
import co.com.crediya.sqs.sender.config.SQSSender;
import co.com.crediya.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SQSNotifySender implements NotificationGateway {
    private final SQSSenderProperties properties;
    private final SQSSender sqsSender;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> send(ApplicationClient applicationClient) {
        System.out.println("Sending notification to SQS for ApplicationClient: " + applicationClient);
        String applicationClientStringify;
        try {
            applicationClientStringify = objectMapper.writeValueAsString(applicationClient.getLoanApplicationCreator());
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error serializing ApplicationClient", e));
        }

        return sqsSender.send(applicationClientStringify, properties.queueUrl())
                .doOnNext(messageId -> System.out.println("Message sent to SQS with ID: " + messageId))
                .then()
                .onErrorMap(e -> new RuntimeException("Error enviando mensaje a SQS", e));    }
}
