import { useEffect, useState, type FormEvent } from 'react';
import { KeyRound, MoreHorizontal, Plus, ShieldCheck, UserRound, UsersRound } from 'lucide-react';
import api from '../services/api';
import type { Role } from '../types';
import { Button } from '../components/ui/Button';
import { Dialog } from '../components/ui/Dialog';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingState } from '../components/ui/LoadingState';
import { PageHeader } from '../components/ui/PageHeader';
import { StatusBadge } from '../components/ui/StatusBadge';
import { TextField } from '../components/ui/TextField';
import { formatDateTime } from '../utils/format';
import { getApiErrorMessage } from '../utils/api-error';
import { useAuth } from '../context/AuthContext';

interface ManagedUser {
    userId: string;
    username: string;
    role: Role;
    isActive: boolean;
    createdAt: string;
    lastLogin?: string | null;
}

async function fetchUsers(): Promise<ManagedUser[]> {
    const { data } = await api.get<ManagedUser[]>('/users');
    return data;
}

export default function UserManagementPage() {
    const { currentUser } = useAuth();
    const [users, setUsers] = useState<ManagedUser[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState<Role>('CASHIER');
    const [formError, setFormError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);
    const [selectedUser, setSelectedUser] = useState<ManagedUser | null>(null);
    const [editRole, setEditRole] = useState<Role>('CASHIER');
    const [editActive, setEditActive] = useState(true);
    const [managing, setManaging] = useState(false);
    const [manageError, setManageError] = useState<string | null>(null);
    const [passwordUser, setPasswordUser] = useState<ManagedUser | null>(null);
    const [newPassword, setNewPassword] = useState('');
    const [resettingPassword, setResettingPassword] = useState(false);
    const [passwordError, setPasswordError] = useState<string | null>(null);

    const retryLoadUsers = async () => {
        setLoading(true);
        setLoadError(null);
        try {
            setUsers(await fetchUsers());
        } catch (error) {
            setLoadError(getApiErrorMessage(error, 'Users could not be loaded.'));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        let active = true;
        fetchUsers()
            .then((data) => { if (active) setUsers(data); })
            .catch((error) => { if (active) setLoadError(getApiErrorMessage(error, 'Users could not be loaded.')); })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, []);

    const closeDialog = () => {
        if (saving) return;
        setDialogOpen(false);
        setUsername('');
        setPassword('');
        setRole('CASHIER');
        setFormError(null);
    };

    const createUser = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (saving) return;

        if (username.trim().length < 3) {
            setFormError('Username must be at least 3 characters.');
            return;
        }
        if (password.length < 8) {
            setFormError('Temporary password must be at least 8 characters.');
            return;
        }

        setSaving(true);
        setFormError(null);
        try {
            const { data } = await api.post<ManagedUser>('/users', {
                username: username.trim(),
                password,
                role,
            });
            setUsers((current) => [...current, data].sort((a, b) => a.username.localeCompare(b.username)));
            setDialogOpen(false);
            setUsername('');
            setPassword('');
            setRole('CASHIER');
        } catch (error) {
            setFormError(getApiErrorMessage(error, 'The account could not be created.'));
        } finally {
            setSaving(false);
        }
    };

    const openManage = (user: ManagedUser) => {
        setSelectedUser(user);
        setEditRole(user.role);
        setEditActive(user.isActive);
        setManageError(null);
    };

    const closeManage = () => {
        if (managing) return;
        setSelectedUser(null);
        setManageError(null);
    };

    const saveAccount = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (!selectedUser || managing || selectedUser.userId === currentUser?.userId) return;
        setManaging(true);
        setManageError(null);
        try {
            const { data } = await api.put<ManagedUser>(`/users/${selectedUser.userId}`, {
                role: editRole,
                isActive: editActive,
            });
            setUsers((current) => current.map((user) => user.userId === data.userId ? data : user));
            setSelectedUser(null);
        } catch (error) {
            setManageError(getApiErrorMessage(error, 'The account could not be updated.'));
        } finally {
            setManaging(false);
        }
    };

    const openPasswordReset = (user: ManagedUser) => {
        setSelectedUser(null);
        setPasswordUser(user);
        setNewPassword('');
        setPasswordError(null);
    };

    const closePasswordReset = () => {
        if (resettingPassword) return;
        setPasswordUser(null);
        setNewPassword('');
        setPasswordError(null);
    };

    const resetPassword = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (!passwordUser || resettingPassword) return;
        if (newPassword.length < 8) {
            setPasswordError('The new password must be at least 8 characters.');
            return;
        }
        setResettingPassword(true);
        setPasswordError(null);
        try {
            await api.patch(`/users/${passwordUser.userId}/password`, { newPassword });
            setPasswordUser(null);
            setNewPassword('');
        } catch (error) {
            setPasswordError(getApiErrorMessage(error, 'The password could not be reset.'));
        } finally {
            setResettingPassword(false);
        }
    };

    return (
        <div className="app-page">
            <PageHeader
                title="Users"
                description="Create secure accounts for store administrators and cashiers."
                action={(
                    <Button leadingIcon={<Plus aria-hidden="true" className="h-5 w-5" />} onClick={() => setDialogOpen(true)}>
                        <span className="hidden sm:inline">Add account</span>
                        <span className="sm:hidden">Add</span>
                    </Button>
                )}
            />

            <div className="mb-5 flex items-start gap-3 border-l-4 border-brand-600 bg-brand-50 px-4 py-3 text-sm text-brand-900">
                <ShieldCheck aria-hidden="true" className="mt-0.5 h-5 w-5 shrink-0" />
                <p>Only administrators can access this page. There is no public signup after the first owner account has been created.</p>
            </div>

            {loading && <LoadingState label="Loading user accounts" />}
            {loadError && (
                <div role="alert" className="border-y border-red-200 bg-red-50 px-4 py-5 text-sm text-red-700">
                    <p>{loadError}</p>
                    <Button variant="secondary" className="mt-4" onClick={() => void retryLoadUsers()}>Try again</Button>
                </div>
            )}
            {!loading && !loadError && users.length === 0 && (
                <EmptyState icon={<UsersRound aria-hidden="true" className="h-8 w-8" />} title="No accounts found" description="Create the first staff account for this store." action={<Button onClick={() => setDialogOpen(true)}>Add account</Button>} />
            )}
            {!loading && !loadError && users.length > 0 && (
                <div className="border-y border-line bg-white">
                    <div className="hidden grid-cols-[minmax(0,1.2fr)_0.7fr_0.7fr_1fr_auto] gap-4 border-b border-line px-4 py-3 text-xs font-semibold uppercase tracking-wide text-muted md:grid">
                        <span>Account</span><span>Role</span><span>Status</span><span>Last sign in</span><span className="sr-only">Actions</span>
                    </div>
                    <ul className="divide-y divide-line">
                        {users.map((user) => (
                            <li key={user.userId} className="grid grid-cols-[1fr_auto] gap-3 px-4 py-4 md:grid-cols-[minmax(0,1.2fr)_0.7fr_0.7fr_1fr_auto] md:items-center md:gap-4">
                                <div className="flex min-w-0 items-center gap-3">
                                    <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-gray-100 text-muted">
                                        <UserRound aria-hidden="true" className="h-5 w-5" />
                                    </span>
                                    <div className="min-w-0">
                                        <p className="truncate font-semibold text-ink">{user.username}</p>
                                        <p className="text-xs text-muted md:hidden">Created {formatDateTime(user.createdAt)}</p>
                                    </div>
                                </div>
                                <div><StatusBadge tone={user.role === 'ADMIN' ? 'warning' : 'neutral'}>{user.role === 'ADMIN' ? 'Administrator' : 'Cashier'}</StatusBadge></div>
                                <div><StatusBadge tone={user.isActive ? 'success' : 'danger'}>{user.isActive ? 'Active' : 'Inactive'}</StatusBadge></div>
                                <p className="text-sm text-muted">{user.lastLogin ? formatDateTime(user.lastLogin) : 'Never'}</p>
                                <Button variant="ghost" size="icon" aria-label={`Manage ${user.username}`} onClick={() => openManage(user)}><MoreHorizontal aria-hidden="true" className="h-5 w-5" /></Button>
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            <Dialog open={dialogOpen} title="Add a store account" description="The new user can sign in immediately with the temporary password." onClose={closeDialog}>
                <form onSubmit={createUser} noValidate className="space-y-4">
                    <TextField
                        id="new-username"
                        label="Username"
                        autoComplete="off"
                        value={username}
                        onChange={(event) => setUsername(event.target.value)}
                        disabled={saving}
                        hint="Use 3–50 letters, numbers, dots, underscores, or hyphens."
                    />
                    <TextField
                        id="new-password"
                        label="Temporary password"
                        type="password"
                        autoComplete="new-password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        disabled={saving}
                        hint="Use at least 8 characters and share it securely."
                    />
                    <div>
                        <label htmlFor="new-role" className="mb-1.5 block text-sm font-medium text-ink">Role</label>
                        <select id="new-role" value={role} onChange={(event) => setRole(event.target.value as Role)} disabled={saving} className="min-h-12 w-full rounded-lg border border-line bg-white px-3.5 text-base text-ink outline-none focus:border-brand-600 focus:ring-3 focus:ring-brand-100">
                            <option value="CASHIER">Cashier — sales access</option>
                            <option value="ADMIN">Administrator — full access</option>
                        </select>
                    </div>
                    {formError && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{formError}</p>}
                    <div className="grid grid-cols-2 gap-3 pt-2">
                        <Button type="button" variant="secondary" size="lg" disabled={saving} onClick={closeDialog}>Cancel</Button>
                        <Button type="submit" size="lg" loading={saving}>{saving ? 'Creating' : 'Create account'}</Button>
                    </div>
                </form>
            </Dialog>

            <Dialog open={selectedUser !== null} title={`Manage ${selectedUser?.username ?? 'account'}`} description={selectedUser?.userId === currentUser?.userId ? 'This is your current account. Its access cannot be changed while you are signed in.' : 'Change this staff member’s access or issue a new password.'} busy={managing} onClose={closeManage}>
                {selectedUser && <form onSubmit={saveAccount} className="space-y-4">
                    <div>
                        <label htmlFor="edit-role" className="mb-1.5 block text-sm font-medium text-ink">Role</label>
                        <select id="edit-role" value={editRole} onChange={(event) => setEditRole(event.target.value as Role)} disabled={managing || selectedUser.userId === currentUser?.userId} className="min-h-12 w-full rounded-lg border border-line bg-white px-3.5 text-base text-ink outline-none focus:border-brand-600 focus:ring-3 focus:ring-brand-100">
                            <option value="CASHIER">Cashier — sales access</option>
                            <option value="ADMIN">Administrator — full access</option>
                        </select>
                    </div>
                    <label className={`flex items-center justify-between gap-4 rounded-lg border border-line px-4 py-3 ${selectedUser.userId === currentUser?.userId ? 'opacity-60' : ''}`}>
                        <span><span className="block font-medium text-ink">Account active</span><span className="mt-0.5 block text-xs text-muted">Inactive accounts cannot sign in.</span></span>
                        <input type="checkbox" checked={editActive} disabled={managing || selectedUser.userId === currentUser?.userId} onChange={(event) => setEditActive(event.target.checked)} className="h-5 w-5 accent-brand-700" />
                    </label>
                    {manageError && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{manageError}</p>}
                    <Button type="button" variant="secondary" className="w-full" leadingIcon={<KeyRound aria-hidden="true" className="h-4 w-4" />} disabled={managing} onClick={() => openPasswordReset(selectedUser)}>Reset password</Button>
                    <div className="grid grid-cols-2 gap-3 pt-2">
                        <Button type="button" variant="secondary" size="lg" disabled={managing} onClick={closeManage}>Cancel</Button>
                        <Button type="submit" size="lg" loading={managing} disabled={selectedUser.userId === currentUser?.userId}>{managing ? 'Saving' : 'Save changes'}</Button>
                    </div>
                </form>}
            </Dialog>

            <Dialog open={passwordUser !== null} title={`Reset ${passwordUser?.username ?? 'account'} password`} description="The old password will stop working immediately. Share the new password securely." busy={resettingPassword} onClose={closePasswordReset}>
                <form onSubmit={resetPassword} className="space-y-4">
                    <TextField id="reset-password" label="New temporary password" type="password" autoComplete="new-password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} disabled={resettingPassword} hint="Use between 8 and 72 characters." />
                    {passwordError && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{passwordError}</p>}
                    <div className="grid grid-cols-2 gap-3 pt-2">
                        <Button type="button" variant="secondary" size="lg" disabled={resettingPassword} onClick={closePasswordReset}>Cancel</Button>
                        <Button type="submit" size="lg" loading={resettingPassword}>{resettingPassword ? 'Resetting' : 'Reset password'}</Button>
                    </div>
                </form>
            </Dialog>
        </div>
    );
}
