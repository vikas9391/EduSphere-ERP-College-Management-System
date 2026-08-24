import { api } from './axios'

export type AudienceType =
  | 'ALL_STUDENTS'
  | 'ALL_TEACHERS'
  | 'CLASS_STUDENTS'
  | 'CLASS_TEACHERS'
  | 'DEPARTMENT_STUDENTS'
  | 'DEPARTMENT_TEACHERS'

export interface Announcement {
  id: number
  title: string
  message: string
  audienceType: AudienceType
  audienceId?: number | null
  senderName: string
  createdAt: string
  read: boolean
}

export interface AudienceOption {
  type: AudienceType
  id?: number | null
  label: string
}

export interface AnnouncementContact {
  id: number
  name: string
  email: string
  phone?: string | null
  role: string
}

export interface CreateAnnouncementRequest {
  title: string
  message: string
  audienceType: AudienceType
  audienceId?: number | null
}

export async function getAnnouncements() {
  const { data } = await api.get<Announcement[]>('/announcements')
  return data
}

export async function getAnnouncementUnreadCount() {
  const { data } = await api.get<number>('/announcements/unread-count')
  return data
}

export async function getAnnouncementOptions() {
  const { data } = await api.get<AudienceOption[]>('/announcements/options')
  return data
}

export async function createAnnouncement(request: CreateAnnouncementRequest) {
  const { data } = await api.post<Announcement>('/announcements', request)
  return data
}

export async function markAnnouncementRead(id: number) {
  await api.post(`/announcements/${id}/read`)
}

export async function markAllAnnouncementsRead() {
  await api.post('/announcements/read-all')
}

export async function getAnnouncementContacts() {
  const { data } = await api.get<AnnouncementContact[]>('/announcements/contacts')
  return data
}
