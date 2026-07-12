package com.hackathon.service;

import com.hackathon.repository.EvaluationRepository;
import com.hackathon.repository.EventCoordinatorRepository;
import com.hackathon.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EvaluationServiceImpl implements EvaluationService {
//    private final EventCoordinatorRepository eventCoordinatorRepository;
//    private final RoundRepository roundRepository;
//    private final EvaluationRepository evaluationRepository;

//    @Override
//    public void checkJudgesCompletedEvaluation(CustomUserDetails userDetails, Integer roundId) {
//        Account account = userDetails.getAccount();
//        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
//                .orElseThrow(() -> new BadRequestException("Bạn không phải là Event Coordinator. Vì vậy bạn không được phép truy cập."));
//
//        Round round = roundRepository.findById(roundId)
//                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này"));
//
//        List<CategoryRound> categoryRound = round.getCategoryRounds();
//        if (categoryRound == null || categoryRound.isEmpty()) {
//            throw new BadRequestException("Không tìm thấy hạng mục nào trong vòng thi này");
//        }
//
//        // Kiểm tra trong tất cả hạng mục, các Team đã có điểm thi chưa
//
//        for (CategoryRound cr : categoryRound) {
//
//            for (TeamParticipant teamParticipant : cr.getTeamParticipants()) {
//                // Chỉ kiểm tra các đội còn tham gia
//                if (teamParticipant.getStatus() == ParticipantStatus.DISQUALIFIED
//                        || teamParticipant.getStatus() == ParticipantStatus.FAILED
//                        || teamParticipant.getStatus() == ParticipantStatus.WITHDRAWN) {
//                    continue;
//                }
//
//                // Check ban giám khảo đã chấm điểm hết chưa
//                if (teamParticipant.getEvaluations() == null || teamParticipant.getEvaluations().isEmpty()) {
//                    throw new BadRequestException(String.format(" Đội '%s' chưa có dữ liệu đánh giá ở hạng mục '%s'."
//                            , teamParticipant.getRegistration().getTeam().getTeamName()
//                            , cr.getCategory().getCategoryName()));
//                }
//                // check giám khảo submit bài nộp chưa
//                for (Evaluation evaluation : teamParticipant.getEvaluations()) {
//                    if (evaluation.getStatus() == EvaluationStatus.NOT_GRADED) {
//                        throw new BadRequestException("Ban giám khảo chưa thực hiện xong quá trình chấm điểm");
//                    }
//                }
//            }
//        }
//    }
}