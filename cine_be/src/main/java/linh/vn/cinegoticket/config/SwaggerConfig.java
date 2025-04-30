package linh.vn.cinegoticket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                //Thiết lập tiêu đề, mô tả, version cho giao diện Swagger
                .info(new Info()
                        .title("Cinego Ticketing API")
                        .version("1.0")
                        .description("API cho hệ thống đặt vé xem phim"))
                //hiện nút Authorize, nhập JWT (phải thêm mới hiển thị ô nhập JWT)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    //chia thành các group, thường dùng trong microservice: chia theo user/admin or chức năng ntn
    @Bean
    public GroupedOpenApi movieApi() {
        return GroupedOpenApi.builder()
                .group("Movie & Genre API")
                .addOpenApiMethodFilter(method -> {
                    String controller = method.getDeclaringClass().getSimpleName();
                    return controller.contains("Movie") || controller.contains("Genre");//lọc ra các controller có chữ "Movie" trong tên
                })
                .build();
    }

    @Bean
    public GroupedOpenApi userBookingApi() {
        return GroupedOpenApi.builder()
                .group("Booking & User API")
                .addOpenApiMethodFilter(method -> {
                    String controller = method.getDeclaringClass().getSimpleName();
                    return controller.contains("Booking") || controller.contains("User");
                })
                .build();
    }
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("All API")
                .pathsToMatch("/api/**")
                .build();
    }




}
