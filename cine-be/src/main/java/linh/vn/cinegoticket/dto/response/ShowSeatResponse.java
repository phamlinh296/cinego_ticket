package linh.vn.cinegoticket.dto.response;

import linh.vn.cinegoticket.entity.ShowSeat;
import lombok.Getter;

@Getter
public class ShowSeatResponse {
    private String seatId;
    private String status;
    private String type;
    private String name;
    private int rowIndex;
    private int colIndex;
    private double price;

    public ShowSeatResponse(ShowSeat showSeat) {
        this.seatId = showSeat.getId();
        this.status = String.valueOf(showSeat.getStatus());
        this.type = showSeat.getCinemaSeat().getSeatType();
        this.name = showSeat.getCinemaSeat().getName();
        this.rowIndex = showSeat.getCinemaSeat().getRowIndex();
        this.colIndex = showSeat.getCinemaSeat().getColIndex();
        this.price = showSeat.getCinemaSeat().getPrice();
    }

}
