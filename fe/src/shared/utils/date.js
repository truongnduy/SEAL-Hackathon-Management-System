import dayjs from 'dayjs';

export const formatDate = (date, format = 'YYYY-MM-DD HH:mm') => {
  if (!date) return '';
  return dayjs(date).format(format);
};

export const isAfter = (date1, date2) => {
  if (!date1 || !date2) return true;
  return dayjs(date1).isAfter(dayjs(date2)) || dayjs(date1).isSame(dayjs(date2));
};
