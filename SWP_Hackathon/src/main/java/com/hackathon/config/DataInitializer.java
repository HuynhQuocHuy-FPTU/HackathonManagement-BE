package com.hackathon.config;

import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Value("${app.init-data:false}") // Mặc định là false (không chạy)
    private boolean initData;

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


    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private HackathonEventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;
    @Override
    public void run(String... args) throws Exception {
        if (!initData) {
            return;
        }
        if (!accountRepository.existsByEmail("admin@hackathon.com")) {
            accountRepository.save(Account.builder()
                    .createdAt(LocalDateTime.now())
                    .email("admin@hackathon.com")
                    .phone("0123456789")
                    .status(AccountStatus.ACTIVE)
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(AccountRole.ADMIN)
                    .isPasswordChanged(true) // Admin tối cao thì gán luôn true để không bị ép đổi pass
                    .build());
            System.out.println("Đã khởi tạo tài khoản Admin: admin@hackathon.com / Mật khẩu: Admin@123");
        }
        Account acc1 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("nguyenvan30498@gmail.com").phone("0976352891").status(AccountStatus.ACTIVE).password(passwordEncoder.encode("123456")).isPasswordChanged(true).role(AccountRole.EVENTCOORDINATOR).build());

        Account acc2 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("tranhoa456@gmail.com").password(passwordEncoder.encode("123456")).phone("0983452324").isPasswordChanged(true).status(AccountStatus.ACTIVE).role(AccountRole.EXPERT).build());

        Account acc3 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("lehuyen4238@gmail.com").password(passwordEncoder.encode("123456")).phone("097635235").isPasswordChanged(true).status(AccountStatus.ACTIVE).role(AccountRole.EXPERT).build());

        Account acc4 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("lehoa345@gmail.com").password(passwordEncoder.encode("123456")).phone("0126789354").isPasswordChanged(true).status(AccountStatus.ACTIVE).role(AccountRole.STUDENT).build());

        Account acc5 = accountRepository.save(Account.builder().createdAt(LocalDateTime.now()).email("nguyenha@gmail.com").password(passwordEncoder.encode("123456")).phone("0976336472").isPasswordChanged(true).status(AccountStatus.ACTIVE).role(AccountRole.STUDENT).build());


        //Create eventcoordiantor
        EventCoordinator eventCoordinator1 = eventCoordinatorRepository.save(EventCoordinator.builder().coordinatorName("Nguyễn Văn Văn").department("Phòng công tác sinh viên ").account(acc1).build());

        //create expert
        expertRepository.save(Expert.builder().expertName("Trần Thị Hoa").department("Khoa kĩ thuật").account(acc2).build());

        expertRepository.save(Expert.builder().expertName("Lê Huyền").department("Khoa kĩ thuật").account(acc3).build());

        //create student
        studentRepository.save(Student.builder().studentCode("SE192345").studentName("Lê Hòa").major("Software engineer").account(acc4).build());

        studentRepository.save(Student.builder().studentCode("SE190934").studentName("Nguyễn Hà").major("Software engineer").account(acc5).build());

        // tạo criteria set dưới database
        CriteriaSet criteriaSet1 = criteriaSetRepository.save(CriteriaSet.builder().criteriaSetName("Đánh giá ý tưởng và thiết kế/ nguyên mẫu").maxScore(100).eventCoordinator(eventCoordinator1).build());

        CriteriaSet criteriaSet2 = criteriaSetRepository.save(CriteriaSet.builder().criteriaSetName("Đánh giá nguyên mẫu và demo").maxScore(100).eventCoordinator(eventCoordinator1).build());

        CriteriaSet criteriaSet3 = criteriaSetRepository.save(CriteriaSet.builder().criteriaSetName("Bộ tiêu chí đánh giá dự án").eventCoordinator(eventCoordinator1).maxScore(100).build());

        CriteriaSet criteriaSet4 = criteriaSetRepository.save(CriteriaSet.builder().criteriaSetName("Bộ tiêu chí đánh giá sản phẩm").eventCoordinator(eventCoordinator1).maxScore(100).build());

        //tạo criteria detail dưới database
        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet1).criteriaName("Bám sát chủ đề, mức độ phù hợp của ý tưởng").weight(new BigDecimal(30)).criteriaType(CriteriaType.SUBMISSION).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet1).criteriaName("Khả thi logic, có khả năng triển khai trong 48h hay không").weight(new BigDecimal(25)).criteriaType(CriteriaType.SUBMISSION).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet1).criteriaName("Sự sáng tạo và đổi mới").weight(new BigDecimal(30)).criteriaType(CriteriaType.SUBMISSION).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet1).criteriaName("Trực quan, rõ ràng và hướng đên người dùng").weight(new BigDecimal(15)).criteriaType(CriteriaType.PRESENTATION).build());

        //===============================

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet2).criteriaName("Ứng dụng AI và hiệu quả sáng tạo").weight(new BigDecimal(20)).criteriaType(CriteriaType.SUBMISSION).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet2).criteriaName("Chất lượng kĩ thuật").description("Chất lượng mã nguồn, độ ổn định và chức năng của sản phẩm").weight(new BigDecimal(25)).criteriaType(CriteriaType.SUBMISSION).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet2).criteriaName("Giao diện người dùng đẹp mắt").description("Đẹp mắt, dễ sử dụng, thân thiện").criteriaType(CriteriaType.SUBMISSION).weight(new BigDecimal(15)).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet2).criteriaName("Trải nghiệm người dùng").description("Mượt mà, ít lỗi, dễ tiếp cận").criteriaType(CriteriaType.PRESENTATION).weight(new BigDecimal(15)).build());

        criteriaDetailRepository.save(CriteriaDetail.builder().criteriaSet(criteriaSet2).criteriaName("Trình bày và demo").description("Logic, rõ ràng, trả lời tất cả câu hỏi của ban giám khảo").criteriaType(CriteriaType.PRESENTATION).weight(new BigDecimal(25)).build());

        Account[] studentAccounts = new Account[15];
        Student[] students = new Student[15];
        Team[] teams = new Team[5];

        int studentIndex = 0;

// =======================
// 1. CREATE 15 STUDENTS
// =======================
        for (int i = 0; i < 15; i++) {

            studentAccounts[i] = accountRepository.save(
                    Account.builder()
                            .createdAt(LocalDateTime.now())
                            .email("student" + (i + 1) + "@gmail.com")
                            .phone("09000000" + i)
                            .password(passwordEncoder.encode("123456")).isPasswordChanged(true)
                            .status(AccountStatus.ACTIVE)
                            .role(AccountRole.STUDENT)
                            .build()
            );

            students[i] = studentRepository.save(
                    Student.builder()
                            .studentCode("SE" + (200000 + i))
                            .studentName("Student " + (i + 1))
                            .major("Software Engineering")
                            .account(studentAccounts[i])
                            .build()
            );
        }

// =======================
// 2. CREATE 5 TEAMS + TEAM MEMBERS
// =======================
        for (int i = 0; i < 5; i++) {

            Team team = teamRepository.save(
                    Team.builder()
                            .teamName("Team " + (i + 1))
                            .teamSize(3)
                            .status(TeamStatus.DRAFT)
                            .build()
            );

            teams[i] = team;

            // mỗi team 3 student
            for (int j = 0; j < 3; j++) {

                Student student = students[studentIndex++];

                TeamMember member = TeamMember.builder()
                        .team(team)
                        .student(student)
                        .isLeader(j == 0)
                        .build();

                teamMemberRepository.save(member);
            }
        }

//// =======================
//// 3. CREATE REGISTRATION (eventId = 1)
//// =======================
//        HackathonEvent event = eventRepository.findById(1)
//                .orElseThrow(() -> new RuntimeException("Event not found"));
//
//        for (Team team : teams) {
//
//            Registration registration = Registration.builder()
//                    .team(team)
//                    .hackathonEvent(event)
//                    .status(RegistrationStatus.PENDING)
//                    .build();
//
//            registrationRepository.save(registration);
//        }
    }
}
