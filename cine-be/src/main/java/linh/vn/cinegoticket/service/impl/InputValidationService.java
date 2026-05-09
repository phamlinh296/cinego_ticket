package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.utils.RegexExtractor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Service;

@Service
public class InputValidationService {

    private final String sql_pattern = RegexExtractor.SQL;
    private final String xss_pattern = RegexExtractor.XSS;
    private final String allow_pattern = RegexExtractor.NORMAL_TEXT;


    //XSS
    //mã hóa ktu đặc biệt
    public String sanitizeInput(String input) {
        return Encode.forHtml(input);//Các ký tự đặc biệt như <, >, &, ' sẽ được mã hóa thành HTML an toàn để ngăn chặn XSS.
    }

    //chỉ cho phép thẻ an toàn, bỏ thẻ k an toàn
    public String sanitizeInputWithSafeList(String input, Safelist safelist) {
        return Jsoup.clean(input, safelist);
    }

    //Kiểm tra xem đầu vào có chứa các đoạn mã XSS (ví dụ: <script>) hay không.
    public boolean containsXss(String input) {
        return input.matches(this.xss_pattern);
    }

    //SQL INJECTION
    //Kiểm tra xem đầu vào có khớp với mẫu regex của SQL Injection hay không.
    public boolean containsSqlInjection(String input) {
        return input.matches(this.sql_pattern);
    }

    //ktra đầu vào cả sql injection, xss
    public boolean checkInput(String text) {
        if (this.containsSqlInjection(text))
            return false;
//        if (this.containsXss(text))
//        	return false;
        return true;
    }

}
