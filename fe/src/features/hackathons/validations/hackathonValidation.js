import dayjs from 'dayjs';

export const validateHackathon = (values) => {
  const errors = {};

  if (!values.name) errors.name = 'Name is required';
  if (!values.slug) errors.slug = 'Slug is required';
  if (!values.season) errors.season = 'Season is required';
  if (!values.year) errors.year = 'Year is required';

  if (values.registration_start && values.registration_end) {
    if (dayjs(values.registration_end).isBefore(dayjs(values.registration_start))) {
      errors.registration_end = 'Registration end must be after or equal to start';
    }
  }

  if (values.event_start && values.event_end) {
    if (dayjs(values.event_end).isBefore(dayjs(values.event_start))) {
      errors.event_end = 'Event end must be after or equal to start';
    }
  }

  if (values.event_start && values.registration_end) {
    if (dayjs(values.event_start).isBefore(dayjs(values.registration_end))) {
      errors.event_start = 'Event start must be after or equal to registration end';
    }
  }

  return errors;
};
