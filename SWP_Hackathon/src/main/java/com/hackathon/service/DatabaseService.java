package com.hackathon.service;

<<<<<<< HEAD

=======
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import com.hackathon.entity.enums.StudentStatus;
import com.hackathon.repository.*;
import lombok.AllArgsConstructor;
<<<<<<< HEAD
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
=======
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Data
@NoArgsConstructor
@AllArgsConstructor
<<<<<<< HEAD
@Builder
=======
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1
public class DatabaseService {
    @Autowired
    private CriteriaSetRepository criteriaSetRepository;

    @Autowired
    private CriteriaDetailRepository criteriaDetailRepository;

    @Autowired
    private ExpertRepository expertRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EventCoordinatorRepository eventCoordinatorRepository;

    @Autowired
    private StudentRepository studentRepository;

<<<<<<< HEAD
=======
    @Autowired
    private PasswordEncoder passwordEncoder;

>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1

    public void createDatabase(){

        //create account dưới database
<<<<<<< HEAD
        Account acc1 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("nguyenvan30498@gmail.com").accountName("vanvan834").phone("0976352891").status(AccountStatus.ACTIVE).password("123456").role(AccountRole.EVENTCOORDINATOR).build());

        Account acc2 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("tranhoa456@gmail.com").password("123456").accountName("hoahoa").phone("0983452324").status(AccountStatus.ACTIVE).role(AccountRole.EXPERT).build());

        Account acc3 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("lehuyen4238@gmail.com").password("123456").accountName("huyenhuyen").phone("097635235").status(AccountStatus.ACTIVE).role(AccountRole.EXPERT).build());

        Account acc4 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("lehoa345@gmail.com").password("123456").accountName("lehoa").phone("0126789354").status(AccountStatus.ACTIVE).role(AccountRole.STUDENT).build());

        Account acc5 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("nguyenha@gmail.com").password("123456").accountName("haha234").phone("0976336472").status(AccountStatus.ACTIVE).role(AccountRole.STUDENT).build());
=======
        if (!accountRepository.existsByEmail("admin@hackathon.com")) {
            accountRepository.save(Account.builder()
                    .createdAt(LocalDateTime.now())
                    .email("admin@hackathon.com")
                    .accountName("System Admin")
                    .phone("0123456789")
                    .status(AccountStatus.ACTIVE)
                    .password(passwordEncoder.encode("Admin@123")) // 🎯 Mật khẩu đã được băm an toàn
                    .role(AccountRole.ADMIN)
//                    .isPasswordChanged(true) // Admin tối cao thì gán luôn true để không bị ép đổi pass
                    .build());
            System.out.println("🚀 Đã khởi tạo tài khoản Admin: admin@hackathon.com / Mật khẩu: Admin@123");
        }
        Account acc1 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("nguyenvan30498@gmail.com").accountName("vanvan834").phone("0976352891").status(AccountStatus.ACTIVE).password(passwordEncoder.encode("123456")).role(AccountRole.EVENTCOORDINATOR).build());

        Account acc2 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("tranhoa456@gmail.com").password(passwordEncoder.encode("123456")).accountName("hoahoa").phone("0983452324").status(AccountStatus.ACTIVE).role(AccountRole.EXPERT).build());

        Account acc3 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("lehuyen4238@gmail.com").password(passwordEncoder.encode("123456")).accountName("huyenhuyen").phone("097635235").status(AccountStatus.ACTIVE).role(AccountRole.EXPERT).build());

        Account acc4 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("lehoa345@gmail.com").password(passwordEncoder.encode("123456")).accountName("lehoa").phone("0126789354").status(AccountStatus.ACTIVE).role(AccountRole.STUDENT).build());

        Account acc5 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("nguyenha@gmail.com").password(passwordEncoder.encode("123456")).accountName("haha234").phone("0976336472").status(AccountStatus.ACTIVE).role(AccountRole.STUDENT).build());
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1


        //Create eventcoordiantor
        EventCoordinator eventCoordinator1 = eventCoordinatorRepository.save(EventCoordinator.builder().coordinatorName("Nguyễn Văn Văn").department("Phòng công tác sinh viên ").account(acc1).build());

        //create expert
        expertRepository.save(Expert.builder().expertName("Trần Thị Hoa").department("Khoa kĩ thuật").account(acc2).build());

        expertRepository.save(Expert.builder().expertName("Lê Huyền").department("Khoa kĩ thuật").account(acc3).build());

        //create student
        studentRepository.save(Student.builder().studentCode("SE192345").studentName("Lê Hòa").major("Software engineer").status(StudentStatus.STUDYING).startDate(LocalDateTime.now()).build());

        studentRepository.save(Student.builder().studentCode("SE190934").studentName("Nguyễn Hà").status(StudentStatus.STUDYING).startDate(LocalDateTime.now()).major("Software engineer").build());

        // tạo criteria set dưới database
        CriteriaSet criteriaSet1 = criteriaSetRepository.save(CriteriaSet.builder().criteriaSetName("Đánh giá ý tưởng và thiết kế/ nguyên mẫu").maxScore(100).eventCoordinator(eventCoordinator1).build());

        CriteriaSet criteriaSet2 = criteriaSetRepository.save(CriteriaSet.builder().criteriaSetName("Đánh giá nguyên mẫu và demo").maxScore(100).eventCoordinator(eventCoordinator1).build());

        CriteriaSet criteriaSet3 = criteriaSetRepository.save(CriteriaSet.builder().criteriaSetName("Bộ tiêu chí đánh giá dự án").eventCoordinator(eventCoordinator1).maxScore(100).build());

        CriteriaSet criteriaSet4 = criteriaSetRepository.save(CriteriaSet.builder().criteriaSetName("Bộ tiêu chí đánh giá sản phẩm").eventCoordinator(eventCoordinator1).maxScore(100).build());

        //tạo criteria detail dưới database
        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet1).criteriaName("Bám sát chủ đề, mức độ phù hợp của ý tưởng").weight(new BigDecimal(30)).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet1).criteriaName("Khả thi logic, có khả năng triển khai trong 48h hay không").weight(new BigDecimal(25)).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet1).criteriaName("Sự sáng tạo và đổi mới").weight(new BigDecimal(30)).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet1).criteriaName("Trực quan, rõ ràng và hướng đên người dùng").weight(new BigDecimal(15)).build());

        //===============================

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet2).criteriaName("Ứng dụng AI và hiệu quả sáng tạo").weight(new BigDecimal(20)).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet2).criteriaName("Chất lượng kĩ thuật").description("Chất lượng mã nguồn, độ ổn định và chức năng của sản phẩm").weight(new BigDecimal(25)).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet2).criteriaName("Giao diện người dùng đẹp mắt").description("Đẹp mắt, dễ sử dụng, thân thiện").weight(new BigDecimal(15)).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet2).criteriaName("Trải nghiệm người dùng").description("Mượt mà, ít lỗi, dễ tiếp cận").weight(new BigDecimal(15)).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet2).criteriaName("Trình bày và demo").description("Logic, rõ ràng, trả lời tất cả câu hỏi của ban giám khảo").weight(new BigDecimal(25)).build());



    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1
