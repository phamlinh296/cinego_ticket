package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.request.UserCreateRequest;
import linh.vn.cinegoticket.dto.request.UserUpdateRequest;
import linh.vn.cinegoticket.dto.response.UserResponse;
import linh.vn.cinegoticket.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    //    public interface UserService extends UserDetailsService {
    List<UserResponse> getAllUsers();

    UserResponse getUser(String id);

    UserResponse getMyInfo();

    User createUser(UserCreateRequest request);

    UserResponse updateUser(String username, UserUpdateRequest request);

    void deleteUser(String username);
}
