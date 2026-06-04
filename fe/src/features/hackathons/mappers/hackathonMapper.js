import dayjs from 'dayjs';

export const mapHackathonToFE = (beData) => {
  if (!beData) return null;
  return {
    ...beData,
    registration_start: beData.registrationStart,
    registration_end: beData.registrationEnd,
    event_start: beData.eventStart,
    event_end: beData.eventEnd,
    wildcard_enabled: beData.wildcardEnabled,
    individual_ranking_enabled: beData.individualRankingEnabled,
    banner_url: beData.bannerUrl,
  };
};

export const mapHackathonToBE = (feData) => {
  if (!feData) return null;
  return {
    name: feData.name,
    slug: feData.slug,
    season: feData.season,
    year: feData.year ? parseInt(feData.year) : null,
    description: feData.description,
    rules: feData.rules,
    bannerUrl: feData.banner_url,
    registrationStart: feData.registration_start ? dayjs(feData.registration_start).format('YYYY-MM-DD') : null,
    registrationEnd: feData.registration_end ? dayjs(feData.registration_end).format('YYYY-MM-DD') : null,
    eventStart: feData.event_start ? dayjs(feData.event_start).format('YYYY-MM-DD') : null,
    eventEnd: feData.event_end ? dayjs(feData.event_end).format('YYYY-MM-DD') : null,
    wildcardEnabled: feData.wildcard_enabled,
    individualRankingEnabled: feData.individual_ranking_enabled,
  };
};
