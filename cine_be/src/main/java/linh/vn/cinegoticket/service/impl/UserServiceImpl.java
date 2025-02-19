package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.request.UserCreateRequest;
import linh.vn.cinegoticket.dto.request.UserUpdateRequest;
import linh.vn.cinegoticket.dto.response.UserResponse;
import linh.vn.cinegoticket.entity.Role;
import linh.vn.cinegoticket.entity.User;
import linh.vn.cinegoticket.enums.ERole;
import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.exception.ErrorCode;
import linh.vn.cinegoticket.mapper.UserMapper;
import linh.vn.cinegoticket.mapper.UserMapperInterface;
import linh.vn.cinegoticket.repository.RoleRepository;
import linh.vn.cinegoticket.repository.UserRepository;
import linh.vn.cinegoticket.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserMapperInterface userMapperInterface;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleRepository roleRepository;

    // CRUD
    @Override
    public UserResponse getUser(String username) {
        // Optional<User> user = userRepository.findById(id);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toUserResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(userMapper::toUserResponse).collect(Collectors.toList());
    }

    @Override
    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    @Override
    // có vào controller đâu?, hnhu bị để t mô tả đã, hnhu bị fe chỗ token sao ấy,
    // nma k vào đc controller sao n ném lõi 1002, đấy là lỗi
    // t định nghĩa trong be 1002 nào,lúc đăng ký, code 10002 là lỗi t định nghĩa
    // tỏng errorcode, nếu exist thì 1002,
    // nma t comment nó đi rồi mà khs n vẫn trả ra 1002
    public User createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.EXISTED);
        }
        User user = userMapper.toUser(request);
        Role userRole = roleRepository.findByName(String.valueOf(ERole.USER))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole); // Mặc định ROLE_USER
        user.setRoles(roles);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        return userRepository.save(user);
    }

    @Override
    public UserResponse updateUser(String username, UserUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userMapperInterface.updateUser(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // update role
        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> roleService.getRoleByName(roleName)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());
        user.setRoles(roles);

        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public void deleteUser(String username) {
        userRepository.deleteByUsername(username);
    }

}
