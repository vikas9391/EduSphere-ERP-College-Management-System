import { FormEvent, useEffect, useMemo, useState } from 'react'
import { Bell, CheckCheck, Loader2, Mail, Megaphone, Phone, Send, Users } from 'lucide-react'
import { toast } from 'sonner'
import { Layout } from '@/components/Layout'
import { useAuthStore } from '@/store/authStore'
import {
  createAnnouncement,
  getAnnouncementContacts,
  getAnnouncementOptions,
  getAnnouncements,
  markAllAnnouncementsRead,
  markAnnouncementRead,
  type Announcement,
  type AnnouncementContact,
  type AudienceOption,
} from '@/api/announcements'

const typeLabel: Record<string, string> = {
  ALL_STUDENTS: 'All students',
  ALL_TEACHERS: 'All teachers',
  CLASS_STUDENTS: 'Students in a class',
  CLASS_TEACHERS: 'Teachers in a class',
  DEPARTMENT_STUDENTS: 'Students in a department',
  DEPARTMENT_TEACHERS: 'Teachers in a department',
}

export function AnnouncementsPage() {
  const { user } = useAuthStore()
  const [announcements, setAnnouncements] = useState<Announcement[]>([])
  const [options, setOptions] = useState<AudienceOption[]>([])
  const [contacts, setContacts] = useState<AnnouncementContact[]>([])
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [markingAllRead, setMarkingAllRead] = useState(false)
  const [title, setTitle] = useState('')
  const [message, setMessage] = useState('')
  const [selectedOption, setSelectedOption] = useState('')

  const unreadCount = announcements.filter((a) => !a.read).length
  const canSend = options.length > 0 && user?.role !== 'STUDENT'

  async function load() {
    setLoading(true)
    try {
      const [received, audienceOptions, people] = await Promise.all([
        getAnnouncements(),
        getAnnouncementOptions(),
        getAnnouncementContacts(),
      ])
      setAnnouncements(received)
      setOptions(audienceOptions)
      setContacts(people)
      if (!selectedOption && audienceOptions.length) {
        const first = audienceOptions[0]
        setSelectedOption(`${first.type}:${first.id ?? ''}`)
      }
    } catch {
      toast.error('Could not load announcements')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [])

  const selected = useMemo(() => {
    const [type, id] = selectedOption.split(':')
    return options.find((o) => o.type === type && String(o.id ?? '') === id)
  }, [options, selectedOption])

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!selected || !title.trim() || !message.trim()) return
    setSending(true)
    try {
      await createAnnouncement({
        title: title.trim(),
        message: message.trim(),
        audienceType: selected.type,
        audienceId: selected.id ?? null,
      })
      toast.success('Announcement sent')
      setTitle('')
      setMessage('')
      await load()
    } catch {
      toast.error('Could not send announcement')
    } finally {
      setSending(false)
    }
  }

  async function markRead(id: number) {
    try {
      await markAnnouncementRead(id)
      setAnnouncements((items) => items.map((a) => a.id === id ? { ...a, read: true } : a))
    } catch {
      toast.error('Could not update announcement')
    }
  }

  async function markAllRead() {
    if (unreadCount === 0 || markingAllRead) return
    setMarkingAllRead(true)
    try {
      await markAllAnnouncementsRead()
      setAnnouncements((items) => items.map((a) => ({ ...a, read: true })))
      toast.success('All announcements marked as read')
    } catch {
      toast.error('Could not mark all announcements as read')
    } finally {
      setMarkingAllRead(false)
    }
  }

  return (
    <Layout>
      <div className="mb-7 flex items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <Megaphone className="text-primary" size={22} />
            <h1 className="font-heading text-2xl font-medium text-text">Announcements</h1>
          </div>
          <p className="mt-1 text-sm text-muted">Send important messages and keep up with announcements from your college.</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">{unreadCount} unread</span>
          {unreadCount > 0 && (
            <button
              type="button"
              onClick={() => void markAllRead()}
              disabled={markingAllRead}
              className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium text-muted hover:text-primary disabled:opacity-60"
            >
              {markingAllRead ? <Loader2 size={14} className="animate-spin" /> : <CheckCheck size={14} />}
              {markingAllRead ? 'Marking...' : 'Mark all as read'}
            </button>
          )}
        </div>
      </div>

      {canSend && (
        <form onSubmit={submit} className="mb-8 rounded-2xl border border-border bg-bg p-5">
          <div className="mb-4 flex items-center gap-2 font-medium text-text"><Send size={17} className="text-primary" /> Send announcement</div>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="text-sm text-muted">Audience
              <select value={selectedOption} onChange={(e) => setSelectedOption(e.target.value)} className="mt-1 w-full rounded-xl border border-border bg-card px-3 py-2.5 text-text">
                {options.map((o) => <option key={`${o.type}:${o.id ?? ''}`} value={`${o.type}:${o.id ?? ''}`}>{o.label}</option>)}
              </select>
            </label>
            <label className="text-sm text-muted">Title
              <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={180} required className="mt-1 w-full rounded-xl border border-border bg-card px-3 py-2.5 text-text" placeholder="Announcement title" />
            </label>
          </div>
          <label className="mt-4 block text-sm text-muted">Message
            <textarea value={message} onChange={(e) => setMessage(e.target.value)} required rows={4} className="mt-1 w-full rounded-xl border border-border bg-card px-3 py-2.5 text-text" placeholder="Write the announcement..." />
          </label>
          <button disabled={sending} className="mt-4 inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-white disabled:opacity-60">
            {sending ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />} Send
          </button>
          {user?.role === 'TEACHER' && <p className="mt-2 text-xs text-muted">Teachers can send only to students in their classes or departments.</p>}
        </form>
      )}

      <div className="grid gap-6 xl:grid-cols-[1fr_320px]">
        <section>
          <div className="mb-3 flex items-center gap-2 font-medium text-text"><Bell size={17} className="text-primary" /> Received announcements</div>
          {loading ? <div className="flex h-32 items-center justify-center text-muted"><Loader2 className="animate-spin" /></div> : announcements.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-border p-8 text-center text-sm text-muted">No announcements yet.</div>
          ) : (
            <div className="space-y-3">
              {announcements.map((a) => (
                <article key={a.id} className={`rounded-2xl border p-5 ${a.read ? 'border-border bg-card' : 'border-primary/30 bg-primary/5'}`}>
                  <div className="flex items-start justify-between gap-3">
                    <div><h2 className="font-medium text-text">{a.title}</h2><p className="mt-1 text-xs text-muted">From {a.senderName} · {new Date(a.createdAt).toLocaleString()}</p></div>
                    {!a.read && <button onClick={() => void markRead(a.id)} className="inline-flex shrink-0 items-center gap-1 rounded-lg border border-border px-2.5 py-1.5 text-xs text-muted hover:text-primary"><CheckCheck size={14} /> Mark read</button>}
                  </div>
                  <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-text">{a.message}</p>
                  <p className="mt-3 text-xs text-muted">{typeLabel[a.audienceType]}</p>
                </article>
              ))}
            </div>
          )}
        </section>

        <aside>
          <div className="mb-3 flex items-center gap-2 font-medium text-text"><Users size={17} className="text-primary" /> People you can contact</div>
          <div className="rounded-2xl border border-border bg-card p-3">
            {contacts.length === 0 ? <p className="p-4 text-sm text-muted">No teacher contacts available yet.</p> : contacts.map((person) => (
              <div key={person.id} className="border-b border-border px-2 py-3 last:border-0">
                <p className="font-medium text-text">{person.name}</p>
                <p className="text-xs text-muted">{person.role}</p>
                <div className="mt-2 flex gap-2">
                  <a href={`mailto:${person.email}`} className="inline-flex items-center gap-1 rounded-lg bg-bg px-2 py-1 text-xs text-muted hover:text-primary"><Mail size={13} /> Email</a>
                  {person.phone && <a href={`tel:${person.phone}`} className="inline-flex items-center gap-1 rounded-lg bg-bg px-2 py-1 text-xs text-muted hover:text-primary"><Phone size={13} /> Call</a>}
                </div>
              </div>
            ))}
          </div>
        </aside>
      </div>
    </Layout>
  )
}
