export const STUDENT_HACKATHON_ERROR_CODES = {
  INVALID_STATE: 'INVALID_STATE',
  HACKATHON_NOT_ONGOING: 'HACKATHON_NOT_ONGOING',
  REGISTRATION_CLOSED: 'REGISTRATION_CLOSED',
  REGISTRATION_WITHDRAWN: 'REGISTRATION_WITHDRAWN',
  REGISTRATION_ALREADY_ACTIVE_ELSEWHERE: 'REGISTRATION_ALREADY_ACTIVE_ELSEWHERE',
};

export const STUDENT_HACKATHON_ERROR_MESSAGES = {
  [STUDENT_HACKATHON_ERROR_CODES.INVALID_STATE]:
    'Đăng ký thất bại: Giải đấu đã đạt giới hạn tối đa số lượng người tham gia.',
  [STUDENT_HACKATHON_ERROR_CODES.HACKATHON_NOT_ONGOING]:
    'Giải đấu hiện không mở đăng ký.',
  [STUDENT_HACKATHON_ERROR_CODES.REGISTRATION_CLOSED]:
    'Thời gian đăng ký đã kết thúc hoặc chưa bắt đầu.',
  [STUDENT_HACKATHON_ERROR_CODES.REGISTRATION_WITHDRAWN]:
    'Bạn đã hủy đăng ký giải này trước đó và không thể đăng ký lại.',
  [STUDENT_HACKATHON_ERROR_CODES.REGISTRATION_ALREADY_ACTIVE_ELSEWHERE]:
    'Bạn đã đăng ký một giải đấu khác. Mỗi người chỉ được đăng ký một giải tại một thời điểm.',
};

export const getStudentHackathonErrorMessage = (error, fallback = 'Không thể đăng ký hackathon') => {
  const code = error?.code || error?.data?.error?.code || error?.response?.data?.error?.code;
  const serverMessage = error?.message || error?.response?.data?.error?.message;

  if (STUDENT_HACKATHON_ERROR_MESSAGES[code]) {
    return serverMessage || STUDENT_HACKATHON_ERROR_MESSAGES[code];
  }

  if (code === STUDENT_HACKATHON_ERROR_CODES.INVALID_STATE) {
    if (serverMessage?.includes('đã đăng ký')) {
      return serverMessage;
    }
    if (serverMessage?.includes('rời Đội') || serverMessage?.includes('hủy đăng ký')) {
      return serverMessage;
    }
    return STUDENT_HACKATHON_ERROR_MESSAGES.INVALID_STATE;
  }

  return serverMessage || fallback;
};
