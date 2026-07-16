import Link from 'next/link';
import { type PostSummary } from '@/entities/post';
import { PostTime } from './PostTime';

export function PostCard({ post }: { post: PostSummary }) {
  return (
    <li>
      <Link
        href={`/posts/${post.id}`}
        className="group relative flex items-center gap-4 overflow-hidden rounded-xl border border-edge bg-surface px-5 py-4 shadow-card transition-[border-color,box-shadow,transform] duration-fast hover:-translate-y-0.5 hover:border-edge-strong hover:shadow-card-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
      >
        <span
          aria-hidden
          className="absolute inset-y-3 left-0 w-rail origin-center scale-y-0 rounded-full bg-accent opacity-0 transition-[transform,opacity] duration-moderate group-hover:scale-y-100 group-hover:opacity-100 group-focus-visible:scale-y-100 group-focus-visible:opacity-100"
        />
        <div className="min-w-0 flex-1">
          <h2 className="line-clamp-2 text-emphasis text-ink transition-colors duration-fast group-hover:text-accent">
            {post.title}
          </h2>
          <p className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-caption">
            <span className="text-label text-muted">{post.author}</span>
            <span aria-hidden className="text-faint">
              ·
            </span>
            <PostTime iso={post.createdAt} className="font-mono text-meta text-faint" />
          </p>
        </div>
        <span
          aria-hidden
          className="shrink-0 translate-x-0 font-mono text-faint transition-[transform,color] duration-moderate group-hover:translate-x-0.5 group-hover:text-accent"
        >
          →
        </span>
      </Link>
    </li>
  );
}
