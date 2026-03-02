package linh.vn.cinegoticket.service;

public interface RateLimitService {
    void checkLoginRateLimit(String key);
}
