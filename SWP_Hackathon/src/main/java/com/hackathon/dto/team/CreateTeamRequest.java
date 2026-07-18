package com.hackathon.dto.team;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateTeamRequest {
//    private Integer teamId;
    private String teamName;
    @NotNull(message = "Danh sách thành viên là bắt buộc")
    @Size(min = 1, message = "Đội phải có ít nhất 1 thành viên ngoài đội trưởng")
    private List<@NotBlank @Email String> memberEmails;

}
