package com.hackathon.service;

import com.hackathon.dto.event.EventDescription;
import com.hackathon.dto.history.ExpertHistoryResponse;
import com.hackathon.dto.history.StudentHistoryResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.ExpertRole;
import com.hackathon.entity.enums.ExpertType;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    //    private final StudentRepository studentRepository;
//    private final TeamRepository teamRepository;
//    private final HackathonEventRepository hackathonEventRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RegistrationRepository registrationRepository;
    private final ParticipantRepository participantRepository;


    @Override
    public StudentHistoryResponse studentHistory(Integer accountId, CustomUserDetails userDetails) {

        //1. Tìm thông tin Student qua Account
        Account currentAccount = userDetails.getAccount();
        if (currentAccount.getRole() != AccountRole.STUDENT
                && currentAccount.getRole() != AccountRole.EVENTCOORDINATOR) {

            throw new BadRequestException("Bạn không có quyền xem lịch sử này.");
        }
        //  Nếu là Sinh viên, CHỈ được xem chính mình. Admin/Coordinator xem ai cũng được.
        if (currentAccount.getRole() == AccountRole.EVENTCOORDINATOR&& accountId == null) {
            throw new BadRequestException("Vui lòng nhập account Id để xem thông tin của student.");
        }
        if(currentAccount.getRole() == AccountRole.STUDENT){
            accountId = currentAccount.getAccountId();
        }
        Account accStudent = accountRepository.findById(accountId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản này."));
        Student student = accStudent.getStudent();
        if (student == null) {
            throw new BadRequestException("Tài khoản này không phải tài khoản của sinh viên.");
        }
        //2. Lấy thông tin chung của student

        StudentHistoryResponse historyResponse = new StudentHistoryResponse();
        historyResponse.setStudentName(student.getStudentName());
        historyResponse.setUniversityName(student.getUniversityName());
        historyResponse.setCreatAt(accStudent.getCreatedAt());

        List<TeamMember> teamMember = teamMemberRepository.findByStudent(student);
        //3. Lấy ds teamMember
        List<StudentHistoryResponse.StudentHistory> historyList = new ArrayList<>();
        for (TeamMember tm : teamMember) {
            if (tm == null || tm.getTeam() == null) {
                continue;
            }
            // lấy ds Team thông qua Team Member
            Team team = tm.getTeam();
            List<Registration> regis = registrationRepository.findByTeam(team);

            for (Registration registration : regis) {

                List<TeamParticipant> participantList =registration.getParticipant();
                if (participantList == null || participantList.isEmpty()) {
                    StudentHistoryResponse.StudentHistory historyStudent = new StudentHistoryResponse.StudentHistory();

                    // Vẫn gán các thông tin cơ bản của Event để không bị trống data lịch sử
                    historyStudent.setEventName(registration.getHackathonEvent().getEventName());
                    historyStudent.setEventId(registration.getHackathonEvent().getEventId());
                    historyStudent.setTeamName(tm.getTeam().getTeamName());
                    historyStudent.setLeader(tm.getIsLeader());
                    historyStudent.setStatus(registration.getStatus());
                    historyStudent.setRegistrationDate(registration.getRegistrationDate());
                    historyStudent.setCategoryName(null);
                    historyStudent.setRoundName(null);
                    historyStudent.setRanking(null);
                    historyList.add(historyStudent);
                    continue;
                }
                for(TeamParticipant participant :participantList){
                    if (participant == null) continue;
                    StudentHistoryResponse.StudentHistory historyStudent = new StudentHistoryResponse.StudentHistory();
                    historyStudent.setEventName(registration.getHackathonEvent().getEventName());
                    historyStudent.setEventId(registration.getHackathonEvent().getEventId());
                    historyStudent.setTeamName(tm.getTeam().getTeamName());
                    historyStudent.setLeader(tm.getIsLeader());
                    historyStudent.setStatus(registration.getStatus());
                    historyStudent.setRegistrationDate(registration.getRegistrationDate());
                    if (participant.getCategoryRound()!= null) {
                        if (participant.getCategoryRound().getCategory() != null) {
                            historyStudent.setCategoryName(participant.getCategoryRound().getCategory().getCategoryName());
                        }
                        if (participant.getCategoryRound().getRound() != null) {
                            historyStudent.setRoundName(participant.getCategoryRound().getRound().getRoundName());
                        }
                    }
                    if (participant.getRank() != null) {
                        historyStudent.setRanking(participant.getRank());
                    } else {
                        historyStudent.setRanking(null);
                    }

//                historyStudent.setReward("");
                    historyList.add(historyStudent);
                }

            }

        }
        return new StudentHistoryResponse(
                historyResponse.getStudentName(),
                historyResponse.getUniversityName(),
                historyResponse.getCreatAt(),
                historyList
        );
    }

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
            String season = event.getSeason();

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
                .type(expert.getType()).histories(histories).build();
    }

}
