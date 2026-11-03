const SIZES = {
  sm: 'h-7 w-7 text-[11px]',
  md: 'h-10 w-10 text-sm',
  lg: 'h-20 w-20 text-2xl',
}

/** Foto de perfil, o las iniciales del usuario si no tiene. */
export default function Avatar({ src, name = '?', size = 'md' }) {
  const sizeClass = SIZES[size] ?? SIZES.md

  if (src) {
    return (
      <img
        src={src}
        alt={name}
        className={`${sizeClass} shrink-0 rounded-full object-cover ring-1 ring-black/10`}
      />
    )
  }

  return (
    <div
      className={`${sizeClass} flex shrink-0 items-center justify-center rounded-full bg-line font-semibold text-ink-soft`}
    >
      {name.slice(0, 2).toUpperCase()}
    </div>
  )
}
