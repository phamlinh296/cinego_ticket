package linh.vn.cinegoticket.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final String[] PUBLIC_ENDPOINTS = {
            "/api/user/**", "/api/auth/**", "/api/movie/**", "/api/show/**", "/api/hall/**",
            "/api/payment/**", "/api/booking/**", "/api/genre/**"
    };

    @Autowired
    private CustomJwtDecoder customJwtDecoder;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Kích hoạt CORS
                .authorizeHttpRequests(request ->
                        request
                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
//                                .requestMatchers("/api/auth/**").permitAll()
                                .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2
                                -> oauth2.jwt(jwtConfigurer -> jwtConfigurer
                                        .decoder(customJwtDecoder)// Thiết lập bộ giải mã JWT
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter())//Chuyển đổi thông tin từ JWT thành đối tượng Authentication
                                )
                                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                );
        ;
        return httpSecurity.build();
    }


    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Cho phép tất cả origin (không bảo mật, chỉ nên dùng trong dev)
        corsConfig.addAllowedOriginPattern("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
//        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

//    @Bean
//    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//
//        // CORS cho API công khai (cho phép tất cả)
//        CorsConfiguration publicCorsConfig = new CorsConfiguration();
//        publicCorsConfig.addAllowedOrigin("*");
//        publicCorsConfig.addAllowedMethod("*");
//        publicCorsConfig.addAllowedHeader("*");
//
//        // CORS cho API bảo mật (yêu cầu JWT)
//        CorsConfiguration securedCorsConfig = new CorsConfiguration();
//        securedCorsConfig.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:5500")); // Chỉ định cụ thể
//        securedCorsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        securedCorsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
//        securedCorsConfig.setAllowCredentials(true); // Nếu API yêu cầu JWT hoặc cookie, setAllowCredentials(true); là bắt buộc.
//
//        // Áp dụng cấu hình
//        source.registerCorsConfiguration("/api/public/**", publicCorsConfig); // API công khai
//        source.registerCorsConfiguration("/api/**", securedCorsConfig); // API bảo mật
//
//        return source;
//    }
}
