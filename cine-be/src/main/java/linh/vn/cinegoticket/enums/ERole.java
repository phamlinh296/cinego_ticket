package linh.vn.cinegoticket.enums;

import java.util.Collections;
import java.util.Set;

import static linh.vn.cinegoticket.enums.Permission.*;

public enum ERole {
    SUPER_ADMIN(
            Set.of(
                    ADMIN_READ,
                    ADMIN_UPDATE,
                    ADMIN_DELETE,
                    ADMIN_CREATE,
                    MANAGER_READ,
                    MANAGER_UPDATE,
                    MANAGER_DELETE,
                    MANAGER_CREATE
            )
    ),
    ADMIN(
            Set.of(
                    ADMIN_READ,
                    ADMIN_UPDATE,
                    ADMIN_DELETE,
                    ADMIN_CREATE,
                    MANAGER_READ,
                    MANAGER_UPDATE,
                    MANAGER_DELETE,
                    MANAGER_CREATE
            )
    ),
    USER(Collections.emptySet());//k co quyền nào
    private final Set<Permission> permissions;

    ERole(Set<Permission> permission) {
        this.permissions = permission;
    }

    public Set<Permission> getPermissions() {
        return this.permissions;
    }

}
