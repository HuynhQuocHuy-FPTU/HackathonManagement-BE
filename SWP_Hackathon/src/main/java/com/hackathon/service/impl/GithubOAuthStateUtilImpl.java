package com.hackathon.service.impl;

import com.hackathon.service.GithubOAuthStateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


 // state = base64(accountId:timestamp:signatureHex)
@Component
public class GithubOAuthStateUtilImpl implements GithubOAuthStateUtil {

    // Dùng chung 1 secret riêng cho OAuth state, KHÁC với secret ký JWT để tránh phụ thuộc chéo.
    @Value("${github.oauth.state-secret}")
    private String stateSecret;

    private static final long MAX_AGE_MILLIS = 10 * 60 * 1000; // state chỉ có hiệu lực 10 phút

    @Override
    // Tạo trạng thái có chữ ký chứa mã tài khoản và thời điểm bắt đầu liên kết.
    public String encode(Integer accountId) {
        long timestamp = System.currentTimeMillis();
        String payload = accountId + ":" + timestamp;
        String signature = sign(payload);
        String raw = payload + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

   //Trả về accountId nếu hợp lệ, throw nếu bị giả mạo hoặc hết hạn.
    @Override
    public Integer decodeAndVerify(String state) {
        String raw = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
        String[] parts = raw.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("State không hợp lệ");
        }

        String accountIdStr = parts[0];
        String timestampStr = parts[1];
        String signature = parts[2];

        String payload = accountIdStr + ":" + timestampStr;
        String expectedSignature = sign(payload);

        if (!expectedSignature.equals(signature)) {
            throw new IllegalArgumentException("State bị giả mạo hoặc không hợp lệ");
        }

        long timestamp = Long.parseLong(timestampStr);
        if (System.currentTimeMillis() - timestamp > MAX_AGE_MILLIS) {
            throw new IllegalArgumentException("State đã hết hạn, vui lòng thử liên kết lại");
        }

        return Integer.parseInt(accountIdStr);
    }

    // Ký nội dung trạng thái bằng khóa bí mật để phát hiện dữ liệu bị thay đổi.
    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stateSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể ký state", e);
        }
    }
}
