// src/features/judging/hooks/useLiveScoringV2.js
import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { message } from 'antd';
import { judgeService } from '../services/judgeService';
import { criteriaService } from '../../criteria/services/criteriaService';
import { presentationService } from '../services/presentationService';

export const useLiveScoringV2 = (assignmentId, roundId, trackId, isFinal, initialAssignmentType) => {
  const [queueData, setQueueData] = useState(null);
  const [submissionsData, setSubmissionsData] = useState([]);
  const [criteria, setCriteria] = useState([]);

  const [scoreState, setScoreState] = useState({ submissionId: null, scores: {}, comment: '' });

  const [rawMyScores, setRawMyScores] = useState([]);
  const [myScoredSubmissions, setMyScoredSubmissions] = useState({});

  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [scoringLocked, setScoringLocked] = useState(false);
  const [isTimerActionLoading, setIsTimerActionLoading] = useState(false);

  const isActionPendingRef = useRef(false);

  const isController = useMemo(() => {
    const role = String(initialAssignmentType || '').toUpperCase();
    return role.includes('HEAD') || role.includes('FINAL_EXTERNAL');
  }, [initialAssignmentType]);

  const [localTimerPhase, setLocalTimerPhase] = useState('IDLE');
  const [localRemainingSeconds, setLocalRemainingSeconds] = useState(0);

  const timerEngineRef = useRef({
    phase: 'IDLE',
    originalPhase: 'PRESENTING', 
    baseSeconds: 0,
    startTimeMs: 0,
    intervalId: null
  });

  const syncTimerState = useCallback((phase, seconds) => {
    timerEngineRef.current.phase = phase;
    timerEngineRef.current.baseSeconds = seconds;
    setLocalTimerPhase(phase);
    setLocalRemainingSeconds(seconds);
  }, []);

  const applyEngineState = useCallback((newPhase, newSeconds) => {
    const engine = timerEngineRef.current;
    
    if (engine.intervalId) {
      clearInterval(engine.intervalId);
      engine.intervalId = null;
    }

    engine.phase = newPhase;
    if (newPhase === 'PRESENTING' || newPhase === 'QA') {
        engine.originalPhase = newPhase;
    }
    
    engine.baseSeconds = newSeconds;
    engine.startTimeMs = Date.now();
    
    setLocalTimerPhase(newPhase);
    setLocalRemainingSeconds(newSeconds);

    if (newPhase === 'PRESENTING' || newPhase === 'QA') {
      engine.intervalId = setInterval(() => {
        const elapsedSeconds = Math.floor((Date.now() - engine.startTimeMs) / 1000);
        const currentTick = Math.max(0, engine.baseSeconds - elapsedSeconds);
        setLocalRemainingSeconds(currentTick);
      }, 1000);
    }
  }, []);

  const fetchStaticData = useCallback(async () => {
    try {
      const [critRes, subRes, myScoresRes] = await Promise.all([
        isFinal ? criteriaService.listByFinalRound(roundId) : criteriaService.listByTrack(trackId),
        judgeService.getSubmissions({ roundId, trackId: isFinal ? undefined : trackId }),
        judgeService.getMyScores(roundId).catch(() => [])
      ]);

      const critData = Array.isArray(critRes) ? critRes : critRes?.items || [];
      setCriteria(critData);
      setSubmissionsData(Array.isArray(subRes) ? subRes : subRes?.items || subRes?.data || []);

      const scoresData = Array.isArray(myScoresRes) ? myScoresRes : myScoresRes?.items || myScoresRes?.data || [];
      setRawMyScores(scoresData);

      const weightMap = {};
      critData.forEach(c => weightMap[c.id] = c.weight || 0);

      const scoredMap = {};
      scoresData.forEach(s => {
        const subId = String(s.submissionId ?? s.submission_id);
        const critId = String(s.criterionId ?? s.criterion_id);
        const val = Number(s.scoreValue ?? s.score_value ?? s.score ?? s.value ?? s.totalScore ?? s.total_score ?? 0);

        if (critId && critId !== 'undefined' && critId !== 'null') {
            if (!scoredMap[subId]) scoredMap[subId] = 0;
            scoredMap[subId] += val * (weightMap[critId] || 0);
        } else {
            scoredMap[subId] = val;
        }
      });

      const finalScoredMap = {};
      Object.keys(scoredMap).forEach(subId => { finalScoredMap[subId] = scoredMap[subId].toFixed(2); });
      setMyScoredSubmissions(finalScoredMap);

    } catch (error) {}
  }, [roundId, trackId, isFinal]);

  const fetchQueue = useCallback(async (force = false) => {
    if (!roundId || (!force && isActionPendingRef.current)) return;
    try {
      const qRes = await presentationService.getQueue(roundId, isFinal ? null : trackId);
      const qData = qRes?.data || qRes;
      setQueueData(qData);

      const track = isFinal ? qData.groups?.[0] : qData.tracks?.find(t => t.trackId === Number(trackId));
      const presenting = (track?.items || track?.teams || []).find(item => item.status === 'PRESENTING');

      if (presenting?.timer) {
        const serverPhase = presenting.timer.phase;
        // KHÔNG HARDCODE. Lấy 100% thời gian thực tế từ BE
        let serverSeconds = presenting.timer.remainingSeconds || 0;
        if (serverSeconds < 0) serverSeconds = 0;

        const currentEngine = timerEngineRef.current;

        if (currentEngine.phase !== serverPhase) {
           applyEngineState(serverPhase, serverSeconds);
        } 
        else {
           if (serverPhase === 'PAUSED' || serverPhase === 'IDLE' || serverPhase === 'SETUP' || serverPhase === 'ENDED') {
               if (currentEngine.baseSeconds !== serverSeconds) {
                   syncTimerState(serverPhase, serverSeconds);
               }
           }
        }
      } else {
        if (timerEngineRef.current.phase !== 'IDLE') {
           applyEngineState('IDLE', 0);
        }
      }
    } catch (error) {} finally {
      setIsLoading(false);
    }
  }, [roundId, trackId, isFinal, applyEngineState, syncTimerState]);

  useEffect(() => {
    fetchStaticData();
    fetchQueue(true);
    // Polling 1s để mượt mà nhất có thể cho buổi demo
    const interval = setInterval(() => fetchQueue(false), 1000);
    return () => clearInterval(interval);
  }, [fetchStaticData, fetchQueue]);

  useEffect(() => {
    return () => {
       if (timerEngineRef.current.intervalId) {
          clearInterval(timerEngineRef.current.intervalId);
       }
    };
  }, []);

  const { trackQueue, activeSlot } = useMemo(() => {
    if (!queueData) return { trackQueue: [], activeSlot: null };
    const track = isFinal ? queueData.groups?.[0] : queueData.tracks?.find(t => t.trackId === Number(trackId));
    let queueItems = track?.items || track?.teams || [];
    queueItems = queueItems.map(item => {
      const subInfo = submissionsData.find(s => s.submissionId === item.submissionId) || {};
      return { 
        ...item, 
        slideFile: subInfo.slideFile, 
        repoUrl: subInfo.repoUrl,
        demoUrl: subInfo.demoUrl || subInfo.demo_url 
      };
    });
    return { trackQueue: queueItems, activeSlot: queueItems.find(item => item.status === 'PRESENTING') };
  }, [queueData, submissionsData, trackId, isFinal]);

  const currentScores = scoreState.submissionId === activeSlot?.submissionId ? scoreState.scores : {};
  const comment = scoreState.submissionId === activeSlot?.submissionId ? scoreState.comment : '';

  const isAllDone = trackQueue.length > 0 && trackQueue.every(item => item.status === 'DONE');

  const hasScoredCurrentTeam = useMemo(() => {
    if (!activeSlot?.submissionId) return false;
    return !!myScoredSubmissions[String(activeSlot.submissionId)];
  }, [activeSlot, myScoredSubmissions]);

  useEffect(() => {
    if (!activeSlot?.submissionId) {
       setScoreState({ submissionId: null, scores: {}, comment: '' });
       return;
    }
    const subIdStr = String(activeSlot.submissionId);
    
    let hasIndividualScoresInDB = false;
    const dbScores = {};
    let dbComment = '';

    (rawMyScores || []).forEach(s => {
      if (String(s.submissionId ?? s.submission_id) === subIdStr) {
        const cId = s.criterionId ?? s.criterion_id;
        if (cId) {
            hasIndividualScoresInDB = true;
            dbScores[String(cId)] = Number(s.scoreValue ?? s.score_value ?? s.score ?? s.value ?? s.totalScore ?? s.total_score ?? 0);
        }
        if (s.comment) dbComment = s.comment;
      }
    });

    const draftKey = `seal_draft_${assignmentId}_${subIdStr}`;
    let localDraft = null;
    try { localDraft = JSON.parse(localStorage.getItem(draftKey)); } catch(e) {}

    let finalScores = {};
    let finalComment = dbComment;

    if (hasIndividualScoresInDB && Object.keys(dbScores).length > 0) {
       finalScores = dbScores;
    } else if (localDraft && localDraft.scores && Object.keys(localDraft.scores).length > 0) {
       finalScores = localDraft.scores;
       if (!finalComment) finalComment = localDraft.comment || '';
    }

    setScoreState({
       submissionId: activeSlot.submissionId,
       scores: finalScores,
       comment: finalComment
    });

  }, [activeSlot?.submissionId, rawMyScores, assignmentId]);

  useEffect(() => {
    if (!scoreState.submissionId) return;
    const draftKey = `seal_draft_${assignmentId}_${scoreState.submissionId}`;
    localStorage.setItem(draftKey, JSON.stringify({ 
       scores: scoreState.scores, 
       comment: scoreState.comment 
    }));
  }, [scoreState, assignmentId]);

  const canScore = !scoringLocked && activeSlot;
  const canSubmitFinalScore = canScore && ['QA', 'ENDED'].includes(localTimerPhase) && !hasScoredCurrentTeam;

  const handleScoreChange = useCallback((criteriaId, value) => {
    setScoreState(prev => ({
       ...prev,
       scores: { ...prev.scores, [criteriaId]: value }
    }));
  }, []);

  const handleSetComment = useCallback((val) => {
    setScoreState(prev => ({ ...prev, comment: val }));
  }, []);

  const calculateTotal = useCallback(() => {
    return criteria.reduce((sum, c) => sum + (currentScores[c.id] || 0) * (c.weight || 0), 0).toFixed(2);
  }, [criteria, currentScores]);

  const submitScore = useCallback(async (isAutoSubmit = false) => {
    if (hasScoredCurrentTeam) return;
    if (!canSubmitFinalScore && !isAutoSubmit) return;

    if (!isAutoSubmit && criteria.some(c => currentScores[c.id] === undefined)) {
        return message.warning("Vui lòng chấm đủ tiêu chí.");
    }

    setIsSubmitting(true);
    try {
      for (const c of criteria) {
          await judgeService.submitScore({
              submissionId: activeSlot.submissionId,
              criterionId: c.id,
              scoreValue: currentScores[c.id] || 0,
              comment: comment.trim(),
              scoreType: 'NORMAL'
          });
      }

      await judgeService.confirmSubmissionScoring(activeSlot.submissionId);

      const finalTotal = calculateTotal();
      
      setRawMyScores(prev => {
        const filtered = prev.filter(p => String(p.submissionId ?? p.submission_id) !== String(activeSlot.submissionId));
        return [
          ...filtered, 
          ...criteria.map(c => ({
             submissionId: activeSlot.submissionId,
             criterionId: c.id,
             scoreValue: currentScores[c.id] || 0,
             comment: comment.trim()
          }))
        ];
      });
      setMyScoredSubmissions(prev => ({ ...prev, [String(activeSlot.submissionId)]: finalTotal }));

      message.success(isAutoSubmit ? "Đã hết giờ Q&A! Hệ thống tự động nộp bài." : "Chốt điểm thành công! Form đã được khóa.");
      
      localStorage.setItem(`seal_draft_${assignmentId}_${activeSlot.submissionId}`, JSON.stringify({ 
         scores: currentScores, 
         comment: comment.trim() 
      }));

      setTimeout(async () => {
         await fetchStaticData();
         await fetchQueue(true);
      }, 1000);

    } catch (error) {
      message.error(error.message || "Lỗi lưu điểm.");
    } finally {
      setIsSubmitting(false);
    }
  }, [hasScoredCurrentTeam, canSubmitFinalScore, criteria, currentScores, activeSlot, comment, calculateTotal, fetchStaticData, fetchQueue, assignmentId]);

  useEffect(() => {
    if (localTimerPhase === 'QA' && localRemainingSeconds === 0 && activeSlot && !hasScoredCurrentTeam) {
      if (!isActionPendingRef.current && !isSubmitting) {
        isActionPendingRef.current = true;
        submitScore(true).finally(() => {
          isActionPendingRef.current = false;
        });
      }
    }
  }, [localTimerPhase, localRemainingSeconds, activeSlot, hasScoredCurrentTeam, isSubmitting, submitScore]);

  const handleTimerAction = useCallback(async (actionType) => {
    if (!isController || !activeSlot) return;

    isActionPendingRef.current = true;
    setIsTimerActionLoading(true);
    
    const previousEngineState = { ...timerEngineRef.current };
    
    let currentTick = previousEngineState.baseSeconds;
    if (previousEngineState.phase === 'PRESENTING' || previousEngineState.phase === 'QA') {
        const elapsed = Math.floor((Date.now() - previousEngineState.startTimeMs) / 1000);
        currentTick = Math.max(0, previousEngineState.baseSeconds - elapsed);
    }

    try {
      if (actionType === 'START_OR_RESUME') {
        const isResume = previousEngineState.phase === 'PAUSED';
        const targetPhase = isResume ? previousEngineState.originalPhase : 'PRESENTING';

        // Lấy đúng số phút Thuyết trình đã cấu hình * 60s
        if (!isResume) {
            currentTick = (activeSlot.timer?.presentationMinutes || 10) * 60;
        }

        applyEngineState(targetPhase, currentTick); 
        
        if (isResume) await presentationService.resumeTimer(roundId, trackId);
        else await presentationService.startTimer(roundId, trackId);
      }
      else if (actionType === 'PAUSE') {
        applyEngineState('PAUSED', currentTick); 
        await presentationService.pauseTimer(roundId, trackId);
      }
      else if (actionType === 'QA') {
        // Lấy đúng số phút QA đã cấu hình * 60s
        const qaSecs = (activeSlot.timer?.qaMinutes || 5) * 60;
        applyEngineState('QA', qaSecs); 
        
        await presentationService.qaTimer(roundId, trackId);
      }
      else if (actionType === 'NEXT') {
        await presentationService.advanceNext(roundId, trackId, { currentSubmissionId: activeSlot.submissionId });
      }
    } catch (error) {
      applyEngineState(previousEngineState.phase, previousEngineState.baseSeconds);
      // Bóc tách lỗi y như file QueuePage
      const beMsg = error?.response?.data?.error?.message || error?.response?.data?.message || error.message;
      message.error(beMsg || 'Lỗi điều khiển đồng hồ.');
    } finally {
      setIsTimerActionLoading(false);
      setTimeout(() => {
        isActionPendingRef.current = false;
        fetchQueue(true);
      }, 1000);
    }
  }, [isController, activeSlot, roundId, trackId, fetchQueue, applyEngineState]);

  return {
    isLoading, criteria, currentScores, comment, setComment: handleSetComment, handleScoreChange, calculateTotal, submitScore, isSubmitting,
    trackQueue, activeSlot, localTimerPhase, localRemainingSeconds, canScore, canSubmitFinalScore, isController, handleTimerAction, isTimerActionLoading,
    myScoredSubmissions, hasScoredCurrentTeam, isAllDone
  };
};