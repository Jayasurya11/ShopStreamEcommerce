import { Component, type ReactNode } from 'react';

/** Last line of defence: a rendering bug shows a friendly message instead of a blank page. */
export default class ErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() { return { failed: true }; }
  componentDidCatch(error: unknown) { console.error('UI error', error); }

  render() {
    if (!this.state.failed) return this.props.children;
    return (
      <main>
        <h1>Something went wrong</h1>
        <p className="muted">An unexpected error occurred. Reload the page to try again.</p>
        <button onClick={() => window.location.reload()}>Reload</button>
      </main>
    );
  }
}
