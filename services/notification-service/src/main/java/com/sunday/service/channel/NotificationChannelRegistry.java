package com.sunday.service.channel;

import com.sunday.service.model.NotificationChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Resolves a NotificationChannelProvider by NotificationChannel. Spring collects every provider bean
 *  into the injected List; adding a 4th channel is one new @Service class, nothing here changes. */
@Component
@RequiredArgsConstructor
public class NotificationChannelRegistry {

    private final List<NotificationChannelProvider> providers;

    private Map<NotificationChannel, NotificationChannelProvider> byChannel;

    @PostConstruct
    void index() {
        byChannel = providers.stream().collect(Collectors.toMap(NotificationChannelProvider::channelType, p -> p));
    }

    public NotificationChannelProvider resolve(NotificationChannel channel) {
        NotificationChannelProvider provider = byChannel.get(channel);
        if (provider == null) {
            throw new IllegalStateException("No provider configured for notification channel " + channel);
        }
        return provider;
    }
}
