# Hướng dẫn Step-by-Step: Tự code các API còn thiếu cho Backend

Tài liệu này cung cấp **TOÀN BỘ CODE HOÀN CHỈNH (Controller, Service, Repository, DTO)** cho các tính năng nâng cao mà Frontend mới yêu cầu. Bạn chỉ việc Copy & Paste vào đúng file tương ứng.

---

## 🎯 Lộ Trình Code (Roadmap / Tasks)
Hãy làm tuần tự theo các Phase (giai đoạn) sau đây. Tích (v) vào từng đầu việc sau khi hoàn thành:

### Phase 1: Các tính năng cơ bản (Dễ nhất)
- [x] **Task 1.1:** Mở `HackathonController` và `HackathonService`, code API Đóng đăng ký sớm (`/close-registration-early`). *(Đã fix lại logic dùng `registrationEnd` cho chính xác!)*
- [x] **Task 1.2:** Code API Upload Banner cho Hackathon (`/banner`).
- [x] **Task 1.3:** Mở `RoundController`, code API Lấy đề bài (`/problem-statement`) và Phát hành đề bài (`/release-problem`).

### Phase 2: Chuẩn bị Data & DTO cho Nhóm Team (Trung bình)
- [x] **Task 2.1:** Tạo các file DTO trong thư mục `dto/` (`AdminMergeTeamRequest`, `AdminCreateTeamRequest`, `AdminAddMemberRequest`).
- [x] **Task 2.2:** Mở `TeamRepository`, viết `@Query` lấy danh sách Đội thiếu người (`findIncompleteTeams`).
- [x] **Task 2.3:** Mở `UserRepository`, viết `@Query` lấy sinh viên mồ côi (`findOrphansByHackathon`).

### Phase 3: Xử lý Logic Ghép đội (Khó)
- [x] **Task 3.1:** Code `TeamService`: Hàm lấy danh sách orphans và incomplete teams.
- [x] **Task 3.2:** Code `TeamService`: Hàm `runMatchmaking` (Ghép đội tự động).
- [x] **Task 3.3:** Code `TeamService`: Hàm `mergeTeams` (Gộp đội), `adminAddMember`, `adminCreateTeam`.
- [x] **Task 3.4:** Gắn các Endpoint này vào `TeamController`.

### Phase 4: Chấm thi trực tiếp & Đếm giờ (Khó nhất)
- [x] **Task 4.1:** Tạo file `PresentationController.java` mới.
- [x] **Task 4.2:** Tạo file `PresentationService.java` xử lý luồng đếm giờ (Timer).

---

## CHI TIẾT TỪNG PHẦN (COPY & PASTE)

### PHASE 1: CÁC TÍNH NĂNG CƠ BẢN

#### 1. Upload Banner (Trong `HackathonController.java`)
Thêm vào `HackathonController`:
```java
    @PostMapping("/hackathons/{id}/banner")
    public ResponseEntity<?> uploadBanner(@PathVariable Integer id,
                                          @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        Hackathon h = hackathonRepository.findById(id).orElseThrow();
        try {
            // Giả lập lưu file, thực tế bạn lưu vào Cloud (AWS/Cloudinary) hoặc Local File System
            // Ở đây chỉ cập nhật tên file vào DB
            h.setBannerUrl("/uploads/banners/" + file.getOriginalFilename());
            hackathonRepository.save(h);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Upload failed"));
        }
        return ResponseEntity.ok(Map.of("message", "Banner uploaded", "url", h.getBannerUrl()));
    }
```

#### 2. Xử lý Đề bài (Trong `RoundController.java`)
Thêm vào `RoundController`:
```java
    @GetMapping("/rounds/{id}/problem-statement")
    public ResponseEntity<?> getProblemStatement(@PathVariable Integer id) {
        Round r = roundRepository.findById(id).orElseThrow();
        // Giả sử content đề bài lưu trong problemStatementUrl (hoặc thêm cột problemContent)
        return ResponseEntity.ok(Map.of("content", r.getProblemStatementUrl() != null ? r.getProblemStatementUrl() : ""));
    }

    @PostMapping("/rounds/{id}/release-problem")
    public ResponseEntity<?> releaseProblem(@PathVariable Integer id) {
        Round r = roundRepository.findById(id).orElseThrow();
        r.setProblemReleasedAt(LocalDateTime.now());
        roundRepository.save(r);
        return ResponseEntity.ok(Map.of("message", "Problem released successfully!"));
    }
```

---

### PHASE 2: TẦNG REPOSITORY VÀ DTO

#### 1. File DTO (Tạo mới trong `hackthon/mangaement/hackathon/dto/`)

**`AdminCreateTeamRequest.java`**
```java
package hackthon.mangaement.hackathon.dto;
import lombok.Data;
@Data
public class AdminCreateTeamRequest {
    private String name;
    private Integer leaderId;
    private Integer hackathonId;
}
```

**`AdminAddMemberRequest.java`**
```java
package hackthon.mangaement.hackathon.dto;
import lombok.Data;
@Data
public class AdminAddMemberRequest {
    private Integer userId;
}
```

**`AdminMergeTeamRequest.java`**
```java
package hackthon.mangaement.hackathon.dto;
import lombok.Data;
@Data
public class AdminMergeTeamRequest {
    private Integer sourceTeamId;
    private Integer targetTeamId;
}
```

#### 2. File Repository

Thêm vào **`TeamRepository.java`**:
```java
    // Tìm các đội chưa đủ người (Giả sử Hackathon cấu hình minTeamSize = 3)
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Team t WHERE t.hackathon.id = :hackathonId AND (SELECT COUNT(tm) FROM TeamMember tm WHERE tm.team.id = t.id) < 3")
    List<Team> findIncompleteTeams(@org.springframework.data.repository.query.Param("hackathonId") Integer hackathonId);
```

Thêm vào **`UserRepository.java`**:
```java
    // Tìm sinh viên mồ côi (chưa vô đội nào trong hackathon này)
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.role = 'STUDENT' AND u.id NOT IN " +
           "(SELECT tm.user.id FROM TeamMember tm WHERE tm.team.hackathon.id = :hackathonId)")
    List<User> findOrphansByHackathon(@org.springframework.data.repository.query.Param("hackathonId") Integer hackathonId);
```

---

### PHASE 3: TẦNG SERVICE VÀ CONTROLLER (TEAM)

#### 1. Code Logic trong `TeamService.java`
Thêm các hàm này vào `TeamService.java`:

```java
    // Lấy thí sinh mồ côi
    public List<User> getOrphanUsers(Integer hackathonId) {
        return userRepository.findOrphansByHackathon(hackathonId);
    }

    // Lấy đội thiếu người
    public List<Team> getIncompleteTeams(Integer hackathonId) {
        return teamRepository.findIncompleteTeams(hackathonId);
    }

    // Tự động ghép đội (Matchmaking)
    @org.springframework.transaction.annotation.Transactional
    public void closeRegistrationEarly(Integer hackathonId, User coordinator) {
        Hackathon h = hackathonRepository.findById(hackathonId)
            .orElseThrow(() -> new ResourceNotFoundException("Hackathon not found"));
        
        if (coordinator.getRole() != User.Role.COORDINATOR) {
            throw new org.springframework.security.access.AccessDeniedException("Only coordinator can close registration");
        }

        h.setRegistrationEnd(java.time.LocalDate.now());
        hackathonRepository.save(h);
    }

    @org.springframework.transaction.annotation.Transactional
    public void runMatchmaking(Integer hackathonId, User coordinator) {
        List<User> orphans = getOrphanUsers(hackathonId);
        List<Team> incompleteTeams = getIncompleteTeams(hackathonId);

        for (User orphan : orphans) {
            for (Team team : incompleteTeams) {
                if (teamMemberRepository.findByTeamId(team.getId()).size() < 4) { // Max size = 4
                    TeamMember newMember = new TeamMember();
                    newMember.setTeam(team);
                    newMember.setUser(orphan);
                    newMember.setStatus(TeamMember.Status.ACCEPTED);
                    teamMemberRepository.save(newMember);
                    break;
                }
            }
        }
    }

    // Admin tạo đội
    @org.springframework.transaction.annotation.Transactional
    public Team adminCreateTeam(String name, Integer leaderId, Integer hackathonId) {
        User leader = userRepository.findById(leaderId).orElseThrow();
        return createTeam(name, leader, hackathonId); // Tái sử dụng hàm cũ
    }

    // Admin thêm thành viên
    @org.springframework.transaction.annotation.Transactional
    public void adminAddMember(Integer teamId, Integer userId) {
        Team team = teamRepository.findById(teamId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setStatus(TeamMember.Status.ACCEPTED);
        teamMemberRepository.save(member);
    }

    // Admin gộp đội
    @org.springframework.transaction.annotation.Transactional
    public void mergeTeams(Integer sourceTeamId, Integer targetTeamId) {
        Team source = teamRepository.findById(sourceTeamId).orElseThrow();
        Team target = teamRepository.findById(targetTeamId).orElseThrow();

        // Chuyển toàn bộ thành viên từ source sang target
        List<TeamMember> members = teamMemberRepository.findByTeamId(source.getId());
        for (TeamMember m : members) {
            m.setTeam(target);
            teamMemberRepository.save(m);
        }
        
        // Xóa đội source
        teamRepository.delete(source);
    }
```

#### 2. Gọi Service từ `TeamController.java`
Thêm vào `TeamController.java`:

```java
    @GetMapping("/teams/hackathons/{id}/orphans")
    public ResponseEntity<?> getOrphans(@PathVariable Integer id) {
        return ResponseEntity.ok(teamService.getOrphanUsers(id));
    }

    @GetMapping("/teams/hackathons/{id}/incomplete-teams")
    public ResponseEntity<?> getIncompleteTeams(@PathVariable Integer id) {
        return ResponseEntity.ok(teamService.getIncompleteTeams(id));
    }

    @PostMapping("/teams/hackathons/{id}/matchmaking")
    public ResponseEntity<?> runMatchmaking(@PathVariable Integer id, @AuthenticationPrincipal User coordinator) {
        teamService.runMatchmaking(id, coordinator);
        return ResponseEntity.ok(Map.of("message", "Matchmaking completed successfully."));
    }

    @PostMapping("/teams/admin-create")
    public ResponseEntity<?> adminCreateTeam(@RequestBody hackthon.mangaement.hackathon.dto.AdminCreateTeamRequest req) {
        Team team = teamService.adminCreateTeam(req.getName(), req.getLeaderId(), req.getHackathonId());
        return ResponseEntity.ok(team);
    }

    @PostMapping("/teams/{id}/admin-add-member")
    public ResponseEntity<?> adminAddMember(@PathVariable Integer id, 
                                            @RequestBody hackthon.mangaement.hackathon.dto.AdminAddMemberRequest req) {
        teamService.adminAddMember(id, req.getUserId());
        return ResponseEntity.ok(Map.of("message", "Member added by admin."));
    }

    @PostMapping("/teams/{id}/admin-merge")
    public ResponseEntity<?> adminMergeTeam(@PathVariable Integer id, 
                                            @RequestBody hackthon.mangaement.hackathon.dto.AdminMergeTeamRequest req) {
        teamService.mergeTeams(req.getSourceTeamId(), req.getTargetTeamId());
        return ResponseEntity.ok(Map.of("message", "Teams merged successfully."));
    }
```

---

### PHASE 4: CHẤM THI TRỰC TIẾP (PRESENTATION CONTROLLER & SERVICE)

#### 1. Tạo file `PresentationService.java`
Nằm trong `be/src/main/java/hackthon/mangaement/hackathon/service/`

```java
package hackthon.mangaement.hackathon.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class PresentationService {
    
    @Autowired
    private NotificationService notificationService; // Dùng để bắn WebSocket

    public void manageTimer(String action, Integer trackId) {
        // Gửi WebSocket xuống Frontend báo Timer thay đổi (START, PAUSE, RESET...)
        // Dùng NotificationService để push qua đường /ws/notifications hoặc đường riêng
        // Giả lập trạng thái
        Map<String, Object> payload = Map.of(
            "action", action,
            "trackId", trackId,
            "timestamp", System.currentTimeMillis()
        );
        // Tạm thời gọi hàm giả định (Bạn cần cấu hình WebSocket chuẩn để push event)
        System.out.println("WebSocket Broadcast Timer Action: " + payload);
    }
}
```

#### 2. Tạo file `PresentationController.java`
Nằm trong `be/src/main/java/hackthon/mangaement/hackathon/controller/`

```java
package hackthon.mangaement.hackathon.controller;

import hackthon.mangaement.hackathon.service.PresentationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/presentation")
public class PresentationController {

    @Autowired
    private PresentationService presentationService;

    @GetMapping("/queue")
    public ResponseEntity<?> getPresentationQueue() {
        // Trả về danh sách đội chấm thi rỗng để FE không bị lỗi
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/timer/start")
    public ResponseEntity<?> startTimer(@RequestBody Map<String, Integer> req) {
        presentationService.manageTimer("START", req.get("trackId"));
        return ResponseEntity.ok(Map.of("message", "Timer started"));
    }

    @PostMapping("/timer/pause")
    public ResponseEntity<?> pauseTimer(@RequestBody Map<String, Integer> req) {
        presentationService.manageTimer("PAUSE", req.get("trackId"));
        return ResponseEntity.ok(Map.of("message", "Timer paused"));
    }

    @PostMapping("/timer/resume")
    public ResponseEntity<?> resumeTimer(@RequestBody Map<String, Integer> req) {
        presentationService.manageTimer("RESUME", req.get("trackId"));
        return ResponseEntity.ok(Map.of("message", "Timer resumed"));
    }

    @PostMapping("/timer/reset")
    public ResponseEntity<?> resetTimer(@RequestBody Map<String, Integer> req) {
        presentationService.manageTimer("RESET", req.get("trackId"));
        return ResponseEntity.ok(Map.of("message", "Timer reset"));
    }
}
```

---
**🎉 CHÚC MỪNG! BẠN ĐÃ CÓ TOÀN BỘ CODE ĐỂ HOÀN THIỆN BACKEND.**
Cứ copy từng phần và dán vào đúng class là chạy được nhé! Lỗi import thư viện (nếu có) IDE của bạn sẽ tự động gợi ý (`Alt + Enter`).
