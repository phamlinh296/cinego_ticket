package linh.vn.cinegoticket.controller;

import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import linh.vn.cinegoticket.dto.request.securequest.*;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.AuthenticationResponse;
import linh.vn.cinegoticket.dto.response.IntrospectResponse;
import linh.vn.cinegoticket.entity.EmailVerificationToken;
import linh.vn.cinegoticket.entity.User;
import linh.vn.cinegoticket.enums.UserStatus;
import linh.vn.cinegoticket.repository.EmailVerificationTokenRepository;
import linh.vn.cinegoticket.repository.UserRepository;
import linh.vn.cinegoticket.service.AuthenticationService;
import linh.vn.cinegoticket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor //tự động autowired các bean final
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;


    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam("token") String token) {

        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ"));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Token đã hết hạn");
        }

        User user = verificationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        return ResponseEntity.ok("Xác nhận thành công! Tài khoản đã được kích hoạt.");
    }

    @PostMapping("/signup")
    public ApiResponse signup(@RequestBody SignUpRequest request,
                              HttpServletRequest servletRequest) {
        return authenticationService.signup(request, servletRequest.getRemoteAddr());
    }
//    @PostMapping("/signup")
//    public ResponseEntity<?> registerUser(@RequestBody UserCreateRequest request) {
//        userService.createUser(request);
//        return ResponseEntity.ok(new ApiResponse<>("User  registered successfully"));
////        return ResponseEntity.ok("User  registered successfully");//bđ trả ve ntn n k ra json
//    }

    @GetMapping("/token")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse> getMe() {
        return ResponseEntity.ok(new ApiResponse("ok"));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        var response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    //2. check token có valid ko
    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);//đtg authenresponse
        return ApiResponse.<IntrospectResponse>builder()
                .data(result)
                .build();

    }

    //3. logout token
    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }

    //4. refresh token
    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshRequest request) throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .data(result)
                .build();
    }
}
