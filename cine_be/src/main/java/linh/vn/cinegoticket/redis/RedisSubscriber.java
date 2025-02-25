package linh.vn.cinegoticket.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import linh.vn.cinegoticket.dto.response.PaymentResponse;
import linh.vn.cinegoticket.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class RedisSubscriber implements MessageListener {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    public RedisSubscriber(EmailService emailService, ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        System.out.println("🚀 RedisSubscriber is ready!");
    }


    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // Chuyển byte[] thành String JSON
            String jsonMessage = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("RedisSubscriber 📩 Received message from Redis: " + jsonMessage);

            // Deserialize json về kiểu PaymentResponse
            PaymentResponse data = objectMapper.readValue(jsonMessage, PaymentResponse.class);
            log.info("RedisSubscriber 🎬 Payment details: " + data);
            log.info("📌 DEBUG TicketDetail: " + objectMapper.writeValueAsString(data.getDetail()));


            // Tạo nội dung email
            String info = "Payment ID " + data.getId() + "\n" +
                    "Total amount: " + data.getPrice() + "\n" +
                    "Create at: " + data.getCreateOn() + "\n" +
                    "Movie name: " + data.getDetail().getMovieName() + "\n" +
                    "Hall name: " + data.getDetail().getHallName() + "\n" +
                    "Start time: " + data.getDetail().getStartTime() + "\n" +
                    "Seats: " + String.join(", ", data.getDetail().getSeats());

            // Gửi email
            String subject = "Movie Project: Payment Information";
            emailService.sendMail(data.getEmail(), subject, info);

            log.info("RedisSubscriber ✅ Email sent to: " + data.getEmail());

        } catch (Exception e) {
            log.info("RedisSubscriber ❌ Error: " + e.getMessage());
        }
    }
}


