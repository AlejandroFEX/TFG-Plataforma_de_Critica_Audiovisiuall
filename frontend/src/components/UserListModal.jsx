import { Link } from 'react-router-dom'
import Avatar from './Avatar'

export default function UserListModal({ title, users, loading, onClose }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 px-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-label={title}
        className="panel flex max-h-[70vh] w-full max-w-sm flex-col shadow-xl"
        onClick={e => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-line px-5 py-3.5">
          <h3 className="text-lg font-semibold">{title}</h3>
          <button
            onClick={onClose}
            aria-label="Cerrar"
            className="text-2xl leading-none text-ink-faint hover:text-ink"
          >
            ×
          </button>
        </div>

        <div className="overflow-y-auto p-2">
          {loading && (
            <p className="py-6 text-center text-sm text-ink-faint">Cargando…</p>
          )}
          {!loading && users?.length === 0 && (
            <p className="py-6 text-center text-sm text-ink-faint">Aún no hay nadie aquí.</p>
          )}
          {users?.map(u => (
            <Link
              key={u.id}
              to={`/profile/${u.id}`}
              onClick={onClose}
              className="flex items-center gap-3 rounded-md px-3 py-2 hover:bg-paper"
            >
              <Avatar src={u.avatarUrl} name={u.username} />
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{u.username}</p>
                <p className="text-xs text-ink-faint">{u.criticPoints.toFixed(2)} CP</p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}
