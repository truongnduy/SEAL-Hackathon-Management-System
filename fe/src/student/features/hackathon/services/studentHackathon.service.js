import axiosClient from '../../../../shared/api/axiosClient';
import { ENDPOINTS } from '../../../../shared/api/endpoints';

const unwrapList = (res) => {
  if (Array.isArray(res)) return res;
  if (Array.isArray(res?.data)) return res.data;
  if (Array.isArray(res?.items)) return res.items;
  return [];
};

const unwrapItem = (res) => res?.data || res || null;

export const studentHackathonService = {
  browse: async (status = 'ONGOING') => {
    const res = await axiosClient.get(ENDPOINTS.STUDENT_HACKATHONS.BROWSE, {
      params: status ? { status } : undefined,
    });
    return unwrapList(res);
  },

  getRegisteredHackathons: async (status = 'ONGOING') => {
    const items = await studentHackathonService.browse(status);
    return items.filter((item) => item.registered);
  },

  /** Hackathon sinh viên đã đăng ký thủ công — không lấy sự kiện ONGOING đầu tiên. */
  getPrimaryRegisteredHackathon: async (status = 'ONGOING') => {
    const registered = await studentHackathonService.getRegisteredHackathons(status);
    if (!registered.length) return null;
    return studentHackathonService.getHackathonDetail(registered[0].id);
  },

  getHackathonDetail: async (hackathonId) => {
    if (!hackathonId) return null;
    try {
      const detail = await axiosClient.get(ENDPOINTS.HACKATHONS.DETAIL(hackathonId));
      return unwrapItem(detail);
    } catch {
      return { id: hackathonId };
    }
  },

  register: async (hackathonId) => {
    return axiosClient.post(ENDPOINTS.STUDENT_HACKATHONS.REGISTER(hackathonId));
  },

  unregister: async (hackathonId) => {
    return axiosClient.delete(ENDPOINTS.STUDENT_HACKATHONS.REGISTER(hackathonId));
  },
};
