const SIZES = {
  sm: 'w-14',
  md: 'w-20 sm:w-24',
}

/** Póster con proporción 2:3; si no hay imagen, un hueco neutro. */
export default function Poster({ url, title, size = 'md' }) {
  const sizeClass = SIZES[size] ?? SIZES.md

  if (url) {
    return (
      <img
        src={url}
        alt={title}
        loading="lazy"
        className={`${sizeClass} aspect-[2/3] shrink-0 rounded object-cover ring-1 ring-black/10`}
      />
    )
  }

  return (
    <div
      className={`${sizeClass} flex aspect-[2/3] shrink-0 items-center justify-center rounded bg-line p-1 text-center text-[10px] text-ink-faint ring-1 ring-black/5`}
    >
      Sin póster
    </div>
  )
}
