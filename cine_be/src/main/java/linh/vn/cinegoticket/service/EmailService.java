package linh.vn.cinegoticket.service;

import org.springframework.stereotype.Service;

@Service
public interface EmailService {
    void sendMail(String toMail, String subject, String body);
}
