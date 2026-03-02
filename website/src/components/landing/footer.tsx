export function Footer() {
  return (
    <footer className="px-4 py-6 pb-8 md:px-6">
      <div className="mx-auto max-w-6xl rounded-3xl bg-surface px-8 py-8 md:px-16">
        <div className="flex flex-col items-center justify-between gap-4 sm:flex-row">
          <div className="flex items-center gap-3">
            <span className="text-lg font-extrabold text-ink">AutoRizz</span>
            <span className="text-sm text-ink-muted">
              &copy; {new Date().getFullYear()}
            </span>
          </div>
          <div className="flex gap-6 text-sm font-medium text-ink-light">
            <a href="#" className="hover:text-ink">
              Privacy
            </a>
            <a href="#" className="hover:text-ink">
              Terms
            </a>
            <a
              href="https://github.com/jordanthejet/cellclaw"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-ink"
            >
              GitHub
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
