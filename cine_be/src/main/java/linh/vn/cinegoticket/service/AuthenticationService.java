package linh.vn.cinegoticket.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import linh.vn.cinegoticket.dto.request.securequest.*;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.AuthenticationResponse;
import linh.vn.cinegoticket.dto.response.IntrospectResponse;
import linh.vn.cinegoticket.entity.User;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
public interface AuthenticationService {
    public ApiResponse signup(SignUpRequest request, String ip);

//    AuthenticationResponse authenticate(AuthenticationRequest request);

    // Định nghĩa phương thức đăng nhập
    AuthenticationResponse login(AuthenticationRequest request);

    String generateToken(User user);

    SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException;

    IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException;

    AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException;

    void logout(LogoutRequest request) throws ParseException, JOSEException;
}
