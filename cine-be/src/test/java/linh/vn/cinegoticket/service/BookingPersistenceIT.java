package linh.vn.cinegoticket.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import linh.vn.cinegoticket.dto.request.BookingRequest;
import linh.vn.cinegoticket.dto.response.BookingResponse;
import linh.vn.cinegoticket.entity.*;
import linh.vn.cinegoticket.enums.ESeat;
import linh.vn.cinegoticket.enums.ESeatStatus;
import linh.vn.cinegoticket.enums.UserStatus;
import linh.vn.cinegoticket.repository.*;
import linh.vn.cinegoticket.service.impl.BookingServiceImpl;
import linh.vn.cinegoticket.service.impl.VNPayService;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Real service and JPA repositories against an isolated MySQL database.
 * No transaction wraps a service call: doing that here would hide missing
 * production transaction boundaries. External systems are not started.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = BookingPersistenceIT.PersistenceConfig.class)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
class BookingPersistenceIT {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("cinego_booking_test")
            .withUsername("cinego_test")
            .withPassword("disposable-test-password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private BookingService service;
    @Autowired
    private ShowSeatRepository seats;
    @Autowired
    private BookingRepository bookings;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @PersistenceContext
    private EntityManager entityManager;

    private Fixture fixture;

    @BeforeEach
    void seedCommittedFixture() {
        fixture = new TransactionTemplate(transactionManager).execute(status -> {
            String suffix = UUID.randomUUID().toString();
            User first = user("first-" + suffix);
            User second = user("second-" + suffix);
            Movie movie = new Movie();
            movie.setTitle("CI booking fixture " + suffix);
            movie.setDurationInMins(120);
            entityManager.persist(movie);
            CinemaHall hall = new CinemaHall("CI-" + suffix, 1, 2);
            entityManager.persist(hall);
            LocalDateTime start = LocalDateTime.of(2030, 1, 1, 12, 0);
            CinemaShow show = new CinemaShow(hall, movie, start, start.plusHours(2));
            entityManager.persist(show);
            CinemaSeat firstSeat = new CinemaSeat(hall, 1, 1, ESeat.REGULAR);
            CinemaSeat secondSeat = new CinemaSeat(hall, 1, 2, ESeat.REGULAR);
            entityManager.persist(firstSeat);
            entityManager.persist(secondSeat);
            ShowSeat firstShowSeat = new ShowSeat(show, firstSeat, ESeatStatus.AVAILABLE);
            ShowSeat secondShowSeat = new ShowSeat(show, secondSeat, ESeatStatus.AVAILABLE);
            entityManager.persist(firstShowSeat);
            entityManager.persist(secondShowSeat);
            entityManager.flush();
            return new Fixture(show.getId(), List.of(firstShowSeat.getId(), secondShowSeat.getId()),
                    first.getUsername(), second.getUsername());
        });
    }

    private User user(String username) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("CI test user");
        user.setStatus(UserStatus.ACTIVE);
        entityManager.persist(user);
        return user;
    }

    @AfterEach
    void restoreRepositoryDelegates() {
        ReflectionTestUtils.setField(service, "showSeatREPO", seats);
        ReflectionTestUtils.setField(service, "bookingREPO", bookings);
    }

    @Test
    void successfulBookingCommitsPendingSeatAndBooking() {
        BookingResponse response = service.createBooking(fixture.firstUser(), request(fixture.seatIds()));

        assertNotNull(response.getId());
        assertEquals("PENDING", response.getStatus());
        assertEquals(1, bookings.countByShowId(fixture.showId()));
        for (String seatId : fixture.seatIds()) {
            assertEquals(ESeatStatus.PENDING, seats.findById(seatId).orElseThrow().getStatus());
        }
        assertEquals(2, bookings.findById(response.getId()).orElseThrow().getSeats().size());
    }

    @Test
    void sequentialSecondBookingCannotClaimPendingSeat() {
        String seatId = fixture.seatIds().getFirst();
        service.createBooking(fixture.firstUser(), request(List.of(seatId)));

        RuntimeException rejection = assertThrows(RuntimeException.class,
                () -> service.createBooking(fixture.secondUser(), request(List.of(seatId))));

        assertEquals("Seat ID " + seatId + " is reserved", rejection.getMessage());
        assertEquals(1, bookings.countByShowId(fixture.showId()));
        assertEquals(ESeatStatus.PENDING, seats.findById(seatId).orElseThrow().getStatus());
    }

    @Test
    void bookingSaveFailureRollsBackEverySeatChange() {
        // Inject a failure at the booking write boundary, after real MySQL seat
        // writes. State assertions query committed rows through real repositories.
        ProxyFactory failingBookings = new ProxyFactory(bookings);
        failingBookings.addAdvice((MethodInterceptor) invocation -> {
            if (invocation.getMethod().getName().equals("save")) {
                throw new BookingWriteFailure();
            }
            return invocation.proceed();
        });
        ReflectionTestUtils.setField(service, "bookingREPO", failingBookings.getProxy());

        assertThrows(BookingWriteFailure.class,
                () -> service.createBooking(fixture.firstUser(), request(fixture.seatIds())));

        assertAll("A rejected booking must not leave committed seat reservations",
                () -> assertEquals(0, bookings.countByShowId(fixture.showId())),
                () -> assertEquals(ESeatStatus.AVAILABLE,
                        seats.findById(fixture.seatIds().get(0)).orElseThrow().getStatus()),
                () -> assertEquals(ESeatStatus.AVAILABLE,
                        seats.findById(fixture.seatIds().get(1)).orElseThrow().getStatus()));
    }

    @Test
    void concurrentRequestsForSameSeatAllowOnlyOneCommittedBooking() throws Exception {
        String seatId = fixture.seatIds().getFirst();
        CountDownLatch readers = new CountDownLatch(2);
        ProxyFactory observedSeats = new ProxyFactory(seats);
        observedSeats.addAdvice((MethodInterceptor) invocation -> {
            Object result = invocation.proceed();
            if (invocation.getMethod().getName().equals("findByIdAndShowId")
                    && result instanceof Optional<?> selected
                    && selected.orElse(null) instanceof ShowSeat seat
                    && seat.getId().equals(seatId) && seat.getStatus() == ESeatStatus.AVAILABLE) {
                // This observer is OUTSIDE the real repository transaction proxy.
                // Widen the read/write window without replacing the database lock.
                // With a service transaction the first worker retains its lock,
                // so this bounded wait expires before the second reader proceeds.
                readers.countDown();
                readers.await(2, TimeUnit.SECONDS);
            }
            return result;
        });
        ReflectionTestUtils.setField(service, "showSeatREPO", observedSeats.getProxy());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> attempt(fixture.firstUser(), seatId, ready, start));
            var second = executor.submit(() -> attempt(fixture.secondUser(), seatId, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Both booking workers must start");
            start.countDown();
            List<Attempt> outcomes = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));

            assertAll("Only one user may own the same show seat",
                    () -> assertEquals(1, outcomes.stream().filter(a -> a.bookingId() != null).count(),
                            () -> "Booking outcomes: " + outcomes),
                    () -> assertEquals(1, outcomes.stream()
                            .filter(a -> ("java.lang.RuntimeException: Seat ID " + seatId + " is reserved")
                                    .equals(a.failure())).count(),
                            () -> "The other request must be rejected as reserved: " + outcomes),
                    () -> assertEquals(1, bookings.countByShowId(fixture.showId())),
                    () -> assertEquals(ESeatStatus.PENDING, seats.findById(seatId).orElseThrow().getStatus()));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Booking workers must terminate");
        }
    }

    private Attempt attempt(String username, String seatId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start booking workers");
        }
        try {
            return new Attempt(service.createBooking(username, request(List.of(seatId))).getId(), null);
        } catch (RuntimeException failure) {
            return new Attempt(null, failure.toString());
        }
    }

    private BookingRequest request(List<String> seatIds) {
        BookingRequest request = new BookingRequest();
        request.setShowId(fixture.showId());
        request.setSeatsId(seatIds);
        return request;
    }

    private record Fixture(String showId, List<String> seatIds, String firstUser, String secondUser) { }
    private record Attempt(String bookingId, String failure) { }
    private static class BookingWriteFailure extends RuntimeException { }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
            TransactionAutoConfiguration.class})
    @EntityScan(basePackageClasses = Booking.class)
    @EnableJpaRepositories(basePackageClasses = BookingRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {BookingRepository.class, ShowSeatRepository.class,
                            UserRepository.class, CinemaShowRepository.class}))
    static class PersistenceConfig {
        @Bean BookingServiceImpl bookingService() { return new BookingServiceImpl(); }
        @Bean UserService userService() { return mock(UserService.class); }
        @Bean PaymentService paymentService() { return mock(PaymentService.class); }
        @Bean PaymentRepository paymentRepository() { return mock(PaymentRepository.class); }
        @Bean SpamUserRepository spamUserRepository() { return mock(SpamUserRepository.class); }
        @Bean VNPayService vnPayService() { return mock(VNPayService.class); }
    }
}
