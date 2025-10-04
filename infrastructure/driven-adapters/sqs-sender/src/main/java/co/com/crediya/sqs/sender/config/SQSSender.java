package co.com.crediya.sqs.sender.config;

import co.com.crediya.model.notification.gateways.NotificationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSender {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;

    public Mono<String> send(String message, String urlQueue) {
        System.out.println("Sending message to SQS: " + message);
        return Mono.fromCallable(() -> buildRequest(message, urlQueue))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message, String urlQueue) {
        return SendMessageRequest.builder()
                .queueUrl(urlQueue != null ? urlQueue : properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
