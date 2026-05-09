package linh.vn.cinegoticket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling //bật scheduler cho toàn app, thì @Scheduled mới hoạt động được
public class SchedulerConfig {
}