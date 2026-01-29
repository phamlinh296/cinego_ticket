package linh.vn.cinegoticket.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import linh.vn.cinegoticket.dto.request.UserUpdateRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.UserResponse;
import linh.vn.cinegoticket.entity.Role;
import linh.vn.cinegoticket.entity.User;
import linh.vn.cinegoticket.enums.ERole;
import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.repository.RoleRepository;
import linh.vn.cinegoticket.repository.UserRepository;
import linh.vn.cinegoticket.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collection;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/user")
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Tag(name="1. User Endpoint")
public class UserController {
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    //get user by username
    @GetMapping("/{username}")
    @Operation(summary = "Lấy thông tin người dùng theo username")
    @Parameter(name = "username", description = "Tên đăng nhập của người dùng")
    ApiResponse<UserResponse> getUser(@PathVariable("username") String username) {
        return ApiResponse.<UserResponse>builder()
                .data(userService.getUser(username))
                .build();
    }

    @Operation(summary = "Lấy danh sách tất cả người dùng (chỉ ADMIN)")
    @GetMapping("/getall")
    @PreAuthorize("hasRole('ADMIN')") //ROLE_ADMIN
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .data(userService.getAllUsers())
                .build();
    }

    @Operation(summary = "Lấy thông tin cá nhân của người dùng hiện tại (đã đăng nhập)")
    @GetMapping("/myInfo")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .data(userService.getMyInfo())
                .build();
    }

    //update user theo username
    @Operation(summary = "Cập nhật thông tin người dùng theo username (chỉ ADMIN)")//mô tả tổng quan mỗi API
    @Parameter(name = "userName", description = "Tên đăng nhập của người dùng cần cập nhật")//mô tả tham số
    @io.swagger.v3.oas.annotations.parameters.RequestBody(//mô tả dữ liệu đầu vào
            description = "Thông tin cập nhật cho người dùng"
    )
    @PutMapping("/{userName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUser(@PathVariable String userName, @RequestBody UserUpdateRequest request) {
        log.info("updateeeeeeee");
        return ApiResponse.<UserResponse>builder()
                .data(userService.updateUser(userName, request))
                .build();
    }

    //delete user
    @Operation(summary = "Xóa người dùng theo username (chỉ ADMIN)")
    @Parameter(name = "username", description = "Tên đăng nhập của người dùng cần xóa")
    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    //Thêm @Transactional vào service gọi deleteByUsername, vì Spring không tự mở giao dịch khi gọi deleteByUsername()  khi gọi từ một lớp khác. nếu không có @Transactional.
    public ApiResponse deleteUser(@PathVariable("username") String username) {
        userService.deleteUser(username);
        return new ApiResponse("Removed user `" + username + "`");
    }

    //OTHER FUNCTIONS
//    @GetMapping("/search")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<?> searchUserByUsername(@RequestParam String username) {
//        return ResponseEntity.ok(userSER.searchByName(username));
//    }
//
//    //Add the Admin role to the user.
//    @GetMapping("/giveadmin")
//
//    //Remove the "Admin" role from the user.
//    @GetMapping("/removeadmin")
//    @GetMapping("/giveadmin")
//    @PreAuthorize("hasRole('SUPER_ADMIN')")
//    public ApiResponse giveAdmin(@RequestParam String username) {
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        Role role = roleRepository.findByName(erole.name())
//                .orElseThrow(() -> new RuntimeException("Role not found"));
//
//        user.getRoles().add(role);   // ✅ thêm role
//        userRepository.save(user);
//    }
//
//
//    @GetMapping("/removeadmin")
//    @PreAuthorize("hasRole('SUPER_ADMIN')")
//    public ApiResponse removeAdmin(@RequestParam String username) {
//        if (userService.UsernameIsExisted(username)) {
//            if (!userService.userHaveRole(username, ERole.ADMIN))
//                throw new AppException("User do not have this role");
//
//            userService.removeRoleUser(username, ERole.ADMIN);
//            return new ApiResponse("Reomved role Admin for user `" + username + "`");
//        }
//        throw new AppException("User not found");
//    }

//    //get  role
//    @GetMapping("/roles")
//    @GetMapping("/forgotpassword")
//    @GetMapping("/recovery")
//    @PostMapping("/recovery")

    @GetMapping("/roles")
//    @PreAuthorize("hasRole('USER')")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Collection<Role>> getRoles(Principal principal) {
        return ResponseEntity.ok(userService.getRoleFromUser(principal.getName()));
    }


//    @GetMapping("/forgotpassword")
//    @Operation(
//            summary = "Get an URL to recover user's password",
//            responses = {
//                    @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "200", description = "A recoveried code will be sent to user's mail.",
//                            content = @Content( mediaType = "application/json", schema = @Schema(implementation = MyApiResponse.class))),
//                    @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "404", description = "User not found.",
//                            content = @Content( mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
//            }
//    )
//    public ResponseEntity<MyApiResponse> forgotPassword(@RequestParam @Valid String username) throws Exception {
//        return ResponseEntity.ok(userSER.getURIforgetPassword(username));
//
//    }
//
//
//    @GetMapping("/recovery")
//    @Operation(
//            summary = "This is an URL will be sent to user's mail. This method will verify for resetting password.",
//            responses = {
//                    @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "200", description = "A recoveried code will be sent to user's mail.",
//                            content = @Content( mediaType = "application/json", schema = @Schema(implementation = MyApiResponse.class))),
//                    @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "404", description = "Email not found.",
//                            content = @Content( mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
//            }
//    )
//    public ResponseEntity<MyApiResponse> checkCode(@RequestParam @Valid String code) {
//        return ResponseEntity.ok(userSER.checkReocveryCode(code.replace(' ', '+')));
//    }
//
//
//    @PostMapping("/recovery")
//    @Operation(
//            summary = "This method allows create new password with code",
//            responses = {
//                    @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "200", description = "Set a new password succesfully.",
//                            content = @Content( mediaType = "application/json", schema = @Schema(implementation = MyApiResponse.class))),
//                    @io.swagger.v3.oas.annotations.responses.ApiResponse( responseCode = "404", description = "This code is expired or invalid.",
//                            content = @Content( mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
//            }
//    )
//    public ResponseEntity<MyApiResponse> setPassword(@RequestParam @Valid String code, @RequestBody @Valid NewPasswordRequest password) {
//        return ResponseEntity.ok(userSER.setNewPassword(code.replace(' ', '+'), password.getPassword()));
//    }
}
