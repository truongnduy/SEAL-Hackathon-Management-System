// src/features/presentation/utils/presentationSubmissionUtils.js
/** Khớp BE `SubmissionGradablePolicy` — chỉ các status này vào hàng đợi khi xáo trộn. */
const GRADABLE_STATUSES = new Set(['SUBMITTED', 'LATE_APPROVED', 'ACCEPTED']);

export const isGradableSubmissionStatus = (status) =>
  Boolean(status && GRADABLE_STATUSES.has(String(status).toUpperCase()));

export const getSubmissionStatusMeta = (status) => {
  const normalized = String(status || 'NONE').toUpperCase();
  switch (normalized) {
    case 'SUBMITTED':
      return { label: 'Nộp đúng hạn', color: 'green', gradable: true };
    case 'LATE_APPROVED':
      return { label: 'Nộp trễ — đã duyệt', color: 'blue', gradable: true };
    case 'ACCEPTED':
      return { label: 'Đã chấp nhận', color: 'green', gradable: true };
    case 'LATE_PENDING':
      return { label: 'Nộp trễ — chờ duyệt', color: 'orange', gradable: false };
    case 'LATE':
      return { label: 'Nộp trễ', color: 'orange', gradable: false };
    case 'REJECTED':
      return { label: 'Bị từ chối', color: 'red', gradable: false };
  }
  return { label: 'Chưa nộp / không xác định', color: 'default', gradable: false };
};

export const countGradableSubmissions = (submissions = []) =>
  submissions.filter((s) => isGradableSubmissionStatus(s.status)).length;
