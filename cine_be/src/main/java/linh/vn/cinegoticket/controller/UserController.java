package linh.vn.cinegoticket.controller;

import linh.vn.cinegoticket.dto.request.UserUpdateRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.UserResponse;
import linh.vn.cinegoticket.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/user")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class UserController {
    @Autowired
    private UserService userService;

    //get user by username
    @GetMapping("/{username}")
    ApiResponse<UserResponse> getUser(@PathVariable("username") String username) {
        return ApiResponse.<UserResponse>builder()
                .data(userService.getUser(username))
                .build();
    }

    @GetMapping("/getall")
    @PreAuthorize("hasRole('ADMIN')")
        //ROLE_ADMIN
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .data(userService.getAllUsers())
                .build();
    }

    @GetMapping("/myInfo")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .data(userService.getMyInfo())
                .build();
    }

    //update user theo username (ban đu theo user id ban đầu_
    @PutMapping("/{userName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUser(@PathVariable String userName, @RequestBody UserUpdateRequest request) {
        log.info("updateeeeeeee");
        return ApiResponse.<UserResponse>builder()
                .data(userService.updateUser(userName, request))
                .build();
    }

    //delete user
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
//
//    //get  role
//    @GetMapping("/roles")
//    @GetMapping("/forgotpassword")
//    @GetMapping("/recovery")
//    @PostMapping("/recovery")
}
