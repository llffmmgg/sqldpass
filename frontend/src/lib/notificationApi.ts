import { fetchApi } from "@/lib/api";

export interface NotificationDto {
  id: number;
  type: string;
  title: string;
  body: string | null;
  link: string | null;
  refId: number | null;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationListResponse {
  items: NotificationDto[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export function fetchNotifications(page = 0, size = 20) {
  return fetchApi<NotificationListResponse>(`/notifications?page=${page}&size=${size}`);
}

export function fetchUnreadCount() {
  return fetchApi<{ count: number }>("/notifications/unread-count");
}

export function markNotificationRead(id: number) {
  return fetchApi<{ ok: boolean }>(`/notifications/${id}/read`, { method: "PATCH" });
}

export function markAllNotificationsRead() {
  return fetchApi<{ updated: number }>("/notifications/read-all", { method: "PATCH" });
}
