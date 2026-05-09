package linh.vn.cinegoticket.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {
    String name;
    String description;

    //    Set<PermissionResponse> permissions;
    public RoleResponse(String name) {
        this.name = name;
    }
}
