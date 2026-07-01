// src/features/judging/services/calibrationService.js
import axiosClient from '../../../shared/api/axiosClient';
import { ENDPOINTS } from '../../../shared/api/endpoints';

export const calibrationService = {
  listByRound: (roundId) =>
    axiosClient.get(ENDPOINTS.CALIBRATION.BASE, { params: { roundId } }),

  create: (payload) =>
    axiosClient.post(ENDPOINTS.CALIBRATION.BASE, payload),

  update: (id, payload) =>
    axiosClient.patch(ENDPOINTS.CALIBRATION.DETAIL(id), payload),
};
