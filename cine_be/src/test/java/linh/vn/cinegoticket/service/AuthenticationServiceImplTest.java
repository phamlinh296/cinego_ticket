package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.request.securequest.AuthenticationRequest;
import linh.vn.cinegoticket.dto.response.AuthenticationResponse;
import linh.vn.cinegoticket.entity.User;
import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.exception.ErrorCode;
import linh.vn.cinegoticket.repository.UserRepository;
import linh.vn.cinegoticket.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)// phải có khi Dùng JUnit 5 (@Test từ org.junit.jupiter.api) + @Mock, @InjectMocks thì các anno này ms hđ
class AuthenticationServiceImplTest {

    @Mock//các phụ thuộc cua authenticationService
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;


    @Spy//generateToken() là method thật, kp mock method => Mockito k thể mock method đó nếu k mock authenticationService (hiện tại bạn đang dùng @InjectMocks).
    //dùng method thật thay vì giả lập method gentoken
    @InjectMocks//đtg chính
    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        // Inject SIGNER_KEY thủ công vì không có @Value khi chạy unit test
        ReflectionTestUtils.setField(authenticationService, "SIGNER_KEY", "sEzN49A5eMzybTam7Km8m5KHpF36Vp+YnVZ/B5VaVsUrHvgDBvnIEy/MznZGVl+5");
    }

    //Thành công:
    @Test
    void login_validCredentials_returnsToken() {
        // 1. Arrange - bối cảnh giả lập (dl đầu vào khi gọi hàm chính + các bước để n thành công; để khi gọi hàm chính n trả về kq)
        AuthenticationRequest request = new AuthenticationRequest("admin", "123");
        User user = new User(); user.setUsername("admin");

        // Giả lập xác thực thành công: trả về Authentication object
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("admin", "123", List.of()));
            //nghĩa là khi hàm cần test(when(authenticate()) nhận 1 đtg tham số (bky đtg nào (any) là obj của UsernamePasswordAuthen)
            //thì sẽ trả về (thenreturn) như kia; để tí so sánh (assert) gtri trả về này vs gtri mong muốn.

        // Giả lập DB trả về user tương ứng
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(user));

        // Giả lập sinh token thành công
        //dù mock gentoken thật, nhưng Vẫn cần override nếu muốn kết quả giả
        when(authenticationService.generateToken(user))
                .thenReturn("fake-jwt");
        //Nếu KHÔNG override thì method sẽ chạy logic thật; thực sự tạo JWT (có thể fail nếu thiếu cấu hình signerKey).

        //Act - Gọi hàm chính cần test- Gọi login(...) để xem với dữ liệu giả lập như trên, hệ thống trả kq ra sao
        AuthenticationResponse res = authenticationService.login(request);

        //Assert - So sánh kq res với kỳ vọng.
        assertEquals("fake-jwt", res.getToken()); //token trả về đúng như mong muốn
        assertTrue(res.isAuthenticated(), "User chưa được xác thực đúng"); //flag authenticated trả về là true
    }

    //User không tồn tại:
    @Test
    void login_userNotFound_throwsAppException() {
        // 1. Chuẩn bị input đầu vào cho hàm login()
        AuthenticationRequest request = new AuthenticationRequest("ghost", "123");

        // 2. Giả lập việc xác thực username/password thành công
        //c1
//        doAnswer(invocation -> null)
//                .when(authenticationManager)
//                .authenticate(any(UsernamePasswordAuthenticationToken.class));//chạy method thật của authenticate(), k giả lập > looix
        //authenticate() KHÔNG phải void → không dùng được doNothing() > Dùng doAnswer để giả lập method kp void trả về null.

        //c2 giả lập kq trả về thay
        when(authenticationManager.authenticate(any()))
                .thenReturn(mock(Authentication.class)); // hoặc return một object giả nào đó

        //giả lập lời gọi authenticate() thành công (k ném lỗi vì mặc định authenticate() ném excep nếu có lỗi pass/username).
        //nhưng ở đây đang muốn test lỗi k tìm thấy user trong db, nên giả vờ bc này thành công

        // 3. Giả lập không tìm thấy user trong database
        when(userRepository.findByUsername("ghost"))
                .thenReturn(Optional.empty());//gọi k trả về user nào

        // 4. Gọi hàm login() và kiểm tra nó ném ra AppException
        AppException ex = assertThrows(AppException.class, () -> {
            authenticationService.login(request);
        });

        // 5. Kiểm tra xem lỗi ném ra có đúng mã lỗi không
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }


    //sai password
    @Test
    void login_invalidPassword_throwsAppException() {
        AuthenticationRequest request = new AuthenticationRequest("admin", "wrong");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)//khi gọi tới authen() ném ra BadCredentialsException, mô phỏng đúng hành vi khi password sai
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        AppException ex = assertThrows(AppException.class, () -> {
            authenticationService.login(request);//gọi hàm that để n ném Bad credentials
        });

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());//khi có excep, try catch ném ra excep này
    }
}
