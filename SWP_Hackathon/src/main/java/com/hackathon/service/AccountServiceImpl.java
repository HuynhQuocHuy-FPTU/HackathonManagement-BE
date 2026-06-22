package com.hackathon.service;

import com.hackathon.dto.student.StudentHistoryResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AccountRole;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
//    private final StudentRepository studentRepository;
//    private final TeamRepository teamRepository;
//    private final HackathonEventRepository hackathonEventRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RegistrationRepository registrationRepository;
    private  final ParticipantRepository participantRepository;


    // Cần bổ sung thêm tham gia round nào , hạng mục nào
    @Override
    public StudentHistoryResponse studentHistory(Integer accountId, CustomUserDetails userDetails) {

        //1. Tìm thông tin Student qua Account
        Account currentAccount = userDetails.getAccount();

        if (currentAccount.getRole() != AccountRole.STUDENT
                && currentAccount.getRole() != AccountRole.ADMIN
                && currentAccount.getRole() != AccountRole.EVENTCOORDINATOR) {

            throw new BadRequestException("Bạn không có quyền xem lịch sử này.");
        }
        //  Nếu là Sinh viên, CHỈ được xem chính mình. Admin/Coordinator xem ai cũng được.
        if (currentAccount.getRole() == AccountRole.STUDENT && currentAccount.getAccountId() != accountId) {
            throw new BadRequestException("Bạn không thể xem lịch sử của sinh viên khác.");
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
                StudentHistoryResponse.StudentHistory historyStudent = new StudentHistoryResponse.StudentHistory();
                historyStudent.setEventName(registration.getHackathonEvent().getEventName());
                historyStudent.setTeamName(tm.getTeam().getTeamName());
                historyStudent.setLeader(tm.getIsLeader());
                historyStudent.setStatus(registration.getStatus());
                historyStudent.setRegistrationDate(registration.getRegistrationDate());

                Optional<Participant> participant = participantRepository.findByRegistration(registration);
                if(participant.isPresent()){
                    Participant parti = participant.get();
                    if(parti.getRank() != null ){
                        historyStudent.setRanking(parti.getRank());
                    }else{
                        historyStudent.setRanking(null);

                    }
                }
//                historyStudent.setReward("");
                historyList.add(historyStudent);

            }

        }
        return new StudentHistoryResponse(
                historyResponse.getStudentName(),
                historyResponse.getUniversityName(),
                historyResponse.getCreatAt(),
                historyList
        );
    }

}
