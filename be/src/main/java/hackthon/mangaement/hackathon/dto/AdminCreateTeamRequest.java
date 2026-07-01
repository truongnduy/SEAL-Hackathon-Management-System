package hackthon.mangaement.hackathon.dto;

import lombok.Data;

@Data
public class AdminCreateTeamRequest {
    private String name;
    private Integer leaderId;
    private Integer hackathonId;
    
}
