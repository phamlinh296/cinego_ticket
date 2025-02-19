package linh.vn.cinegoticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CinemaHallRequest {

    @NotBlank
    @NotNull
    private String name;

    @NotNull
    private int totalRow;

    @NotNull
    private int totalCol;
}
