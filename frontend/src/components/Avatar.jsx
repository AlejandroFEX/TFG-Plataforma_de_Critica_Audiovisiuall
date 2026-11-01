const SIZES = {
  sm: 'w-7 h-7 text-xs',
  md: 'w-10 h-10 text-sm',
  lg: 'w-20 h-20 text-2xl',
}

/** Foto de perfil, o las iniciales del usuario si no tiene. */
export default function Avatar({ src, name = '?', size = 'md' }) {
  const sizeClass = SIZES[size] ?? SIZES.md

  if (src) {
    return (
      <img
        src={src}
        alt={name}
        className={`${sizeClass} rounded-full object-cover border border-gray-100 flex-shrink-0`}
      />
    )
  }

  return (
    <div
      className={`${sizeClass} rounded-full bg-indigo-100 text-indigo-700 font-semibold flex items-center justify-center flex-shrink-0`}
    >
      {name.slice(0, 2).toUpperCase()}
    </div>
  )
}
