package linh.vn.cinegoticket.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import linh.vn.cinegoticket.dto.request.securequest.*;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.AuthenticationResponse;
import linh.vn.cinegoticket.dto.response.IntrospectResponse;
import linh.vn.cinegoticket.entity.EmailVerificationToken;
import linh.vn.cinegoticket.entity.InvalidatedToken;
import linh.vn.cinegoticket.entity.User;
import linh.vn.cinegoticket.enums.UserStatus;
import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.exception.ErrorCode;
import linh.vn.cinegoticket.mapper.UserMapper;
import linh.vn.cinegoticket.repository.InvalidatedTokenRepository;
import linh.vn.cinegoticket.repository.UserRepository;
import linh.vn.cinegoticket.repository.EmailVerificationTokenRepository;
import linh.vn.cinegoticket.service.AuthenticationService;
import linh.vn.cinegoticket.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    @Value("${jwt.signerKey}")
    private String SIGNER_KEY;

    @Value("${jwt.valid-duration}")
    private long VALID_DURATION;

    @Value("${jwt.refreshable-duration}")
    private long REFRESHABLE_DURATION;

    @Value("${jwt.email.domain}")
    private String domain;

    private  final UserRepository userRepository;
    private  final InvalidatedTokenRepository invalidatedTokenRepository;
    private  final AuthenticationManager authenticationManager;
    private  final UserMapper userMapper;
    private  final EmailVerificationTokenRepository emailTokenRepository;
    private  final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ApiResponse signup(SignUpRequest request, String ip) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ApiResponse.error("Email đã tồn tại");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            return ApiResponse.error("Username đã tồn tại");
        }

        User user = userMapper.toPendingUser(request);

        userRepository.save(user);
        log.info("New user registered: {} from IP: {}", user.getUsername(), ip);

        // Tạo token
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        emailTokenRepository.save(token);

        // Link verify
        String verifyLink = domain + "/api/auth/verify?token=" + token.getToken();

        // Gửi mail xác nhận
        String emailBody =
                "Chào " + user.getUsername() + " .Vui lòng click vào link sau để kích hoạt tài khoản:" + verifyLink + " .Link hết hạn sau 24 giờ.";

        emailService.sendMail(
                user.getEmail(),
                "Xác nhận tạo tài khoản CINEGO",
                emailBody
        );

        return ApiResponse.success("Đăng ký thành công, vui lòng kiểm tra email để xác nhận!");
    }


    @Override
    public AuthenticationResponse login(AuthenticationRequest request) {
        // Xác thực thông tin đăng nhập
        Authentication authentication =authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            log.info("Authentication successful for user: {}", request.getUsername());

        //        Spring Security đã tự load UserDetails trong quá trình authenticate.
        //➡ Việc load lại DB lần thứ 2 không sai nhưng không cần thiết, chậm hệ thống.
        //✔ Cách đúng: Lấy user từ SecurityContext:

        SecurityContextHolder.getContext().setAuthentication(authentication);
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;

        if (principal instanceof org.springframework.security.core.userdetails.User) {
            username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
        } else {
            username = principal.toString();
        }

        // Lấy entity User từ DB
        log.info("Fetching user details for username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found after authentication", ErrorCode.UNAUTHENTICATED));

        if(!user.getStatus().equals(UserStatus.ACTIVE)){
            throw new AppException("Tài khoản chưa được kích hoạt. Vui lòng kiểm tra email để xác nhận.", ErrorCode.UNAUTHENTICATED);
        }

        log.info(user.getUsername() + " logged in successfully.");

        // Tạo JWT: chỉ tạo accesstoken
        var accessToken = generateToken(user);
//      var refreshToken = generateToken(user, REFRESHABLE_DURATION);

            return AuthenticationResponse.builder()
                    .token(accessToken)
//                    .refreshToken(refreshToken)
                    .authenticated(true)
                    .build();

    }

    @Override
    public String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("devteria.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))//jwtAuthenticationConverter()  mặc định: lấy quyền hạn từ claim scope
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        //1. PARSE TOKEN từ string >> token 3 phần
        SignedJWT signedJWT = SignedJWT.parse(token);
        //1. xác minh chữ kí
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        var verified = signedJWT.verify(verifier);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT
                .getJWTClaimsSet()
                .getIssueTime()
                .toInstant()
                .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();


        if (!(verified && expiryTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }


    @Override
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException exception) {
            log.info("Token already expired or invalid");
            // Ném lỗi 401 Unauthorized
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is invalid or expired");
        }
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        SignedJWT signedJWT;
        try {
            signedJWT = verifyToken(request.getToken(), true); // Xác minh Refresh Token
        } catch (AppException ex) {
            log.warn("Token validation failed: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token has been revoked");
        }

        // Lấy thông tin từ token
        var jit = signedJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        // Lưu token vào danh sách đã vô hiệu hóa
        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();
        invalidatedTokenRepository.save(invalidatedToken);

        // Lấy thông tin người dùng từ token, Sinh token mới
        var username = signedJWT.getJWTClaimsSet().getSubject();
        var user = userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        var token = generateToken(user);

        return AuthenticationResponse.builder().token(token).authenticated(true).build();
    }


    //tạo claim scope cho token
    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
            });

        return stringJoiner.toString();//"ROLE_ADMIN ROLE_USER".
    }

}
