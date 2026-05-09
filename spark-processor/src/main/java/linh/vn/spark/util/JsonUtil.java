package linh.vn.spark.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import linh.vn.spark.model.PaymentEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Slf4j
public class JsonUtil implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static PaymentEvent parsePaymentEvent(String json) {
        try {
            return objectMapper.readValue(json, PaymentEvent.class);
        } catch (Exception e) {
            log.error("[JsonUtil] Failed to parse PaymentEvent: {} | json={}", e.getMessage(), json);
            return null;
        }
    }

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("[JsonUtil] Serialization failed: {}", e.getMessage());
            return "{}";
        }
    }
}