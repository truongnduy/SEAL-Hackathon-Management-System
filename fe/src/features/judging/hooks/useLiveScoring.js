import { useState, useEffect, useCallback } from 'react';
import { message } from 'antd';
import { judgeService } from '../services/judgeService';
// Import Mapper từ chính file của nhóm bạn (đảm bảo đường dẫn đúng)
import { mapCriterionToFE } from '../../criteria/mappers/criteriaMapper'; 

export const useLiveScoring = (assignmentId) => {
  const [teams, setTeams] = useState([]);
  const [criteria, setCriteria] = useState([]);
  const [selectedTeam, setSelectedTeam] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Lưu điểm đang chấm (Dạng: { criteriaId: score })
  const [currentScores, setCurrentScores] = useState({});
  const [comment, setComment] = useState('');

  const fetchScoringData = useCallback(async () => {
    if (!assignmentId) return;
    setIsLoading(true);
    try {
      // 1. Tạm thời MOCK danh sách Teams chờ Backend viết API
      // Sau này Backend có API, bạn chỉ cần thay đoạn này thành:
      // const teamsRes = await judgeService.getTeamsToScore(assignmentId);
      const fetchedTeams = [
        { id: 1, name: 'Cyber Nurture', leader: 'Nguyễn Văn A', status: 'PENDING' },
        { id: 2, name: 'Tech Innovators', leader: 'Trần Thị B', status: 'SCORED', totalScore: 85.5 },
        { id: 3, name: 'SEAL Alpha', leader: 'Lê Văn C', status: 'PENDING' },
      ];

      // 2. GỌI API THẬT ĐỂ LẤY CRITERIA (Dùng API của nhóm)
      const criteriaRes = await judgeService.getScoringCriteria(assignmentId);
      
      // Sử dụng Mapper của nhóm để chuẩn hóa dữ liệu
      const rawCriteria = Array.isArray(criteriaRes) ? criteriaRes : criteriaRes?.items || criteriaRes?.data || [];
      const fetchedCriteria = rawCriteria.map(mapCriterionToFE).filter(Boolean);

      setTeams(fetchedTeams);
      setCriteria(fetchedCriteria);
      
      if (fetchedTeams.length > 0) {
        setSelectedTeam(fetchedTeams[0]);
      }
    } catch (error) {
      message.error(error?.message || 'Lỗi khi tải dữ liệu chấm thi từ máy chủ.');
    } finally {
      setIsLoading(false);
    }
  }, [assignmentId]);

  useEffect(() => {
    fetchScoringData();
  }, [fetchScoringData]);

  // Cập nhật điểm khi kéo Slider
  const handleScoreChange = (criteriaId, value) => {
    setCurrentScores(prev => ({ ...prev, [criteriaId]: value }));
  };

  // LOGIC TÍNH ĐIỂM DỰA TRÊN WEIGHT & MAX_SCORE
  const calculateTotalScore = () => {
    let total = 0;
    criteria.forEach(c => {
      // Nếu loại là PENALTY (nếu có), trừ điểm thay vì cộng. 
      // Nhưng theo MOCK của bạn, các type là Technical, Innovation, General
      const rawScore = currentScores[c.id] || 0; 
      
      // Công thức: Tổng điểm = (Điểm đạt được / Điểm Max) * Trọng số * 100
      // Ví dụ: Đạt 80/100, Trọng số 0.4 => (80/100) * 0.4 * 100 = 32 điểm thành phần
      // Hoặc tính thẳng: Điểm đạt * Trọng số (Nếu hệ thống chấm theo thang tổng)
      
      // Áp dụng công thức đơn giản nhất theo hệ thống MOCK: 
      // Giám khảo chấm điểm (rawScore), hệ thống tự nhân với trọng số (weight).
      total += rawScore * (c.weight || 0); 
    });
    return total.toFixed(2);
  };

  // NỘP ĐIỂM
  const submitFinalScore = async () => {
    if (!selectedTeam) return;
    
    // Kiểm tra xem đã chấm đủ tiêu chí chưa
    const missingCriteria = criteria.some(c => currentScores[c.id] === undefined);
    if (missingCriteria) {
      message.warning('Vui lòng kéo thanh điểm cho tất cả các tiêu chí trước khi chốt.');
      return;
    }

    setIsSubmitting(true);
    try {
      const finalTotalScore = parseFloat(calculateTotalScore());
      const payload = {
        scores: criteria.map(c => ({
          criteriaId: c.id,
          score: currentScores[c.id] || 0
        })),
        comment: comment,
        totalScore: finalTotalScore
      };
      
      // MOCK CALL API (Sau này mở comment dòng dưới)
      // await judgeService.submitScore(assignmentId, selectedTeam.id, payload);
      
      setTimeout(() => {
        message.success(`Đã lưu điểm cho đội ${selectedTeam.name} thành công!`);
        setTeams(prev => prev.map(t => 
          t.id === selectedTeam.id ? { ...t, status: 'SCORED', totalScore: finalTotalScore } : t
        ));
        setCurrentScores({});
        setComment('');
        setIsSubmitting(false);
      }, 800);
      
    } catch (error) {
      message.error(error?.message || 'Lỗi khi lưu điểm. Vui lòng kiểm tra lại.');
      setIsSubmitting(false);
    }
  };

  return {
    teams, criteria, selectedTeam, setSelectedTeam,
    isLoading, isSubmitting, currentScores, handleScoreChange,
    comment, setComment, calculateTotalScore, submitFinalScore
  };
};