import { useState, useEffect, useCallback } from "react";
import { reviewService } from "../services/reviewService";
import { hackathonService } from "../../hackathons/services/hackathonService";
import { mapHackathonToFE } from "../../hackathons/mappers/hackathonMapper";

// === HOOK: LẤY DỮ LIỆU ĐỂ KIỂM TRA ĐIỀU KIỆN PHÁT HÀNH ===
export const useReadiness = (hackathonId) => {
  const [readinessData, setReadinessData] = useState(null);
  const [hackathon, setHackathon] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Hàm gọi API
  const fetchReadiness = useCallback(async () => {
    try {
      setIsLoading(true);
      const [hRes, rRes] = await Promise.all([
        hackathonService.getById(hackathonId),
        reviewService.checkReadiness(hackathonId),
      ]);
      setHackathon(mapHackathonToFE(hRes));
      setReadinessData(rRes?.data || rRes);
      setError(null);
    } catch (error) {
      console.error("Error fetching readiness:", error);
      setError(error);
    } finally {
      setIsLoading(false);
    }
  }, [hackathonId]);

  useEffect(() => {
    fetchReadiness();
  }, [fetchReadiness]);

  return {
    hackathon,
    readinessData,
    isLoading,
    error,
    refetch: fetchReadiness,
  };
};
