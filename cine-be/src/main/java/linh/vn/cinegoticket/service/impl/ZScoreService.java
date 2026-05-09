package linh.vn.cinegoticket.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZScoreService {

    // threshold configurable later via @Value
    private final double threshold = 3.0;

    public boolean isOutlier(double newAmount, List<Double> history) {
        if (history == null || history.size() < 5) return false;

        double mean = history.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double variance = history.stream()
                .mapToDouble(a -> Math.pow(a - mean, 2))
                .sum() / history.size();

        double std = Math.sqrt(variance);
        if (std == 0.0) return false;

        double z = (newAmount - mean) / std;
        return Math.abs(z) > threshold;
    }
}
