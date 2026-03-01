export function Footer() {
  return (
    <footer className="border-t border-gray-200 bg-gray-50 py-8">
      <div className="mx-auto max-w-6xl px-6">
        <div className="flex flex-col items-center justify-between gap-4 sm:flex-row">
          <div className="text-sm text-gray-500">
            &copy; {new Date().getFullYear()} AutoRizz. Built at Sundai Club.
          </div>
          <div className="flex gap-6 text-sm text-gray-500">
            <a href="#" className="hover:text-gray-700">
              Privacy Policy
            </a>
            <a href="#" className="hover:text-gray-700">
              Terms of Service
            </a>
            <a
              href="https://github.com/jordanthejet/cellclaw"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-gray-700"
            >
              GitHub
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
