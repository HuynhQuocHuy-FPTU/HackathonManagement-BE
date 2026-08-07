package com.hackathon.config;

import com.hackathon.dto.event.EventDescription;
import com.hackathon.dto.event.Prize;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.repository.*;
import com.hackathon.service.impl.EvaluationAuditLogServiceImpl;
import com.hackathon.service.grading.support.ScoreCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    /*
     * Thay bằng ID event và round thật.
     * Round phải thuộc event.
     */
    private static final Integer TARGET_EVENT_ID = 10;
    private static final Integer TARGET_ROUND_ID = 1;

    private static final int ADMIN_COUNT = 2;
    private static final int COORDINATOR_COUNT = 3;
    private static final int EXPERT_COUNT = 6;
    private static final int STUDENT_COUNT = 35;
    private static final int TEAM_COUNT = 10;
    private static final int MEMBER_PER_TEAM = 3;

    @Value("${app.init-data:false}")
    private boolean initData;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EventCoordinatorRepository eventCoordinatorRepository;

    @Autowired
    private ExpertRepository expertRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private HackathonEventRepository eventRepository;

    @Autowired
    private RoundRepository roundRepository;

    @Autowired
    private CategoryRoundRepository categoryRoundRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ExpertAssignRepository expertAssignRepository;

    @Autowired
    private EvaluationCriteriaRepository evaluationCriteriaRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationAuditLogRepository evaluationAuditLogRepository;

    @Autowired
    private EvaluationAuditLogServiceImpl evaluationAuditLogServiceImpl;

    @Autowired
    private CriteriaSetRepository criteriaSetRepository;

    @Autowired
    private CriteriaDetailRepository criteriaDetailRepository;

    @Autowired
    private ScoreCalculator scoreCalculator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (!initData) {
            return;
        }

        String password =
                passwordEncoder.encode("123456");

        createAdmins(password);
        createEventCoordinators(password);

        createExperts(password);
        createCriteriaSets();

        createStudents(password);
        Team[] teams = createTeams();

        createDemoEvents();

        registerTeamsForEvent(
                TARGET_EVENT_ID
        );
//
//        submitForParticipantsWithoutSubmission(
//                TARGET_ROUND_ID
//        );
//
//
//        gradeSubmissionsByAssignments(
//                TARGET_ROUND_ID
//        );
    }

    // =====================================================
    // ACCOUNT
    // =====================================================

    private Account createAccountIfAbsent(
            String email,
            String phone,
            String password,
            AccountRole role
    ) {
        return accountRepository.findByEmail(email)
                .orElseGet(() -> accountRepository.save(
                        Account.builder()
                                .email(email)
                                .phone(phone)
                                .password(password)
                                .status(AccountStatus.ACTIVE)
                                .role(role)
                                .isPasswordChanged(true)
                                .createdAt(LocalDateTime.now())
                                .build()
                ));
    }

    private void createAdmins(String password) {
        for (int index = 1;
             index <= ADMIN_COUNT;
             index++) {

            createAccountIfAbsent(
                    "admin" + index + "@hackathon.com",
                    String.format("090000000%d", index),
                    password,
                    AccountRole.ADMIN
            );
        }
    }

    private void createEventCoordinators(
            String password
    ) {
        for (int index = 1;
             index <= COORDINATOR_COUNT;
             index++) {

            int number = index;

            Account account = createAccountIfAbsent(
                    "coordinator"
                            + number
                            + "@hackathon.com",
                    String.format("090000001%d", number),
                    password,
                    AccountRole.EVENTCOORDINATOR
            );

            boolean profileExists =
                    eventCoordinatorRepository
                            .findByAccount_AccountId(
                                    account.getAccountId()
                            )
                            .isPresent();

            if (!profileExists) {
                eventCoordinatorRepository.save(
                        EventCoordinator.builder()
                                .coordinatorName(
                                        "Event Coordinator "
                                                + number
                                )
                                .department(
                                        "Phòng tổ chức sự kiện"
                                )
                                .organization(
                                        "FPT University"
                                )
                                .account(account)
                                .build()
                );
            }
        }
    }

    private void createDemoEvents() {
        EventCoordinator coordinator = eventCoordinatorRepository
                .findFirstByOrderByCoordinatorIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "Không có Event Coordinator để tạo event demo"));

        LocalDateTime now = LocalDateTime.now();

        createEventIfAbsent(
                "AI Innovation Hackathon 2026",
                "AI Innovation Challenge",
                "FPT University HCM",
                now.plusDays(10),
                now.plusDays(12),
                now.plusDays(7),
                now.plusDays(9),
                EventSeason.SUMMER,
                coordinator
        );

        createEventIfAbsent(
                "Green Tech Hackathon 2026",
                "Technology for a Greener Future",
                "FPT University Da Nang",
                now.plusDays(25),
                now.plusDays(27),
                now.plusDays(20),
                now.plusDays(23),
                EventSeason.FALL,
                coordinator
        );

        createEventIfAbsent(
                "Smart City Hackathon 2026",
                "Building Smarter Cities",
                "FPT University Ha Noi",
                now.plusDays(40),
                now.plusDays(42),
                now.plusDays(35),
                now.plusDays(38),
                EventSeason.FALL,
                coordinator
        );
    }

    private void createEventIfAbsent(
            String eventName,
            String title,
            String address,
            LocalDateTime startDate,
            LocalDateTime endDate,
            LocalDateTime registrationDeadline,
            LocalDateTime workshopTime,
            EventSeason season,
            EventCoordinator coordinator
    ) {
        HackathonEvent event = eventRepository
                .findByEventNameContainingIgnoreCase(eventName)
                .stream()
                .filter(item -> eventName.equalsIgnoreCase(
                        item.getEventName()))
                .findFirst()
                .orElseGet(() -> HackathonEvent.builder()
                        .eventName(eventName)
                        .status(EventStatus.ACTIVE)
                        .createAt(LocalDateTime.now())
                        .eventCoordinator(coordinator)
                        .build());

        event.setTitle(title);
        event.setAddress(address);
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setRegistrationDeadline(registrationDeadline);
        event.setWorkshopTime(workshopTime);
        event.setWorkshopStatus(WorkshopStatus.UPCOMING);
        event.setSeason(season);
        event.setSeasonYear(startDate.getYear());
        event.setMinTeam(2);
        event.setMaxTeam(20);
        event.setMinTeamSize(2);
        event.setMaxTeamSize(5);
        event.setBannerUrl("https://placehold.co/1200x400");
        event.setDescription(createDemoEventDescription(title));

        HackathonEvent savedEvent = eventRepository.save(event);
        createDemoEventStructureIfMissing(savedEvent);
    }

    private EventDescription createDemoEventDescription(String title) {
        return new EventDescription(
                title + " là sân chơi phát triển sản phẩm công nghệ sáng tạo.",
                List.of(
                        new Prize("Giải Nhất", "20.000.000 VNĐ"),
                        new Prize("Giải Nhì", "10.000.000 VNĐ"),
                        new Prize("Giải Ba", "5.000.000 VNĐ")
                ),
                List.of(
                        "Nhận cố vấn từ chuyên gia",
                        "Mở rộng mạng lưới nghề nghiệp",
                        "Nhận chứng nhận tham gia"
                ),
                List.of(
                        "Gian lận hoặc sao chép sản phẩm",
                        "Vi phạm quy tắc ứng xử"
                ),
                List.of(
                        "Mỗi sinh viên chỉ thuộc một team",
                        "Nộp bài đúng thời hạn",
                        "Tuân thủ quyết định của ban tổ chức"
                )
        );
    }

    private void createDemoEventStructureIfMissing(HackathonEvent event) {
        List<Category> categories = categoryRepository
                .findAllByHackathonEvent_EventId(event.getEventId());
        if (categories.isEmpty()) {
            categories = categoryRepository.saveAll(List.of(
                    createCategory(event, "Artificial Intelligence"),
                    createCategory(event, "Software Solutions")
            ));
        }

        List<Round> rounds = roundRepository
                .findAllByHackathonEvent_EventId(event.getEventId());
        if (!rounds.isEmpty()) {
            return;
        }

        CriteriaSet criteriaSet = criteriaSetRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Không có criteria set để tạo round demo"));

        List<Expert> judges = expertRepository.findAll()
                .stream()
                .limit(2)
                .toList();
        if (judges.isEmpty()) {
            throw new IllegalStateException(
                    "Không có expert để phân công event demo");
        }

        Round preliminaryRound = createDemoRound(
                event,
                criteriaSet,
                "Vòng sơ loại",
                1,
                event.getStartDate(),
                event.getStartDate().plusHours(20),
                5
        );
        Round finalRound = createDemoRound(
                event,
                criteriaSet,
                "Vòng chung kết",
                2,
                event.getStartDate().plusDays(1),
                event.getEndDate(),
                3
        );

        for (Round round : List.of(preliminaryRound, finalRound)) {
            createEvaluationCriteriaForRound(round, criteriaSet);
            for (Category category : categories) {
                CategoryRound categoryRound = new CategoryRound();
                categoryRound.setCategory(category);
                categoryRound.setRound(round);
                CategoryRound savedCategoryRound =
                        categoryRoundRepository.save(categoryRound);

                for (int index = 0; index < judges.size(); index++) {
                    ExpertAssign assignment = ExpertAssign.builder()
                            .expert(judges.get(index))
                            .categoryRound(savedCategoryRound)
                            .role(index == 0
                                    ? ExpertRole.CORE_JUDGE
                                    : ExpertRole.GUEST_JUDGE)
                            .build();
                    expertAssignRepository.save(assignment);
                }
            }
        }
    }

    private Category createCategory(
            HackathonEvent event,
            String categoryName
    ) {
        Category category = new Category();
        category.setCategoryName(categoryName);
        category.setHackathonEvent(event);
        return category;
    }

    private Round createDemoRound(
            HackathonEvent event,
            CriteriaSet criteriaSet,
            String roundName,
            int orderIndex,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int topN
    ) {
        Round round = Round.builder()
                .roundName(roundName)
                .description("Vòng thi demo của " + event.getEventName())
                .startTime(startTime)
                .endTime(endTime)
                .submissionDeadline(endTime.minusHours(4))
                .evaluationDeadline(endTime.minusHours(2))
                .resolveAppealDeadline(endTime.minusHours(1))
                .advancementRule("TOP_N")
                .topN(topN)
                .orderIndex(orderIndex)
                .submissionType(SubmissionType.BOTH)
                .allowedFileType(List.of(FileType.PDF, FileType.ZIP))
                .maxFileCount(3)
                .status(RoundStatus.UPCOMING)
                .criteriaSet(criteriaSet)
                .hackathonEvent(event)
                .build();
        return roundRepository.save(round);
    }

    private void createEvaluationCriteriaForRound(
            Round round,
            CriteriaSet criteriaSet
    ) {
        List<CriteriaDetail> criteriaDetails = criteriaDetailRepository
                .findByCriteriaSet_CriteriaSetId(
                        criteriaSet.getCriteriaSetId());

        if (criteriaDetails.isEmpty()) {
            throw new IllegalStateException(
                    "Criteria set '" + criteriaSet.getCriteriaSetName()
                            + "' chưa có tiêu chí chi tiết");
        }

        List<EvaluationCriteria> evaluationCriteria = criteriaDetails
                .stream()
                .map(detail -> {
                    EvaluationCriteria criterion = new EvaluationCriteria();
                    criterion.setCriteriaName(detail.getCriteriaName());
                    criterion.setDescription(detail.getDescription());
                    criterion.setWeight(detail.getWeight());
                    criterion.setMaxScore(criteriaSet.getMaxScore());
                    criterion.setType(detail.getCriteriaType());
                    criterion.setRound(round);
                    return criterion;
                })
                .toList();
        evaluationCriteriaRepository.saveAll(evaluationCriteria);
    }

    private void createExperts(String password) {
        for (int index = 1;
             index <= EXPERT_COUNT;
             index++) {

            int number = index;

            Account account = createAccountIfAbsent(
                    "expert"
                            + number
                            + "@hackathon.com",
                    String.format("090000002%d", number),
                    password,
                    AccountRole.EXPERT
            );

            boolean profileExists = expertRepository
                    .findByAccount_AccountId(
                            account.getAccountId()
                    )
                    .isPresent();

            if (!profileExists) {
                expertRepository.save(
                        Expert.builder()
                                .expertName(
                                        "Expert " + number
                                )
                                .department(
                                        "Khoa Công nghệ thông tin"
                                )
                                .organization(
                                        "FPT University"
                                )
                                .account(account)
                                .build()
                );
            }
        }
    }

    // =====================================================
    // CRITERIA SET
    // =====================================================

    /**
     * Tạo bốn bộ tiêu chí mẫu cho Event Coordinator đầu tiên.
     * Nếu bộ tiêu chí đã tồn tại thì bỏ qua để không tạo dữ liệu trùng
     * mỗi khi ứng dụng khởi động lại.
     */
    private void createCriteriaSets() {
        EventCoordinator coordinator =
                eventCoordinatorRepository.findAll()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "Không có Event Coordinator để tạo bộ tiêu chí"
                        ));

        createCriteriaSetIfAbsent(
                coordinator,
                "Bộ tiêu chí Ý tưởng và Sáng tạo",
                List.of(
                        criteriaDetail(
                                "Tính sáng tạo",
                                "Đánh giá mức độ mới mẻ và khác biệt của ý tưởng",
                                "40",
                                CriteriaType.SUBMISSION
                        ),
                        criteriaDetail(
                                "Tính phù hợp",
                                "Đánh giá mức độ phù hợp với chủ đề hackathon",
                                "35",
                                CriteriaType.SUBMISSION
                        ),
                        criteriaDetail(
                                "Khả năng phát triển",
                                "Đánh giá tiềm năng mở rộng của sản phẩm",
                                "25",
                                CriteriaType.PRESENTATION
                        )
                )
        );

        createCriteriaSetIfAbsent(
                coordinator,
                "Bộ tiêu chí Kỹ thuật và Chất lượng",
                List.of(
                        criteriaDetail(
                                "Chất lượng kỹ thuật",
                                "Đánh giá kiến trúc, mã nguồn và công nghệ sử dụng",
                                "40",
                                CriteriaType.SUBMISSION
                        ),
                        criteriaDetail(
                                "Mức độ hoàn thiện",
                                "Đánh giá tính ổn định và đầy đủ của chức năng",
                                "35",
                                CriteriaType.SUBMISSION
                        ),
                        criteriaDetail(
                                "Khả năng demo",
                                "Đánh giá khả năng vận hành trong buổi trình bày",
                                "25",
                                CriteriaType.PRESENTATION
                        )
                )
        );

        createCriteriaSetIfAbsent(
                coordinator,
                "Bộ tiêu chí Giá trị và Tác động",
                List.of(
                        criteriaDetail(
                                "Giá trị thực tiễn",
                                "Đánh giá khả năng giải quyết vấn đề thực tế",
                                "40",
                                CriteriaType.SUBMISSION
                        ),
                        criteriaDetail(
                                "Tác động người dùng",
                                "Đánh giá lợi ích sản phẩm mang lại cho người dùng",
                                "30",
                                CriteriaType.SUBMISSION
                        ),
                        criteriaDetail(
                                "Khả năng thương mại",
                                "Đánh giá tiềm năng áp dụng và thương mại hóa",
                                "30",
                                CriteriaType.PRESENTATION
                        )
                )
        );

        createCriteriaSetIfAbsent(
                coordinator,
                "Bộ tiêu chí Trình bày và Demo",
                List.of(
                        criteriaDetail(
                                "Kỹ năng trình bày",
                                "Đánh giá cách truyền đạt rõ ràng và thuyết phục",
                                "40",
                                CriteriaType.PRESENTATION
                        ),
                        criteriaDetail(
                                "Chất lượng demo",
                                "Đánh giá tính trực quan và ổn định của phần demo",
                                "30",
                                CriteriaType.PRESENTATION
                        ),
                        criteriaDetail(
                                "Khả năng phản biện",
                                "Đánh giá cách trả lời câu hỏi của ban giám khảo",
                                "30",
                                CriteriaType.PRESENTATION
                        )
                )
        );
    }

    /**
     * Lưu CriteriaSet trước để có ID, sau đó gắn và lưu các CriteriaDetail.
     * Tổng trọng số của mỗi bộ dữ liệu mẫu là 100.
     */
    private void createCriteriaSetIfAbsent(
            EventCoordinator coordinator,
            String criteriaSetName,
            List<CriteriaDetail> details
    ) {
        if (criteriaSetRepository.existsByCriteriaSetName(
                criteriaSetName
        )) {
            return;
        }

        CriteriaSet criteriaSet = criteriaSetRepository.save(
                CriteriaSet.builder()
                        .criteriaSetName(criteriaSetName)
                        .maxScore(100)
                        .eventCoordinator(coordinator)
                        .build()
        );

        details.forEach(detail ->
                detail.setCriteriaSet(criteriaSet)
        );
        criteriaDetailRepository.saveAll(details);
    }

    /**
     * Tạo CriteriaDetail chưa gắn CriteriaSet.
     * Quan hệ sẽ được thiết lập sau khi CriteriaSet được lưu thành công.
     */
    private CriteriaDetail criteriaDetail(
            String name,
            String description,
            String weight,
            CriteriaType type
    ) {
        return CriteriaDetail.builder()
                .criteriaName(name)
                .description(description)
                .weight(new BigDecimal(weight))
                .criteriaType(type)
                .build();
    }

    // =====================================================
    // STUDENT
    // =====================================================

    private Student[] createStudents(String password) {
        Student[] students =
                new Student[STUDENT_COUNT];

        for (int index = 0;
             index < STUDENT_COUNT;
             index++) {

            int number = index + 1;

            Account account = createAccountIfAbsent(
                    "student"
                            + number
                            + "@hackathon.com",
                    String.format("091%07d", number),
                    password,
                    AccountRole.STUDENT
            );

            String studentCode =
                    "SE"
                            + String.format(
                            "%06d",
                            200001 + index
                    );

            students[index] = studentRepository
                    .findByAccount_AccountId(
                            account.getAccountId()
                    )
                    .orElseGet(() -> studentRepository.save(
                            Student.builder()
                                    .studentCode(studentCode)
                                    .studentName(
                                            "Student " + number
                                    )
                                    .major(
                                            "Software Engineering"
                                    )
                                    .account(account)
                                    .build()
                    ));
        }

        return students;
    }

    // =====================================================
    // TEAM
    // =====================================================

    private Team[] createTeams() {
        Team[] teams = new Team[TEAM_COUNT];

        /*
         * Lấy toàn bộ student từ database rồi loại những student đã thuộc
         * một team. Danh sách còn lại chỉ chứa student có thể được chia team.
         */
        List<Student> availableStudents =
                new ArrayList<>(studentRepository.findAll());
        availableStudents.removeIf(student ->
                !teamMemberRepository
                        .findByStudent(student)
                        .isEmpty()
        );

        int studentIndex = 0;

        for (int teamIndex = 0;
             teamIndex < TEAM_COUNT;
             teamIndex++) {

            String teamName =
                    "Team " + (teamIndex + 1);

            Team team = teamRepository.findAll()
                    .stream()
                    .filter(item ->
                            teamName.equalsIgnoreCase(
                                    item.getTeamName()
                            )
                    )
                    .findFirst()
                    .orElseGet(() -> teamRepository.save(
                            Team.builder()
                                    .teamName(teamName)
                                    .teamSize(
                                            MEMBER_PER_TEAM
                                    )
                                    .status(TeamStatus.ACTIVE)
                                    .build()
                    ));

            teams[teamIndex] = team;

            boolean alreadyHasMembers =
                    !teamMemberRepository
                            .findByTeam(team)
                            .isEmpty();

            if (alreadyHasMembers) {
                continue;
            }

            /*
             * Giữ logic cũ: lấy lần lượt MEMBER_PER_TEAM student trong
             * danh sách đã được lọc để thêm vào team hiện tại.
             */
            for (int memberIndex = 0;
                 memberIndex < MEMBER_PER_TEAM;
                 memberIndex++) {

                if (studentIndex >= availableStudents.size()) {
                    throw new IllegalStateException(
                            "Không đủ student chưa có team để tạo "
                                    + teamName
                    );
                }

                Student student =
                        availableStudents.get(studentIndex++);

                teamMemberRepository.save(
                        TeamMember.builder()
                                .team(team)
                                .student(student)
                                .isLeader(
                                        memberIndex == 0
                                )
                                .build()
                );
            }
        }

        return teams;
    }

    // =====================================================
    // REGISTRATION
    // =====================================================

    /**
     * Đăng ký các team hiện có trong database vào event.
     * Có thể chạy riêng mà không cần gọi createTeams() trước đó.
     */
    private void registerTeamsForEvent(
            Integer eventId
    ) {
        HackathonEvent event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy event ID: "
                                + eventId
                ));

        /*
         * Lấy team trực tiếp từ database để hàm đăng ký có thể chạy
         * độc lập, không phụ thuộc vào mảng trả về từ createTeams().
         */
        List<Team> teams = teamRepository.findAll()
                .stream()
                .sorted(Comparator.comparingInt(
                        Team::getTeamId
                ))
                .toList();

        if (teams.isEmpty()) {
            System.out.println(
                    "Không có team trong database để đăng ký"
            );
            return;
        }

        int createdCount = 0;
        int skippedCount = 0;

        for (Team team : teams) {
            boolean alreadyRegistered =
                    registrationRepository
                            .findByTeamAndHackathonEvent_EventId(
                                    team,
                                    eventId
                            )
                            .isPresent();

            if (alreadyRegistered) {
                skippedCount++;
                continue;
            }

            registrationRepository.save(
                    Registration.builder()
                            .team(team)
                            .hackathonEvent(event)
                            .registrationDate(
                                    LocalDateTime.now()
                            )
                            .status(
                                    RegistrationStatus.PENDING
                            )
                            .build()
            );

            team.setStatus(TeamStatus.PENDING);
            teamRepository.save(team);

            createdCount++;
        }

        System.out.println(
                "Registration vừa tạo: " + createdCount
        );
        System.out.println(
                "Registration được bỏ qua: "
                        + skippedCount
        );
    }

    // =====================================================
    // SUBMISSION
    // =====================================================

    /**
     * Lấy TeamParticipant từ database theo event của round và chỉ tạo bài
     * cho participant đã được duyệt, thuộc đúng round, chưa có bài cuối.
     * Vì vậy hàm có thể chạy riêng, không cần danh sách team trên RAM.
     */
    private void submitForParticipantsWithoutSubmission(
            Integer roundId
    ) {
        Round round = roundRepository
                .findById(roundId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy round ID: "
                                + roundId
                ));

        Integer eventId =
                round.getHackathonEvent().getEventId();

        List<TeamParticipant> participants =
                participantRepository
                        .findAllByRegistration_HackathonEvent_EventIdAndCategoryRoundIsNotNull(
                                eventId
                        );

        int createdCount = 0;
        int skippedCount = 0;

        for (TeamParticipant participant : participants) {
            CategoryRound categoryRound =
                    participant.getCategoryRound();

            if (categoryRound.getRound() == null
                    || !roundId.equals(
                    categoryRound.getRound()
                            .getRoundId()
            )) {
                continue;
            }

            Registration registration =
                    participant.getRegistration();

            if (registration == null
                    || registration.getStatus()
                    != RegistrationStatus.APPROVED) {
                skippedCount++;
                continue;
            }

            Team team = registration.getTeam();

            Submission existingSubmission =
                    submissionRepository.findFinalSubmission(
                            categoryRound.getCategoryRoundId(),
                            team.getTeamId()
                    );

            if (existingSubmission != null) {
                skippedCount++;

                if (participant.getSubmissionStatus()
                        != SubmissionStatus.SUBMITTED) {
                    participant.setSubmissionStatus(
                            SubmissionStatus.SUBMITTED
                    );
                    participantRepository.save(
                            participant
                    );
                }

                continue;
            }

            submissionRepository.save(
                    Submission.builder()
                            .createAt(LocalDateTime.now())
                            .description(
                                    "Bài nộp demo của "
                                            + team.getTeamName()
                            )
                            .githubUrl(
                                    "https://github.com/"
                                            + "hackathon-demo/team-"
                                            + team.getTeamId()
                                            + "/round-"
                                            + roundId
                            )
                            .latestCommitSha(
                                    "demo-commit-team-"
                                            + team.getTeamId()
                                            + "-round-"
                                            + roundId
                            )
                            .isFinal(true)
                            .team(team)
                            .teamParticipant(participant)
                            .build()
            );

            participant.setSubmissionStatus(
                    SubmissionStatus.SUBMITTED
            );
            participantRepository.save(participant);

            createdCount++;
        }

        System.out.println(
                "Submission vừa tạo: " + createdCount
        );
        System.out.println(
                "Participant được bỏ qua: "
                        + skippedCount
        );
    }

    // =====================================================
    // GRADING
    // =====================================================

    /**
     * Lấy tiêu chí, final submission và ExpertAssign trực tiếp từ database
     * theo roundId; không phụ thuộc vào bước tạo team hoặc đăng ký event.
     */
    private void gradeSubmissionsByAssignments(
            Integer roundId
    ) {
        Round round = roundRepository
                .findById(roundId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy round ID: "
                                + roundId
                ));

        List<EvaluationCriteria> criteria =
                evaluationCriteriaRepository
                        .findByRound_RoundId(roundId);

        if (criteria == null || criteria.isEmpty()) {
            throw new RuntimeException(
                    "Round chưa có EvaluationCriteria"
            );
        }

        List<CategoryRound> categoryRounds =
                categoryRoundRepository
                        .findCategoryRoundByRound_RoundId(
                                roundId
                        );

        /*
         * Gộp tất cả final submission của round,
         * sau đó sắp xếp theo team ID.
         *
         * teamOrder được dùng để tạo khoảng cách điểm
         * cố định giữa các team.
         */
        List<Submission> submissions =
                new ArrayList<>();

        for (CategoryRound categoryRound
                : categoryRounds) {
            submissions.addAll(
                    submissionRepository
                            .findFinalSubmissionsByCategoryRoundId(
                                    categoryRound
                                            .getCategoryRoundId()
                            )
            );
        }

        submissions.sort(
                Comparator.comparingInt(
                        submission ->
                                submission.getTeam()
                                        .getTeamId()
                )
        );

        int createdCount = 0;
        int skippedCount = 0;

        for (int teamOrder = 0;
             teamOrder < submissions.size();
             teamOrder++) {

            Submission submission =
                    submissions.get(teamOrder);

            CategoryRound categoryRound =
                    submission.getTeamParticipant()
                            .getCategoryRound();

            List<ExpertAssign> assignments =
                    expertAssignRepository
                            .findByCategoryRoundId(
                                    categoryRound
                                            .getCategoryRoundId()
                            )
                            .stream()
                            .filter(assign ->
                                    assign.getRole()
                                            == ExpertRole.CORE_JUDGE
                                            || assign.getRole()
                                            == ExpertRole.GUEST_JUDGE
                            )
                            .toList();

            for (int judgeOrder = 0;
                 judgeOrder < assignments.size();
                 judgeOrder++) {

                ExpertAssign assignment =
                        assignments.get(judgeOrder);

                Optional<Evaluation> existingEvaluation =
                        evaluationRepository
                                .findByExpertAssignIdAndSubmissionId(
                                        assignment.getAssignId(),
                                        submission.getSubmissionId()
                                );

                if (existingEvaluation.isPresent()) {
                    saveInitialAuditAttemptsIfMissing(
                            existingEvaluation.get(),
                            criteria
                    );
                    skippedCount++;
                    continue;
                }

                createGradedEvaluation(
                        submission,
                        assignment,
                        criteria,
                        teamOrder,
                        judgeOrder
                );

                createdCount++;
            }
        }

        System.out.println(
                "Evaluation vừa tạo: " + createdCount
        );
        System.out.println(
                "Evaluation được bỏ qua: "
                        + skippedCount
        );
    }

    private void createGradedEvaluation(
            Submission submission,
            ExpertAssign assignment,
            List<EvaluationCriteria> criteria,
            int teamOrder,
            int judgeOrder
    ) {
        Evaluation evaluation = new Evaluation();

        evaluation.setSubmission(submission);
        evaluation.setExpertAssign(assignment);
        evaluation.setStatus(
                EvaluationStatus.GRADED
        );
        evaluation.setIsReEvaluation(false);
        evaluation.setComment(
                "Điểm chấm demo của "
                        + assignment.getExpert()
                        .getExpertName()
        );

        List<EvaluationDetail> details =
                new ArrayList<>();

        for (int criteriaIndex = 0;
             criteriaIndex < criteria.size();
             criteriaIndex++) {

            EvaluationCriteria criterion =
                    criteria.get(criteriaIndex);

            BigDecimal score = generateUniqueScore(
                    criterion,
                    teamOrder,
                    judgeOrder,
                    criteriaIndex
            );

            EvaluationDetail detail =
                    new EvaluationDetail();

            detail.setEvaluation(evaluation);
            detail.setEvaluationCriteria(criterion);
            detail.setScore(score);
            detail.setComment(
                    "Nhận xét demo cho "
                            + criterion.getCriteriaName()
            );
            detail.setIsReEvaluation(false);

            details.add(detail);
        }

        evaluation.setEvaluationDetails(details);

        BigDecimal totalScore =
                scoreCalculator.calculateWeightedTotal(
                        details
                );

        evaluation.setScore(totalScore);

        /*
         * Evaluation cascade ALL xuống EvaluationDetail.
         */
        Evaluation savedEvaluation = evaluationRepository.save(evaluation);
        saveInitialAuditAttemptsIfMissing(savedEvaluation, criteria);

        System.out.println(
                submission.getTeam().getTeamName()
                        + " - "
                        + assignment.getExpert()
                        .getExpertName()
                        + " = "
                        + totalScore
        );
    }

    private void saveInitialAuditAttemptsIfMissing(
            Evaluation evaluation,
            List<EvaluationCriteria> criteria
    ) {
        if (evaluationAuditLogRepository
                .countByEvaluation_EvaluationId(
                        evaluation.getEvaluationId()) > 0) {
            return;
        }

        Account judgeAccount = evaluation.getExpertAssign()
                .getExpert()
                .getAccount();

        criteria.stream()
                .map(EvaluationCriteria::getType)
                .filter(type -> type != null)
                .distinct()
                .forEach(type -> evaluationAuditLogServiceImpl.saveAttempt(
                        judgeAccount,
                        evaluation,
                        type,
                        false
                ));
    }

    /**
     * Tạo điểm không bằng nhau giữa các team.
     *
     * Mỗi team kế tiếp tăng ít nhất 0.30 điểm.
     * Mỗi tiêu chí và giám khảo chỉ cộng thêm lượng nhỏ,
     * không làm mất khoảng cách chính giữa các team.
     */
    private BigDecimal generateUniqueScore(
            EvaluationCriteria criterion,
            int teamOrder,
            int judgeOrder,
            int criteriaIndex
    ) {
        BigDecimal teamOffset =
                new BigDecimal("0.30")
                        .multiply(
                                BigDecimal.valueOf(teamOrder)
                        );

        BigDecimal judgeOffset =
                new BigDecimal("0.05")
                        .multiply(
                                BigDecimal.valueOf(judgeOrder)
                        );

        BigDecimal criteriaOffset =
                new BigDecimal("0.02")
                        .multiply(
                                BigDecimal.valueOf(
                                        criteriaIndex
                                )
                        );

        BigDecimal score =
                new BigDecimal("5.00")
                        .add(teamOffset)
                        .add(judgeOffset)
                        .add(criteriaOffset);

        /*
         * Không vượt quá maxScore của tiêu chí.
         * Nếu maxScore chưa cấu hình thì mặc định tối đa 10.
         */
        BigDecimal maximumScore =
                criterion.getMaxScore() > 0
                        ? BigDecimal.valueOf(
                        criterion.getMaxScore()
                )
                        : BigDecimal.TEN;

        return score.min(maximumScore);
    }

}
