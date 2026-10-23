import { useEffect, useId, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
    BarChart3,
    ChevronRight,
    ClipboardCheck,
    History,
    Home,
    LogOut,
    Menu,
    Package,
    Settings,
    ShoppingBasket,
    SlidersHorizontal,
    Users,
    X,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { cn } from '../../utils/cn';

const tabs = [
    { label: 'Home', path: '/', icon: Home },
    { label: 'POS', path: '/pos', icon: ShoppingBasket },
    { label: 'Products', path: '/products', icon: Package },
] as const;

const drawerLinks = [
    { label: 'Sales history', path: '/sales', icon: History, adminOnly: false },
    { label: 'Stocktake', path: '/stocktake', icon: ClipboardCheck, adminOnly: true },
    { label: 'Stock movements', path: '/movements', icon: SlidersHorizontal, adminOnly: true },
    { label: 'Reports', path: '/reports', icon: BarChart3, adminOnly: false },
    { label: 'Users', path: '/settings/users', icon: Users, adminOnly: true },
    { label: 'Settings', path: '/settings', icon: Settings, adminOnly: false },
] as const;

export default function BottomNav() {
    const { currentUser, logout } = useAuth();
    const location = useLocation();
    const navigate = useNavigate();
    const titleId = useId();
    const [drawerOpen, setDrawerOpen] = useState(false);
    const isAdmin = currentUser?.role === 'ADMIN';

    const isActive = (path: string) =>
        path === '/' ? location.pathname === '/' : location.pathname.startsWith(path);
    const moreIsActive = drawerLinks.some((link) => isActive(link.path));

    useEffect(() => {
        if (!drawerOpen) return;
        const previousOverflow = document.body.style.overflow;
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') setDrawerOpen(false);
        };
        document.body.style.overflow = 'hidden';
        document.addEventListener('keydown', handleKeyDown);
        return () => {
            document.body.style.overflow = previousOverflow;
            document.removeEventListener('keydown', handleKeyDown);
        };
    }, [drawerOpen]);

    const goTo = (path: string) => {
        setDrawerOpen(false);
        navigate(path);
    };

    return (
        <>
            {drawerOpen && (
                <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center sm:p-6">
                    <button
                        type="button"
                        aria-label="Close More menu"
                        className="absolute inset-0 cursor-default bg-black/45"
                        onClick={() => setDrawerOpen(false)}
                    />
                    <section
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby={titleId}
                        className="relative w-full max-w-lg rounded-t-2xl bg-white px-5 pb-[calc(env(safe-area-inset-bottom)+1.25rem)] pt-4 sm:rounded-2xl"
                    >
                        <div className="mb-2 flex min-h-11 items-center justify-between border-b border-line pb-3">
                            <div>
                                <h2 id={titleId} className="text-lg font-semibold text-ink">More</h2>
                                <p className="text-sm text-muted">Signed in as {currentUser?.username}</p>
                            </div>
                            <button
                                type="button"
                                onClick={() => setDrawerOpen(false)}
                                aria-label="Close"
                                className="flex h-11 w-11 items-center justify-center rounded-lg text-muted hover:bg-gray-100"
                            >
                                <X aria-hidden="true" className="h-5 w-5" />
                            </button>
                        </div>
                        <nav aria-label="More navigation">
                            {drawerLinks
                                .filter((link) => !link.adminOnly || isAdmin)
                                .map(({ label, path, icon: Icon }) => (
                                    <button
                                        key={path}
                                        type="button"
                                        onClick={() => goTo(path)}
                                        className={cn(
                                            'interactive-row text-left text-sm font-medium',
                                            isActive(path) && 'text-brand-700',
                                        )}
                                    >
                                        <Icon aria-hidden="true" className="mr-3 h-5 w-5 shrink-0" />
                                        <span className="flex-1">{label}</span>
                                        <ChevronRight aria-hidden="true" className="h-4 w-4 text-gray-400" />
                                    </button>
                                ))}
                            <button
                                type="button"
                                onClick={logout}
                                className="flex min-h-13 w-full items-center py-3 text-left text-sm font-semibold text-red-700"
                            >
                                <LogOut aria-hidden="true" className="mr-3 h-5 w-5" />
                                Log out
                            </button>
                        </nav>
                    </section>
                </div>
            )}

            <nav
                aria-label="Primary navigation"
                className="fixed inset-x-0 bottom-0 z-40 border-t border-line bg-white/95 backdrop-blur-sm"
                style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
            >
                <div className="mx-auto grid max-w-lg grid-cols-4">
                    {tabs.map(({ label, path, icon: Icon }) => {
                        const active = isActive(path);
                        return (
                            <button
                                key={path}
                                type="button"
                                onClick={() => navigate(path)}
                                aria-current={active ? 'page' : undefined}
                                className={cn(
                                    'flex min-h-16 flex-col items-center justify-center gap-1 px-2 text-xs font-medium transition-colors',
                                    active ? 'text-brand-700' : 'text-gray-500 hover:text-ink',
                                )}
                            >
                                <Icon aria-hidden="true" className="h-5 w-5" strokeWidth={active ? 2.4 : 1.9} />
                                <span>{label}</span>
                            </button>
                        );
                    })}
                    <button
                        type="button"
                        onClick={() => setDrawerOpen(true)}
                        aria-expanded={drawerOpen}
                        aria-haspopup="dialog"
                        className={cn(
                            'flex min-h-16 flex-col items-center justify-center gap-1 px-2 text-xs font-medium transition-colors',
                            moreIsActive ? 'text-brand-700' : 'text-gray-500 hover:text-ink',
                        )}
                    >
                        <Menu aria-hidden="true" className="h-5 w-5" strokeWidth={moreIsActive ? 2.4 : 1.9} />
                        <span>More</span>
                    </button>
                </div>
            </nav>
        </>
    );
}
