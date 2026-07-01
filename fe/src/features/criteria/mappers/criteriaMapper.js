export const mapCriterionToFE = (beData) => {
  if (!beData) return null;
  return {
    id: beData.id,
    track_id: beData.trackId,
    round_id: beData.roundId,
    source_criteria_id: beData.sourceCriteriaId,
    name: beData.name,
    type: beData.type,
    weight: beData.weight,
    max_score: beData.maxScore,
    description: beData.description,
    rubric_url: beData.rubricUrl,
    display_order: beData.displayOrder == null || beData.displayOrder < 1 ? 1 : beData.displayOrder,
  };
};

export const mapCriterionToBE = (feData) => {
  if (!feData) return null;
  const payload = {
    name: feData.name?.trim(),
    type: feData.type,
    weight: Number(feData.weight) || 0,
    maxScore: Number(feData.max_score) || 0,
    description: feData.description?.trim(),
    rubricUrl: feData.rubric_url?.trim() || null,
    displayOrder: feData.display_order ? Number(feData.display_order) : 1,
  };
  if (feData.id) {
    payload.id = feData.id;
  }
  return payload;
};