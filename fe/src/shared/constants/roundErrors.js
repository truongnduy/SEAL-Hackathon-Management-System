export const ROUND_ERROR_MESSAGES = {
  ROUND_FINAL_EXAM_ORDER: 'Ngày thi Chung kết phải sau vòng Sơ loại',
  ROUND_PRELIM_EXAM_ORDER: 'Ngày thi Sơ loại phải trước Chung kết',
  ROUND_FINAL_REQUIRES_PRELIM: 'Tạo vòng Sơ loại trước khi tạo Chung kết',
  ROUND_DUPLICATE_FINAL: 'Hackathon đã có vòng Chung kết — mỗi kỳ chỉ 1 vòng final',
  ROUND_TYPE_DUPLICATE: 'Hackathon đã có vòng loại này — mỗi loại chỉ tạo được 1 lần (Sơ loại / Bán kết / Chung kết)',
  ROUND_EXAM_BEFORE_SUBMISSION_OPEN: 'Ngày giờ thi phải trước thời điểm mở nộp bài',
  ROUND_EXAM_BEFORE_KICKOFF: 'Ngày thi phải sau khi Khai mạc kết thúc',
  EVENT_OUT_OF_HACKATHON: 'Ngày thi nằm ngoài khoảng thời gian diễn ra hackathon',
};

export const getRoundErrorMessage = (error) => {
  if (!error) return 'Lỗi khi lưu vòng thi';
  const code = error.code || error.data?.error?.code;
  if (code && ROUND_ERROR_MESSAGES[code]) {
    return ROUND_ERROR_MESSAGES[code];
  }
  if (code === 'ROUND_DEADLINE_INVALID') {
    return error.message || error.data?.error?.message || 'Hạn chót nộp bài không hợp lệ';
  }
  return error.message || error.data?.error?.message || 'Lỗi khi lưu vòng thi';
};
