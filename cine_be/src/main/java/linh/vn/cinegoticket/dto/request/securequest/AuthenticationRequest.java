package linh.vn.cinegoticket.dto.request.securequest;

import lombok.*;
import lombok.experimental.FieldDefaults;

//login request
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {
    String username;
    String password;
}

