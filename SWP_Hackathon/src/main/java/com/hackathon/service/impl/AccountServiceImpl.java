package com.hackathon.service.impl;

import com.hackathon.dto.history.ExpertHistoryResponse;
import com.hackathon.dto.history.StudentHistoryResponse;
import com.hackathon.dto.round.RoundStatusDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
// Tổng hợp lịch sử tham gia sự kiện của sinh viên và lịch sử chấm thi của chuyên gia.
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final StudentRepository studentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RegistrationRepository registrationRepository;


    // Kiểm tra quyền truy cập rồi tổng hợp đội, sự kiện, vòng thi và kết quả của sinh viên.
    // Dữ liệu được gom theo từng lần đăng ký để trả về đầy đủ quá trình tham gia cuộc thi.
    @Override
    public StudentHistoryResponse studentHistory(Integer studentId, CustomUserDetails userDetails) {
        Account currentAccount = userDetails.getAccount();

        //  Nếu là Sinh viên, CHỈ được xem chính mình. Coordinator xem ai cũng được.
        if (currentAccount.getRole() == AccountRole.STUDENT) {
            studentId = currentAccount.getStudent().getStudentId();
        } else if (currentAccount.getRole() == AccountRole.EVENTCOORDINATOR) {
            if (studentId == null) {
                throw new BadRequestException("Vui lòng nhập studentId để xem thông tin của sinh viên.");
            }
        } else {
            throw new BadRequestException("Bạn không có quyền xem lịch sử này");
        }

        Student accStudent = studentRepository.findById(studentId)
                .orElseThrow(() -> new BadRequestException("Tài khoản này không phải tài khoản của sinh viên."));

        //2. Lấmy thông tin chung của student
        StudentHistoryResponse historyResponse = new StudentHistoryResponse();
        historyResponse.setStudentName(accStudent.getStudentName());
        historyResponse.setUniversityName(accStudent.getUniversityName());
        historyResponse.setCreatAt(currentAccount.getCreatedAt());

        List<TeamMember> teamMember = teamMemberRepository.findByStudent(accStudent);
        //3. Lấy ds teamMember
        Map<Integer, StudentHistoryResponse.StudentHistory> historyMap = new HashMap<>();
        for (TeamMember tm : teamMember) {
            if (tm == null || tm.getTeam() == null) {
                continue;
            }
            // lấy ds Team thông qua Team Member
            Team team = tm.getTeam();
            List<Registration> regis = registrationRepository.findByTeam(team);

            for (Registration registration : regis) {

                if (registration != null && registration.getHackathonEvent() != null) {

                    Integer eventId = registration.getHackathonEvent().getEventId();
                    if (!historyMap.containsKey(eventId)) {
                        StudentHistoryResponse.StudentHistory historyStudent = new StudentHistoryResponse.StudentHistory();

                        historyStudent.setEventName(registration.getHackathonEvent().getEventName());
                        historyStudent.setEventId(registration.getHackathonEvent().getEventId());
                        historyStudent.setTeamName(tm.getTeam().getTeamName());
                        historyStudent.setLeader(tm.getIsLeader());
                        historyStudent.setStatus(registration.getStatus());
                        historyStudent.setRegistrationDate(registration.getRegistrationDate());
                        String award = null;
                        Integer ranking = null;

                        List<RoundStatusDTO> listRounds = new ArrayList<>();
                        List<TeamParticipant> participantList = registration.getParticipants();

                        if (participantList != null && !participantList.isEmpty()) {
                            for (TeamParticipant tp : participantList) {
                                if (tp == null) continue;
                                if (tp.getCategoryRound() != null && tp.getCategoryRound().getRound() != null) {
                                    Round round = tp.getCategoryRound().getRound();
                                    String categoryName = tp.getCategoryRound().getCategory() != null ?
                                            tp.getCategoryRound().getCategory().getCategoryName() : "N/A";

                                    RoundStatusDTO roundStatusDTO = RoundStatusDTO.builder()
                                            .roundId(round.getRoundId())
                                            .categoryName(categoryName)
                                            .roundName(round.getRoundName())
                                            .status(tp.getStatus())
                                            .build();
                                    listRounds.add(roundStatusDTO);
                                }
                                if (tp.getRank() != null) {
                                    ranking = tp.getRank();
                                }
                                if (tp.getTitleAward() != null) {
                                    award = tp.getTitleAward();
                                }

                            }

                        }
                        historyStudent.setListRounds(listRounds);
                        historyStudent.setRanking(ranking);
                        historyStudent.setReward(award);
                        historyMap.put(eventId, historyStudent);
                    }
                }

            }
        }
        historyResponse.setList(new ArrayList<>(historyMap.values()));
        return historyResponse;
    }


    // Kiểm tra tài khoản chuyên gia và lấy toàn bộ lịch sử được phân công chấm bài.
    // Kết quả bao gồm thông tin sự kiện, vòng thi, vai trò và trạng thái đánh giá tương ứng.
    @Override
    public ExpertHistoryResponse expertHistory(Integer accountId, CustomUserDetails userDetails) {

        Account currentAccount = userDetails.getAccount();
        if (currentAccount.getRole() != AccountRole.EXPERT
                && currentAccount.getRole() != AccountRole.EVENTCOORDINATOR) {
            throw new BadRequestException("Bạn không có quyền xem lịch sử này.");
        }
        if (currentAccount.getRole() == AccountRole.EXPERT) {
            accountId = currentAccount.getAccountId();
        }

        if (currentAccount.getRole() == AccountRole.EVENTCOORDINATOR
                && accountId == null) {
            throw new BadRequestException("Vui lòng chọn Expert.");
        }

        Account accExpert = accountRepository.findById(accountId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản này."));
        Expert expert = accExpert.getExpert();
        if (expert == null) {
            throw new BadRequestException("Tài khoản này không phải tài khoản của Expert.");
        }


        List<ExpertHistoryResponse.ExpertHistoryDetail> histories = new ArrayList<>();

        List<ExpertAssign> expertAssign = expert.getExpertAssigns();
        for (ExpertAssign ex : expertAssign) {

            CategoryRound cr = ex.getCategoryRound();
            HackathonEvent event = cr.getRound().getHackathonEvent();
            Integer eventId = event.getEventId();
            String eventName = event.getEventName() != null ? event.getEventName() : "N/A";
            String season = event.getSeason() != null ? event.getSeason().name() : null;

            ExpertHistoryResponse.ExpertHistoryDetail response = ExpertHistoryResponse.ExpertHistoryDetail.builder()
                    .eventId(eventId)
                    .eventName(eventName)
                    .roundId(ex.getCategoryRound().getRound().getRoundId())
                    .categoryId(ex.getCategoryRound().getCategory().getCategoryId())
                    .roundName(ex.getCategoryRound().getRound().getRoundName())
                    .season(season)
                    .categoryName(ex.getCategoryRound().getCategory().getCategoryName())
                    .expertRole(ex.getRole()).build();

            histories.add(response);

        }

        return ExpertHistoryResponse.builder()
                .expertId(expert.getExpertId())
                .expertName(expert.getExpertName())
                .department(expert.getDepartment())
                .histories(histories).build();
    }

}
