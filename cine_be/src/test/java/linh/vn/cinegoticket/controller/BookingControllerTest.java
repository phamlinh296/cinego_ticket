package linh.vn.cinegoticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import linh.vn.cinegoticket.dto.request.BookingRequest;
import linh.vn.cinegoticket.dto.response.BookingResponse;
import linh.vn.cinegoticket.repository.UserRepository;
import linh.vn.cinegoticket.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc(addFilters = false) // Tắt bảo mật nếu không test security
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;
    @MockBean
    private UserRepository userRepository;

    //1. success - 200 OK

//    @Test
//    void createBooking_validRequest_success() throws Exception {
//        // giả lập req và res
//        BookingRequest req = new BookingRequest();
//        req.setShowId("show123");
//        req.setSeatsId(List.of("A1", "A2"));
//
//        BookingResponse mockResponse = new BookingResponse();
//        mockResponse.setShowId("show123");
//        mockResponse.setFullname("John Doe");
//        mockResponse.setSeats(List.of("A1", "A2"));
//
//        // Mock phương thức của BookingService
//        when(bookingService.createBooking(eq("john"), any(BookingRequest.class)))//truyển req này thì trả res này
//                .thenReturn(mockResponse);
//
//        // Gửi yêu cầu và kiểm tra phản hồi
//        mockMvc.perform(post("/api/booking/add")//Gửi HTTP request đến controller; hàm post này
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(new ObjectMapper().writeValueAsString(req))//Chuyển object thành JSON string
//                        .with(user("john").roles("USER")))  // Giả lập user 'john' với role USER
//                .andExpect(status().isOk())//kỳ vọng trả về
//                .andExpect(jsonPath("$.showId").value("show123"))
//                .andExpect(jsonPath("$.fullname").value("John Doe"))
//                .andExpect(jsonPath("$.seats[0]").value("A1"));
//    }


    //1.Thiếu dữ liệu (validation fail  @Valid @RequestBody)- 400 Bad Request
    //Khi request body thiếu trường bắt buộc, ví dụ seatsId null hoặc rỗng.
    @Test
    void createBooking_missingFields_shouldReturnBadRequest() throws Exception {
        BookingRequest req = new BookingRequest(); // thiếu showId, seatsId

        mockMvc.perform(post("/api/booking/add")
                        .with(user("john").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    //2.Sai Role - 	403 Forbidden
    //Khi user không có role "USER" mà chỉ là "GUEST" hoặc không có role.
//    @Test
//    void createBooking_forbiddenRole_shouldReturn403() throws Exception {
//        BookingRequest req = new BookingRequest();
//        req.setShowId("show123");
//        req.setSeatsId(List.of("A1"));
//
//        mockMvc.perform(post("/api/booking/add")
//                        .with(user("john").roles("GUEST"))  // Không đúng role
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(new ObjectMapper().writeValueAsString(req)))
//                .andExpect(status().isForbidden());
//    }
//
//    //3. Chưa đăng nhập (Principal null) - 401 Unauthorized
//    @Test
//    void createBooking_unauthenticated_shouldReturn401() throws Exception {
//        BookingRequest req = new BookingRequest();
//        req.setShowId("show123");
//        req.setSeatsId(List.of("A1"));
//
//        mockMvc.perform(post("/api/booking/add")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(new ObjectMapper().writeValueAsString(req)))
//                .andExpect(status().isUnauthorized());
//    }

}
