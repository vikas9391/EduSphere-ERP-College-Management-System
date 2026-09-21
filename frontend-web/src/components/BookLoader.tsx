import { useEffect, useState } from 'react'

export function BookLoader({ fullScreen = true, label = 'Loading your workspace…' }: { fullScreen?: boolean; label?: string }) {
  return (
    <div className={fullScreen ? 'fixed inset-0 z-[9999] flex items-center justify-center bg-[#F8F8F2]' : 'flex items-center justify-center py-10'}>
      <div className="flex flex-col items-center justify-center">
        <div className="book-loader" aria-hidden="true">
          <div className="book-loader__cover book-loader__cover--left" />
          <div className="book-loader__cover book-loader__cover--right" />
          <div className="book-loader__pages"><span /><span /><span /><span /></div>
          <div className="book-loader__spine" />
        </div>
        <div className="mt-7 text-center">
          <div className="text-[20px] font-extrabold tracking-tight text-[#1F2937]">Edu<span className="text-[#2E7D32]">Sphere</span></div>
          <div className="mt-1 text-sm text-[#6B7280]">{label}</div>
          <div className="mt-4 flex justify-center gap-1.5"><span className="book-loader__dot" /><span className="book-loader__dot book-loader__dot--2" /><span className="book-loader__dot book-loader__dot--3" /></div>
        </div>
      </div>
    </div>
  )
}

export function NavigationLoader() {
  const [visible, setVisible] = useState(true)

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | undefined
    const handle = () => {
      setVisible(true)
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => setVisible(false), 520)
    }
    window.addEventListener('popstate', handle)
    window.addEventListener('edusphere:navigation', handle)
    const initialTimer = setTimeout(() => setVisible(false), 650)
    return () => {
      clearTimeout(initialTimer)
      if (timer) clearTimeout(timer)
      window.removeEventListener('popstate', handle)
      window.removeEventListener('edusphere:navigation', handle)
    }
  }, [])

  if (!visible) return null
  return <BookLoader label="Opening your page…" />
}
