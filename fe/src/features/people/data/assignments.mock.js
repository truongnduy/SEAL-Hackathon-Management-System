export const MOCK_ASSIGNMENTS = [
  {
    id: 1,
    hackathon_id: 1,
    person_id: 1,
    type: 'JUDGE', 
    round_id: 1,   // Trỏ tới ID của Round Sơ loại
    track_id: null,
    assignment_type: 'HEAD' // 'NORMAL' | 'CALIBRATION' | 'HEAD'
  },
  {
    id: 2,
    hackathon_id: 1,
    person_id: 2,
    type: 'MENTOR',
    round_id: null,
    track_id: 1,
    assignment_type: null
  }
];