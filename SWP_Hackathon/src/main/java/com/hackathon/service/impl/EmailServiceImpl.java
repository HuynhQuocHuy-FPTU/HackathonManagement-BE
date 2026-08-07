package com.hackathon.service.impl;

import com.hackathon.email.MailRequest;
import com.hackathon.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;


@Slf4j
@Service
@RequiredArgsConstructor
// Tạo nội dung và gửi các loại thư điện tử phục vụ xác thực, bảo mật và thông báo.
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.dev-log-link:true}")
    private boolean devLogLink;

    @Override
    @Async
    // Gửi đường dẫn xác minh để người dùng kích hoạt tài khoản mới.
    public void sendVerificationEmail(String toEmail, String token) {
        // Ghép mã xác minh vào đường dẫn của giao diện người dùng.
        String verifyUrl = frontendUrl + "/verify-account?token=" + token;
        // Thiết lập tiêu đề giúp người nhận nhận biết mục đích của thư.
        String subject = "Xác thực tài khoản Hackathon";
        // Tạo nội dung thư văn bản và chèn đường dẫn xác minh.
        String body = """
                Xin chào,
                
                Vui lòng nhấn vào liên kết sau để xác thực email và kích hoạt tài khoản:
                %s
                
                Liên kết có hiệu lực trong 24 giờ.
                
                Trân trọng,
                Ban tổ chức Hackathon
                """.formatted(verifyUrl);

        // Khi chưa cấu hình tài khoản gửi thư, chỉ ghi đường dẫn trong môi trường phát triển.
        if (!StringUtils.hasText(mailUsername)) {
            // Chỉ ghi thông tin nhạy cảm khi cấu hình phát triển cho phép.
            if (devLogLink) {
                log.info("=== DEV: Verification link for {} ===\n{}", toEmail, verifyUrl);
            }
            return;
        }

        // Tạo thư văn bản đơn giản sau khi đã xác nhận cấu hình gửi thư tồn tại.
        SimpleMailMessage message = new SimpleMailMessage();
        // Gán lần lượt người gửi, người nhận, tiêu đề và nội dung của thư.
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        // Chuyển thư cho bộ gửi thư của hệ thống.
        mailSender.send(message);
    }

    @Override
    @Async
    // Gửi mã xác thực dùng cho quá trình đặt lại mật khẩu.
    public void sendForgotPasswordEmail(String toEmail, String otp) {
        // Chuẩn bị đường dẫn trang nhập mật khẩu mới để dùng khi ghi nhật ký phát triển.
        String resetUrl = frontendUrl + "/reset-password";
        String subject = "[Hackathon System] Yêu cầu đặt lại mật khẩu";
        String body = """
                Xin chào,
                
                Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình.
                Vui lòng sử dụng mã xác thực sau để hoàn thành quá trình đặt lại mật khẩu:
                %s
                
                Mã xác thực có hiệu lực trong 15 phút.
                
                Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                
                Trân trọng,
                Ban tổ chức Hackathon
                """.formatted(otp);

        // Không gọi máy chủ thư khi tài khoản gửi chưa được cấu hình.
        if (!StringUtils.hasText(mailUsername)) {
            if (devLogLink) {
                log.info("=== DEV: Forgot password link for {} ===\n{}", toEmail, resetUrl);
            }
            return;
        }

        // Tạo và gửi thư văn bản chứa mã đặt lại mật khẩu.
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @Override
    @Async
    // Gửi một thư văn bản chung với tiêu đề và nội dung do luồng gọi cung cấp.
    public void sendGeneralEmail(String toEmail, String tile, String emailMessage) {
        // Khởi tạo thư và điền đầy đủ thông tin bắt buộc trước khi gửi.
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject("FPT HACKATHON - " + tile);
        message.setText(emailMessage);
        mailSender.send(message);
    }

    @Override
    @Async
    // Thông báo cho giám khảo khi có bài dự thi cần được chấm lại.
    public void sendNotifyToExpertReEvaluation(String toEmail, String teamName) {
        // Sử dụng thư văn bản vì nội dung thông báo không cần mẫu giao diện phức tạp.
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject("FPT HACKATHON - Yêu cầu chấm lại bài dự thi theo đơn phúc khảo");

        // Chèn tên đội vào nội dung để giám khảo xác định đúng bài cần xử lý.
        message.setText("""
            Kính gửi Quý Ban Giám khảo,

            Ban Tổ chức trân trọng thông báo rằng đơn phúc khảo của đội %s đã được xem xét và chấp thuận.

            Theo đó, Ban Tổ chức kính đề nghị Quý Ban Giám khảo tiến hành xem xét và chấm lại bài dự thi của đội theo quy định của cuộc thi.

            Kính mong Quý Ban Giám khảo hoàn thành việc chấm lại trong thời gian quy định để Ban Tổ chức tổng hợp và công bố kết quả.

            Trân trọng,

            Ban Tổ chức FPT Hackathon
            """.formatted(teamName));

        // Gửi thông báo sau khi hoàn thiện nội dung.
        mailSender.send(message);

    }

    @Override
    @Async
    // Gửi tài khoản và mật khẩu tạm thời cho thành viên được ban tổ chức mời.
    public void sendTemporaryPasswordEmail(String toEmail, String tempPassword, String fullName) {
        // Tạo đường dẫn đăng nhập để người nhận có thể sử dụng tài khoản ngay.
        String loginUrl = frontendUrl + "/login";
        String subject = "[Hackathon System] Thông tin cấp tài khoản thành viên mới";
        String body = """
                Xin chào %s,
                
                Bạn đã được Ban tổ chức cấp tài khoản thành viên trên hệ thống quản lý Hackathon.
                Dưới đây là thông tin đăng nhập của bạn:
                - Đường dẫn hệ thống: %s
                - Tài khoản (Email): %s
                - Mật khẩu tạm thời: %s
                
                LƯU Ý BẢO MẬT: 
                Để đảm bảo an toàn, bạn bắt buộc phải thực hiện đổi mật khẩu trong lần đầu tiên đăng nhập thành công vào hệ thống.
                
                Trân trọng,
                Ban tổ chức giải đấu.
                """.formatted(fullName, loginUrl, toEmail, tempPassword);

        // Nếu chưa cấu hình gửi thư, ghi thông tin theo chế độ phát triển rồi kết thúc.
        if (!StringUtils.hasText(mailUsername)) {
            if (devLogLink) {
                log.info("=== DEV: Temporary password for {} ===\nPassword: {}\nLogin URL: {}", toEmail, tempPassword, loginUrl);
            }
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }


    @Override
    @Async
    // Gửi thư HTML bằng mẫu Thymeleaf và dữ liệu động trong yêu cầu gửi thư.
    public void sendEmail(MailRequest request, String templateName) throws MessagingException {
        try {
            // Tạo thư MIME để hỗ trợ nội dung HTML và các phần mở rộng khi cần.
            MimeMessage message = mailSender.createMimeMessage();

            // Dùng mã hóa UTF-8 để nội dung tiếng Việt hiển thị đúng.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");

            // Đưa các biến động của yêu cầu vào ngữ cảnh xử lý mẫu.
            Context context = new Context();
            context.setVariables(request.getProps());

            // Kết hợp mẫu và dữ liệu để tạo nội dung HTML hoàn chỉnh.
            String html = templateEngine.process(templateName, context);

            // Gán người nhận, tiêu đề và nội dung HTML trước khi gửi.
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(html, true);
            // Gửi thư sau khi toàn bộ trường bắt buộc đã được thiết lập.
            mailSender.send(message);
            System.out.println("Gửi Email thành công tới: " + request.getTo());

            // Chuyển lỗi gửi thư sang ngoại lệ chuyên biệt để luồng gọi có thể xử lý thống nhất.
        } catch (Exception e) {
            System.out.println("Gửi Email thất bại tới: " + request.getTo() + e.getMessage());
            throw new MessagingException("Lỗi gửi email: " + e.getMessage(), e);

        }
    }

}
