package hackthon.mangaement.hackathon.dto;

import lombok.Data;

@Data
public class AdminMergeTeamRequest {
    private Integer sourceTeamId;
    private Integer targetTeamId;
}
