package linh.vn.cinegoticket.dto.response;

import linh.vn.cinegoticket.entity.CinemaSeat;
import linh.vn.cinegoticket.enums.ESeatStatus;

public class SeatsResponse {

    private String name;
    private String type;
    private ESeatStatus status;
    private double price;

    public SeatsResponse(CinemaSeat s) {
        this.name = s.getName();
        this.type = s.getSeatType();
        this.status = s.getStatus();
        this.price = s.getPrice();
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getStatus() {
        return this.status.name();
    }

    public double getPrice() {
        return this.price;
    }
}
