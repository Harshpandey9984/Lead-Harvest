package com.company.scraper.common.notification;

import com.company.scraper.common.config.AppProperties;
import com.company.scraper.common.dto.NotificationRequest;
import com.company.scraper.common.model.Notification;
import com.company.scraper.common.model.NotificationStatus;
import com.company.scraper.common.repository.NotificationRepository;
import com.company.scraper.common.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final MediaType JSON = MediaType.parse("application/json");

    private final JavaMailSender mailSender;
    private final OkHttpClient httpClient;
    private final NotificationRepository repository;
    private final AppProperties properties;
    private final ObjectMapper mapper;

    public NotificationService(JavaMailSender mailSender,
                               OkHttpClient httpClient,
                               NotificationRepository repository,
                               AppProperties properties,
                               ObjectMapper mapper) {
        this.mailSender = mailSender;
        this.httpClient = httpClient;
        this.repository = repository;
        this.properties = properties;
        this.mapper = mapper;
    }

    public Notification send(NotificationRequest request) {
        Notification notification = Notification.builder()
            .channel(request.channel())
            .recipient(request.recipient())
            .payload(JsonUtils.toJson(mapper, request.payload()))
            .status(NotificationStatus.PENDING)
            .createdAt(Instant.now())
            .build();
        notification = repository.save(notification);

        try {
            switch (request.channel()) {
                case EMAIL -> sendEmail(request);
                case TELEGRAM -> sendTelegram(request);
                case WEBHOOK -> sendWebhook(request);
            }
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
        }
        return repository.save(notification);
    }

    private void sendEmail(NotificationRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.recipient());
        message.setSubject("Scraper Notification");
        message.setText(JsonUtils.toJson(mapper, request.payload()));
        mailSender.send(message);
    }

    private void sendTelegram(NotificationRequest request) throws Exception {
        if (properties.getNotification().getTelegramBotToken() == null) {
            throw new IllegalStateException("Telegram bot token not configured");
        }
        String url = "https://api.telegram.org/bot" + properties.getNotification().getTelegramBotToken() + "/sendMessage";
        String body = mapper.writeValueAsString(
            java.util.Map.of(
                "chat_id", properties.getNotification().getTelegramChatId(),
                "text", JsonUtils.toJson(mapper, request.payload())
            )
        );
        Request httpRequest = new Request.Builder()
            .url(url)
            .post(RequestBody.create(body, JSON))
            .build();
        httpClient.newCall(httpRequest).execute().close();
    }

    private void sendWebhook(NotificationRequest request) throws Exception {
        if (properties.getNotification().getWebhookUrl() == null) {
            throw new IllegalStateException("Webhook URL not configured");
        }
        Request httpRequest = new Request.Builder()
            .url(properties.getNotification().getWebhookUrl())
            .post(RequestBody.create(JsonUtils.toJson(mapper, request.payload()), JSON))
            .build();
        httpClient.newCall(httpRequest).execute().close();
    }
}
