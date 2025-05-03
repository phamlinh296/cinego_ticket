package linh.vn.cinegoticket.controller;

import linh.vn.cinegoticket.dto.response.MovieResponse;
import linh.vn.cinegoticket.repository.UserRepository;
import linh.vn.cinegoticket.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(MovieController.class)
@AutoConfigureMockMvc(addFilters = false) // Tắt filter để không bị chặn bởi Security
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieService movieService;
    @MockBean
    private UserRepository userRepository;  // Mock UserRepository


    @Test
    void getMovies_validRequest_shouldReturnListOfMovies() throws Exception {
        // Tạo mock MovieResponse
        MovieResponse movie1 = new MovieResponse();
        movie1.setId(1L);
        movie1.setTitle("Inception");
        movie1.setDurationInMins(148);
        movie1.setLanguage("English");

        MovieResponse movie2 = new MovieResponse();
        movie2.setId(2L);
        movie2.setTitle("Parasite");
        movie2.setDurationInMins(132);
        movie2.setLanguage("Korean");

        // Mock service
        when(movieService.getMovies(0, 100000)).thenReturn(List.of(movie1, movie2));

        // Gọi API và kiểm tra phản hồi
        mockMvc.perform(get("/api/movie/getall")
                        .param("pageNumber", "0")
                        .param("pageSize", "100000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].title").value("Inception"))
                .andExpect(jsonPath("$[1].title").value("Parasite"));
    }

    //1. pageSize là chuỗi không hợp lệ
    @Test
    void getMovies_invalidPageSizeFormat_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/movie/getall")
                        .param("pageSize", "abc")  // Kiểu dữ liệu sai
                        .param("pageNumber", "0"))
                .andExpect(status().isBadRequest());
    }

    //2. pageSize âm
    @Test
    void getMovies_negativePageSize_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/movie/getall")
                        .param("pageSize", "-5")  // Giá trị âm
                        .param("pageNumber", "0"))
                .andExpect(status().isBadRequest());

    }


}

