package linh.vn.cinegoticket.repository;

import linh.vn.cinegoticket.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    @Query(value = "SELECT p FROM Payment p WHERE p.booking.user.id = :user_id")
    public List<Payment> findAllByUserId(@Param("user_id") String user_id);

    @Query(value = "SELECT p FROM Payment p WHERE p.booking.id = :booking_id")
    public List<Payment> findAllByBookingId(@Param("booking_id") String booking_id);
}
