package linh.vn.cinegoticket.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "SpamUser")
public class SpamUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, length = 26, insertable = false)
    private String id;

    @OneToOne
    @NotNull
    private User user;

    @NotNull
    @Column(name = "spamTimes")
    private int spamTimes;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = true, updatable = true)
    private Date updateAt;

    public SpamUser() {
    }

    public SpamUser(User user) {
        this.user = user;
        this.spamTimes = 1;
    }

    public int increase() {
        this.spamTimes += 1;
        return this.spamTimes;
    }
}




