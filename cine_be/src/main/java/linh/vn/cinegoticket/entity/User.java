package linh.vn.cinegoticket.entity;

import jakarta.persistence.*;
import linh.vn.cinegoticket.enums.UserStatus;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String username;
    private String password;
    private String fullName;
    private String address;
    private String email;


    private String phone;
//    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @ManyToMany
    private Set<Role> roles;


}
