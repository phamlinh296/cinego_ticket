package linh.vn.cinegoticket.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateRequest {
    private String username;
    private String password;
    private String fullName;
    private String address;
    private String email;


    private String phone;

}
