package co.com.crediya.model.notification.gateways;

import co.com.crediya.model.applicationclient.ApplicationClient;
import reactor.core.publisher.Mono;

public interface NotificationGateway {
    Mono<Void> send(ApplicationClient applicationClient);
}
