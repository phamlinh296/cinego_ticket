package linh.vn.cinegoticket.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
    private MessageDigest digest;//class tạo bộ băm theo các thuật toán như SHA-1, SHA-256, MD5, v.v.

    public HashUtil() throws NoSuchAlgorithmException {//tạo bộ bam
        this.digest = MessageDigest.getInstance("SHA-256");//khởi tạo đối tượng MessageDigest sử dụng thuật toán SHA-256.
    }

    public String calculateHash(String inputData) {
        byte[] inputBytes = inputData.getBytes(StandardCharsets.UTF_8);//chuyển chuỗi cần mã hóa thành byte
        byte[] hashBytes = digest.digest(inputBytes);//băm và trả trả về mảng byte
        return bytesToHex(hashBytes);//chuyển mảng byte thành chuỗi hex
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes)// Duyệt qua từng byte trong mảng bytes
            result.append(String.format("%02x", b));//Chuyển mỗi byte thành chuỗi hex 2 ký tự

        return result.toString();
    }

}