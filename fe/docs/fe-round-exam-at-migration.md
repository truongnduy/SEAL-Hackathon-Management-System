# Hướng dẫn FE — Cập nhật Round (`examAt`, bỏ `sequenceOrder`)

Tài liệu này tổng hợp thay đổi BE đã triển khai để team Frontend chỉnh form **Thêm/Sửa vòng thi**, mapping API và xử lý lỗi.  
Phạm vi: **Round (vòng thi)**. **Track (bảng đấu)** có mục riêng ở cuối.

---

## 1. Tóm tắt thay đổi

| Trước | Sau |
|--------|-----|
| Form có ô **「Thứ tự」** (`sequenceOrder`: 1, 2, …) | **Bỏ ô Thứ tự** trên UI Round |
| Thứ tự vòng do user nhập số | Thứ tự vòng = **sắp xếp theo `examAt`** (ngày + giờ thi) |
| `sequenceOrder` bắt buộc trong body | **`examAt` bắt buộc**; gửi `sequenceOrder` → **BE bỏ qua** (field không còn) |
| Dễ 409 trùng `(round_id, sequence_order)` trên Track | Track: **không gửi** `sequenceOrder` → BE tự `max + 1` |

**Khái niệm nghiệp vụ**

- **`examAt`**: ngày/giờ **thi** (presentation, thi đấu…) — dùng để biết vòng nào trước/sau trong hackathon.
- **`submissionOpen` / `submissionDeadline`**: cửa sổ **nộp bài** — độc lập với ngày thi.
- **Track** trong cùng vòng sơ loại: các bảng **song song**, không xếp lịch theo `sequenceOrder` trên UI.

---

## 2. API Round — contract mới

### 2.1 Endpoints (không đổi path)

| Method | Path | Ghi chú |
|--------|------|---------|
| POST | `/api/v1/hackathons/{hackathonId}/rounds` | Tạo vòng |
| GET | `/api/v1/hackathons/{hackathonId}/rounds` | List — sort **`examAt` tăng dần** |
| GET | `/api/v1/rounds/{id}` | Chi tiết |
| PUT | `/api/v1/rounds/{id}` | Cập nhật |

### 2.2 Request — POST tạo vòng

**Bắt buộc:** `name`, `examAt`, `submissionDeadline`  
**Không gửi:** `sequenceOrder` (đã xóa khỏi contract)

```json
{
  "name": "Vòng Sơ loại",
  "examAt": "2026-06-10T08:00:00",
  "isFinal": false,
  "roundType": "PRELIMINARY",
  "submissionOpen": "2026-06-01T00:00:00",
  "submissionDeadline": "2026-06-09T23:59:59",
  "codingDurationHours": 7,
  "lateSubmissionPolicy": "ALLOW_LATE_PENDING",
  "topNAdvance": 2,
  "minTeamsFinal": 6,
  "wildcardEnabled": true,
  "tiebreakRule": "PENALTY_SCORE"
}
```

**Vòng Chung kết** (tạo **sau** khi đã có ít nhất một vòng Sơ loại/Bán kết):

```json
{
  "name": "Vòng Chung kết",
  "examAt": "2026-07-05T08:00:00",
  "isFinal": true,
  "roundType": "FINAL",
  "submissionDeadline": "2026-07-12T23:59:59",
  "lateSubmissionPolicy": "HARD_LOCK",
  "tiebreakRule": "PENALTY_SCORE"
}
```

`topNAdvance`, `minTeamsFinal` → **null** / không gửi khi `isFinal: true`.

### 2.3 Request — PUT cập nhật vòng

**Bắt buộc:** `name`, `examAt`, `submissionDeadline` (+ các field khác như hiện tại)

```json
{
  "name": "Vòng Sơ loại",
  "examAt": "2026-06-10T08:00:00",
  "submissionOpen": "2026-06-01T00:00:00",
  "submissionDeadline": "2026-06-09T23:59:59",
  "codingDurationHours": 7,
  "topNAdvance": 2,
  "wildcardEnabled": true,
  "minTeamsFinal": 6,
  "tiebreakRule": "PENALTY_SCORE"
}
```

### 2.4 Response — thay đổi field

**`RoundResponse` / `RoundSummaryResponse`**

| Field | Trạng thái |
|-------|------------|
| `sequenceOrder` | **Đã xóa** — không còn trong JSON |
| `examAt` | **Mới** — `string` ISO-8601 local datetime, ví dụ `2026-06-10T08:00:00` |

Ví dụ `GET .../hackathons/1/rounds`:

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Vòng Sơ loại",
      "examAt": "2026-06-10T08:00:00",
      "submissionDeadline": "2026-06-09T23:59:59",
      "isActive": false,
      "scoringLocked": false,
      "trackCount": 2,
      "criteriaCount": 0,
      "currentWeightTotal": 0
    },
    {
      "id": 2,
      "name": "Vòng Chung kết",
      "examAt": "2026-07-05T08:00:00",
      "submissionDeadline": "2026-07-12T23:59:59",
      "isActive": false,
      "trackCount": 0,
      "criteriaCount": 5,
      "currentWeightTotal": 1
    }
  ]
}
```

**Hiển thị thứ tự trên UI:** dùng thứ tự phần tử trong `data[]` (BE đã sort `examAt` ASC), hoặc sort lại client theo `examAt` — **không** hiển thị “Vòng 1 / Vòng 2” từ `sequenceOrder`.

---

## 3. Form UI — checklist chỉnh sửa

### 3.1 Modal 「Thêm vòng thi」 / 「Sửa vòng thi」

| Hành động | Chi tiết |
|-----------|----------|
| **Xóa** | Ô 「Thứ tự」 / `sequenceOrder` |
| **Thêm** | Ô **「Ngày giờ thi」** → map `examAt` (date + time picker, bắt buộc) |
| **Giữ** | Mở nộp bài, Hạn chót nộp bài, Loại vòng, Chung kết, … |
| **Validate client (gợi ý)** | `examAt >= submissionOpen` (nếu có mở nộp bài); deadline > now (đồng bộ BE) |

**Gợi ý label**

- `examAt` → **「Ngày giờ thi」** (tooltip: khác với hạn nộp bài)
- `submissionOpen` → **「Mở nộp bài」**
- `submissionDeadline` → **「Hạn chót nộp bài」**

### 3.2 Luồng tạo vòng (mainflow GĐ1)

```text
1. POST Round Sơ loại   (examAt sớm, isFinal=false)
2. POST Round Chung kết (examAt muộn hơn Sơ loại, isFinal=true)  ← chỉ sau bước 1
3. POST Track(s) vào round Sơ loại (không gửi sequenceOrder)
4. Criteria, nhân sự, events, readiness, activate…
```

FE nên **không** cho tạo Chung kết trước Sơ loại (hoặc hiển thị lỗi từ BE bảng mục 4).

---

## 4. Mã lỗi BE — map message UI

Cấu trúc lỗi chuẩn:

```json
{
  "success": false,
  "error": {
    "code": "ROUND_PRELIM_EXAM_ORDER",
    "message": "...",
    "status": 422,
    "details": { }
  }
}
```

| `error.code` | HTTP | Khi nào | Gợi ý UI |
|--------------|------|---------|----------|
| `ROUND_FINAL_EXAM_ORDER` | 422 | Tạo/sửa CK: `examAt` ≤ ngày thi Sơ loại | 「Ngày thi Chung kết phải sau vòng Sơ loại」 |
| `ROUND_PRELIM_EXAM_ORDER` | 422 | Tạo/sửa Sơ loại: `examAt` ≥ ngày thi CK đã có | 「Ngày thi Sơ loại phải trước Chung kết」 |
| `ROUND_FINAL_REQUIRES_PRELIM` | 422 | Tạo CK khi chưa có vòng Sơ loại/Bán kết | 「Tạo vòng Sơ loại trước」 |
| `ROUND_DUPLICATE_FINAL` | 409 | Tạo CK thứ 2 | 「Đã có vòng Chung kết」 |
| `ROUND_EXAM_BEFORE_SUBMISSION_OPEN` | 422 | `examAt` < `submissionOpen` | 「Ngày thi phải sau thời điểm mở nộp bài」 |
| `ROUND_DEADLINE_INVALID` | 422 | Deadline ≤ open hoặc ≤ hiện tại | Giữ message BE |
| `VALIDATION_FAILED` | 400 | Thiếu `examAt`, `name`, … | Highlight field bắt buộc |

**Lỗi cũ không còn dùng cho Round**

- `DB_INTEGRITY_VIOLATION` do `uk_rounds_hackathon_sequence` — không còn nếu FE không gửi `sequenceOrder`.
- `TRACK_SEQUENCE_DUPLICATE` — chỉ Track; xem mục 5.

---

## 5. Track (bảng đấu) — liên quan form Add Track

Endpoint: `POST /api/v1/rounds/{roundId}/tracks`

| Field | FE |
|-------|-----|
| `sequenceOrder` | **Không gửi** (không có ô trên form) |
| Các field khác | Giữ như hiện tại (`name`, `minTeamSize`, `maxTeamSize`, …) |

BE tự gán `sequence_order = max + 1` trong round.  
Nếu FE vẫn gửi `"sequenceOrder": 1` cố định → BE **không 409**, sẽ gán số tiếp theo khi trùng.

Response Track vẫn có `sequenceOrder` (số bảng nội bộ) — có thể dùng label 「Bảng 1」「Bảng 2」 khi list, không cần input lúc tạo.

---

## 6. TypeScript / model gợi ý

```typescript
// Round — cập nhật interface
interface CreateRoundPayload {
  name: string;
  examAt: string; // ISO local, e.g. "2026-06-10T08:00:00"
  isFinal?: boolean;
  roundType?: 'PRELIMINARY' | 'SEMIFINAL' | 'FINAL';
  submissionOpen?: string | null;
  submissionDeadline: string;
  codingDurationHours?: number;
  lateSubmissionPolicy?: 'ALLOW_LATE_PENDING' | 'HARD_LOCK';
  topNAdvance?: number | null;
  minTeamsFinal?: number | null;
  wildcardEnabled?: boolean;
  tiebreakRule?: 'PENALTY_SCORE' | 'SUBMISSION_TIME' | 'COORDINATOR_DECISION';
  // sequenceOrder?: number;  // REMOVED
}

interface RoundSummary {
  id: number;
  name: string;
  examAt: string;
  submissionDeadline: string;
  isActive: boolean;
  scoringLocked: boolean;
  trackCount: number;
  criteriaCount: number;
  currentWeightTotal: number;
}
```

---

## 7. Migration checklist FE

- [ ] Xóa state/form field `sequenceOrder` trên màn Round
- [ ] Thêm `examAt` (date + time), required
- [ ] POST/PUT map `examAt` vào body
- [ ] Parse response: đọc `examAt`, bỏ phụ thuộc `sequenceOrder`
- [ ] List rounds: sort/display theo `examAt` (hoặc tin thứ tự API)
- [ ] Map 5 mã lỗi mới (bảng mục 4)
- [ ] Mainflow: nút 「Thêm Chung kết」 chỉ enable sau khi đã có Sơ loại (tuỳ UX)
- [ ] Add Track: không gửi `sequenceOrder`
- [ ] Test E2E: tạo 2 vòng + 2 track không còn 409

---

## 8. DB / deploy (tham khảo)

BE dùng cột `rounds.exam_at`, bỏ `sequence_order`.  
Nếu môi trường dev còn cột cũ, chạy script (BE):

`src/main/resources/db/manual/V20260522_round_exam_at.sql`

Sau deploy BE mới, **restart API** rồi FE test lại.

---

## 9. Liên hệ spec đầy đủ

- Workflow MF-01: `docs/workflow/mf01.md` (FR-02 Round)
- Happy path test: `docs/workflow/mf01-gd1-test-happy-path.md` (đã đổi mẫu `examAt`)

---

*Tài liệu sinh từ phiên chỉnh BE Round — examAt & validation. Cập nhật khi BE đổi thêm contract.*
