import { Outlet } from 'react-router-dom';
import BottomNav from './BottomNav';

export default function AppShell() {
    return (
        <div className="min-h-screen bg-canvas text-ink">
            <a href="#main-content" className="fixed left-3 top-3 z-[70] -translate-y-20 rounded-lg bg-ink px-4 py-3 text-sm font-semibold text-white focus:translate-y-0">
                Skip to content
            </a>
            <main id="main-content" className="min-h-screen"><Outlet /></main>
            <BottomNav />
        </div>
    );
}
