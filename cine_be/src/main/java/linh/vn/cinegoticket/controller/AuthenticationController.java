package linh.vn.cinegoticket.controller;

import com.nimbusds.jose.JOSEException;
import linh.vn.cinegoticket.dto.request.UserCreateRequest;
import linh.vn.cinegoticket.dto.request.securequest.AuthenticationRequest;
import linh.vn.cinegoticket.dto.request.securequest.IntrospectRequest;
import linh.vn.cinegoticket.dto.request.securequest.LogoutRequest;
import linh.vn.cinegoticket.dto.request.securequest.RefreshRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.AuthenticationResponse;
import linh.vn.cinegoticket.dto.response.IntrospectResponse;
import linh.vn.cinegoticket.service.AuthenticationService;
import linh.vn.cinegoticket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor //tự động autowired các bean final
public class AuthenticationController {
    @Autowired
    private final AuthenticationService authenticationService;
    @Autowired
    private final UserService userService;


    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return ResponseEntity.ok(new ApiResponse<>("User  registered successfully"));
//        return ResponseEntity.ok("User  registered successfully");//bđ trả ve ntn n k ra json
    }

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
