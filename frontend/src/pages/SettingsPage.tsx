import { useState, type FormEvent, type ReactNode } from 'react';
import { ChevronRight, KeyRound, Layers3, LogOut, ShieldCheck, UserRound, UsersRound } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Button } from '../components/ui/Button';
import { Dialog } from '../components/ui/Dialog';
import { PageHeader } from '../components/ui/PageHeader';
import { StatusBadge } from '../components/ui/StatusBadge';
import { TextField } from '../components/ui/TextField';
import { getApiErrorMessage } from '../utils/api-error';

export default function SettingsPage() {
    const { currentUser, logout } = useAuth();
    const navigate = useNavigate();
    const [passwordOpen, setPasswordOpen] = useState(false);
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [message, setMessage] = useState<string | null>(null);
    const isAdmin = currentUser?.role === 'ADMIN';

    const closePassword = () => {
        if (saving) return;
        setPasswordOpen(false);
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
        setError(null);
    };

    const changePassword = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (saving) return;
        if (newPassword.length < 8 || newPassword.length > 72) {
            setError('The new password must be between 8 and 72 characters.');
            return;
        }
        if (newPassword !== confirmPassword) {
            setError('The new passwords do not match.');
            return;
        }
        setSaving(true);
        setError(null);
        try {
            await api.patch('/users/me/password', { currentPassword, newPassword });
            setPasswordOpen(false);
            setCurrentPassword('');
            setNewPassword('');
            setConfirmPassword('');
            setMessage('Your password has been changed successfully.');
        } catch (caught) {
            setError(getApiErrorMessage(caught, 'Your password could not be changed.'));
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="app-page">
            <PageHeader title="Settings" description="Manage your account, security and store tools." />

            {message && <div role="status" className="mb-6 flex items-start gap-3 border-l-4 border-brand-700 bg-brand-50 px-4 py-3 text-sm text-brand-900"><ShieldCheck aria-hidden="true" className="mt-0.5 h-5 w-5 shrink-0" /><span className="flex-1">{message}</span><button type="button" className="font-semibold" onClick={() => setMessage(null)}>Dismiss</button></div>}

            <section className="mb-8">
                <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted">Your account</h2>
                <div className="border-y border-line bg-white">
                    <div className="flex items-center gap-4 px-4 py-5">
                        <span className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-brand-50 text-brand-700"><UserRound aria-hidden="true" className="h-5 w-5" /></span>
                        <div className="min-w-0 flex-1"><p className="truncate font-semibold text-ink">{currentUser?.username}</p><p className="mt-0.5 text-sm text-muted">Store account</p></div>
                        <StatusBadge tone={isAdmin ? 'warning' : 'neutral'}>{isAdmin ? 'Administrator' : 'Cashier'}</StatusBadge>
                    </div>
                    <SettingsRow icon={<KeyRound className="h-5 w-5" />} title="Change password" description="Confirm your current password first" onClick={() => { setMessage(null); setPasswordOpen(true); }} />
                </div>
            </section>

            {isAdmin && <section className="mb-8">
                <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted">Administration</h2>
                <div className="border-y border-line bg-white">
                    <SettingsRow icon={<UsersRound className="h-5 w-5" />} title="Staff accounts" description="Create users, roles and password resets" onClick={() => navigate('/settings/users')} />
                    <SettingsRow icon={<Layers3 className="h-5 w-5" />} title="Product categories" description="Organise the product catalogue" onClick={() => navigate('/products/categories')} />
                </div>
            </section>}

            <section>
                <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted">Session</h2>
                <div className="border-y border-line bg-white">
                    <button type="button" onClick={logout} className="flex min-h-16 w-full items-center gap-4 px-4 py-3 text-left text-red-700 hover:bg-red-50">
                        <LogOut aria-hidden="true" className="h-5 w-5 shrink-0" />
                        <span className="flex-1"><span className="block font-semibold">Log out</span><span className="mt-0.5 block text-sm text-red-600">End this session on this device</span></span>
                        <ChevronRight aria-hidden="true" className="h-4 w-4" />
                    </button>
                </div>
                <p className="mt-4 text-center text-xs text-muted">e-SpazaSethu · Secure local store management</p>
            </section>

            <Dialog open={passwordOpen} title="Change your password" description="Your current session will remain active after the change." busy={saving} onClose={closePassword}>
                <form onSubmit={changePassword} className="space-y-4">
                    <TextField id="current-password" label="Current password" type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} disabled={saving} />
                    <TextField id="new-password" label="New password" type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} disabled={saving} hint="Use between 8 and 72 characters." />
                    <TextField id="confirm-password" label="Confirm new password" type="password" autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} disabled={saving} />
                    {error && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
                    <div className="grid grid-cols-2 gap-3 pt-2">
                        <Button type="button" variant="secondary" size="lg" disabled={saving} onClick={closePassword}>Cancel</Button>
                        <Button type="submit" size="lg" loading={saving}>{saving ? 'Saving' : 'Change password'}</Button>
                    </div>
                </form>
            </Dialog>
        </div>
    );
}

function SettingsRow({ icon, title, description, onClick }: { icon: ReactNode; title: string; description: string; onClick: () => void }) {
    return <button type="button" onClick={onClick} className="flex min-h-16 w-full items-center gap-4 border-b border-line px-4 py-3 text-left last:border-b-0 hover:bg-gray-50"><span className="text-brand-700">{icon}</span><span className="min-w-0 flex-1"><span className="block font-semibold text-ink">{title}</span><span className="mt-0.5 block text-sm text-muted">{description}</span></span><ChevronRight aria-hidden="true" className="h-4 w-4 shrink-0 text-gray-400" /></button>;
}
