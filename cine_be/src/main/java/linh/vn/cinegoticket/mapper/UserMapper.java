package linh.vn.cinegoticket.mapper;

import linh.vn.cinegoticket.dto.request.UserCreateRequest;
import linh.vn.cinegoticket.dto.response.RoleResponse;
import linh.vn.cinegoticket.dto.response.UserResponse;
import linh.vn.cinegoticket.entity.User;
import linh.vn.cinegoticket.enums.UserStatus;
import linh.vn.cinegoticket.service.impl.RoleService;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;


@Component
public class UserMapper {

    private final RoleService roleService;

    public UserMapper(RoleService roleService) {
        this.roleService = roleService;
    }

    //userRequest > user
    public User toUser(UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setStatus(UserStatus.ACTIVE);//tạo mới user kích hoạt active

        //set role sau
        //Set<String> roles (request) sang Set<Role> roles (user)
//        Set<Role> roles = request.getRoles().stream()
//                .map(roleName -> roleService.getRoleByName(roleName)
//                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
//                .collect(Collectors.toSet());
//        user.setRoles(roles);
        return user;
    }

    //user > reponse
    public UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        

        //Set<Role> roles sang Set<RoleResponse> roles;
        Set<RoleResponse> roles = user.getRoles().stream()
                .map(role -> new RoleResponse(role.getName(), role.getDescription()))
                .collect(Collectors.toSet());
        response.setRoles(roles);
        return response;
    }


}
