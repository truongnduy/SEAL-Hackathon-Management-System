import axiosClient from '../../../shared/api/axiosClient';
import { ENDPOINTS } from '../../../shared/api/endpoints';

const mapNotification = (item) => ({
  id: item.id,
  type: item.type || 'REMINDER',
  title: item.title || 'Thông báo',
  description: item.body || item.description || '',
  time: item.sentAt || item.time || '',
  is_read: Boolean(item.isRead ?? item.is_read),
});

export const notificationService = {
  list: async (unreadOnly) => {
    const data = await axiosClient.get(ENDPOINTS.NOTIFICATIONS.ME, {
      params: unreadOnly ? { unreadOnly: true } : {},
    });
    const items = Array.isArray(data) ? data : data?.items || [];
    return items.map(mapNotification);
  },

  markRead: (notificationIds) =>
    axiosClient.patch(ENDPOINTS.NOTIFICATIONS.MARK_READ, { notificationIds }),

  mapNotification,
};
