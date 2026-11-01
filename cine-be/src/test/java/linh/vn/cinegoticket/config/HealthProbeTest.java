package linh.vn.cinegoticket.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicReference;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = HealthProbeTest.ProbeApplication.class,
        properties = "spring.config.location=classpath:application-health.yaml")
@AutoConfigureMockMvc
class HealthProbeTest {
    @Autowired private MockMvc mvc;
    @Autowired private ApplicationContext context;
    @MockBean private CustomJwtDecoder decoder;
    @Autowired @Qualifier("dbHealthIndicator") private ProbeHealth database;
    @Autowired @Qualifier("redisHealthIndicator") private ProbeHealth redis;

    @BeforeEach
    void healthyDependencies() {
        database.value.set(Health.up().build());
        redis.value.set(Health.up().build());
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
    }

    @Test
    void readinessIsPublicButDoesNotExposeDependencyDetails() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void databaseFailureMakesReadinessUnavailable() throws Exception {
        database.value.set(Health.down().withDetail("private", "must-not-leak").build());
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("must-not-leak"))));
    }

    @Test
    void redisFailureMakesReadinessUnavailable() throws Exception {
        redis.value.set(Health.down().build());
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void refusingTrafficMakesReadinessUnavailable() throws Exception {
        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void otherActuatorPathsAndWriteMethodsRemainProtected() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
        mvc.perform(post("/actuator/health/readiness")).andExpect(status().isUnauthorized());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class, KafkaAutoConfiguration.class,
            MailSenderAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
    @Import(SecurityConfig.class)
    static class ProbeApplication {
        @Bean ProbeHealth dbHealthIndicator() { return new ProbeHealth(); }
        @Bean ProbeHealth redisHealthIndicator() { return new ProbeHealth(); }
    }

    // Real HealthIndicator default methods are used by Actuator's aggregator.
    // Only the dependency's state is controlled, not the health HTTP endpoint.
    static class ProbeHealth implements HealthIndicator {
        final AtomicReference<Health> value = new AtomicReference<>(Health.up().build());
        @Override public Health health() { return value.get(); }
    }
}
