import { Link } from 'react-router-dom'

export default function UserListModal({ title, users, loading, onClose }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl w-full max-w-sm max-h-[70vh] flex flex-col shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h3 className="font-semibold text-gray-900 text-sm">{title}</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-xl leading-none"
          >
            ×
          </button>
        </div>
        <div className="overflow-y-auto p-2">
          {loading && (
            <p className="text-center text-gray-400 text-sm py-6">Cargando...</p>
          )}
          {!loading && users?.length === 0 && (
            <p className="text-center text-gray-400 text-sm py-6">
              Aún no hay usuarios aquí.
            </p>
          )}
          {users?.map((u) => (
            <Link
              key={u.id}
              to={`/profile/${u.id}`}
              onClick={onClose}
              className="flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-gray-50"
            >
              {u.avatarUrl ? (
                <img
                  src={u.avatarUrl}
                  alt={u.username}
                  className="w-9 h-9 rounded-full object-cover"
                />
              ) : (
                <div className="w-9 h-9 rounded-full bg-indigo-100 flex items-center justify-center text-xs font-semibold text-indigo-700">
                  {u.username.slice(0, 2).toUpperCase()}
                </div>
              )}
              <div className="min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">{u.username}</p>
                <p className="text-xs text-gray-400">{u.criticPoints.toFixed(2)} CP</p>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  )
}