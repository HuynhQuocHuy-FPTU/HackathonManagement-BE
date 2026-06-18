package com.hackathon.dto.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class CriteriaSetResponseDTO {
    private Integer criteriaSetId;
<<<<<<< HEAD
    private String criteriaSetName;
    private Integer maxScore;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CriteriaDetailResponseDTO> criteriaDetails;

=======
    private String criteriaName;
    private Integer maxScore;
>>>>>>> 9531c5707e665b72c6b01f18dd63c66d5df8fca1

}