package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamResponse;

import com.hackathon.entity.*;
import com.hackathon.entity.enums.TeamStatus;
import com.hackathon.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeamServiceImpl implements TeamService {
    @Autowired
    private HackathonEventRepository eventRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private AccountRepository accRepository;
    @Autowired
    private TeamMemberRepository teamMemberRepository;
    @Autowired
    private RegistrationRepository registrationRepository;

    //FUNCTION 1:Create Team (Nguoi tao team se dc gan role la leader)
    @Transactional
    @Override
    public TeamResponse createTeam(CreateTeamRequest request) {
        //1. Get event
        HackathonEvent event = eventRepository.findById(request.getEventID()).orElseThrow(() -> new RuntimeException("Not found event with ID:" + request.getEventID()));
        //2. Get current account(Bo sung sau, sua lai khi co account)
        Account currentAccount = accRepository.findById(request.getEventID()).orElseThrow(() -> new RuntimeException("Not found account with ID:" + request.getEventID()));

        //3. Create Team
        Team team = new Team();
        team.setTeamName(request.getTeamName());
        team.setStatus(TeamStatus.PENDING);
        team.setTeamSize(1);// moi tao chi co leader nen la  1
        Team saveTeam = teamRepository.save(team);

        //4. Gán leader
        TeamMember leaderMember = new TeamMember();
        leaderMember.setTeam(team);
        leaderMember.setIsLeader(true);
        leaderMember.setStudent(currentAccount.getStudent());
        TeamMember saveLeader = teamMemberRepository.save(leaderMember);

        // Tạo  Registration để link Team với Event
        Registration registration = new Registration();
        registration.setTeam(saveTeam);   // Gán team vừa tạo vào
        registration.setHackathonEvent(event);     // Gán event tìm được vào
        registration.setRegistrationDate(LocalDateTime.now());
        registrationRepository.save(registration);

        //5. Tạo object save infor of leader
        TeamResponse.MemberInfo leaderInfo = new TeamResponse.MemberInfo(
                currentAccount.getStudent().getStudentId(),
                currentAccount.getStudent().getStudentCode(),
                currentAccount.getStudent().getStudentName(),
                currentAccount.getStudent().getAccount().getEmail()
        );
        List<TeamResponse.MemberInfo> members = List.of(leaderInfo);

        //6. Return TeamResponse
        return new TeamResponse(
                saveTeam.getTeamId(),
                saveTeam.getTeamName(),
                event.getEventId(),
                event.getTitle(),
                leaderInfo, members,
                saveTeam.getCreateAt()

        );


    }

    //FUNCTION 2:UPDATE INFORMATION ABOUT TEAM AS NAME
    @Override
    public void updateInfo(CreateTeamRequest request, Integer teamId, Integer accId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // 1. Get event từ registration
        Registration registration = registrationRepository.findByTeam(team)
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        HackathonEvent event = registration.getHackathonEvent();

        // 2. Check deadline
        if (LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new RuntimeException("Deadline passed, cannot update team");
        }

        // 3. Check leader
        TeamMember leader = teamMemberRepository
                .findByTeamAndIsLeaderTrue(team)
                .orElseThrow(() -> new RuntimeException("Leader not found"));

        if (leader.getStudent().getAccount().getAccountId() == accId) {
            throw new RuntimeException("User is not leader");
        }

        // 4. Update
        team.setTeamName(request.getTeamName());

        teamRepository.save(team);
    }
}
