import { useEffect, useState, type FormEvent } from 'react';
import { ArrowLeft, Eye, EyeOff, LockKeyhole, Store, UserRound } from 'lucide-react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import type { LoginResponse } from '../types';
import { Button } from '../components/ui/Button';
import { LoadingState } from '../components/ui/LoadingState';
import { TextField } from '../components/ui/TextField';
import { getApiErrorMessage } from '../utils/api-error';

type SetupState = 'checking' | 'available' | 'closed' | 'error';

export default function SetupOwnerPage() {
    const navigate = useNavigate();
    const { token, currentUser, login } = useAuth();
    const [setupState, setSetupState] = useState<SetupState>('checking');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        api.get<{ setupRequired: boolean }>('/auth/setup-status')
            .then(({ data }) => setSetupState(data.setupRequired ? 'available' : 'closed'))
            .catch(() => setSetupState('error'));
    }, []);

    if (token && currentUser) return <Navigate to="/" replace />;

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (isSubmitting) return;

        if (username.trim().length < 3) {
            setError('Username must be at least 3 characters.');
            return;
        }
        if (password.length < 8) {
            setError('Password must be at least 8 characters.');
            return;
        }
        if (password !== confirmPassword) {
            setError('Passwords do not match.');
            return;
        }

        setError(null);
        setIsSubmitting(true);
        try {
            await api.post('/auth/setup-owner', { username: username.trim(), password });
            const { data } = await api.post<LoginResponse>('/auth/login', { username: username.trim(), password });
            login(data.token, data.user);
            navigate('/', { replace: true });
        } catch (requestError) {
            setError(getApiErrorMessage(requestError, 'The owner account could not be created.'));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <main className="min-h-screen bg-canvas px-5 py-8 sm:px-8">
            <div className="mx-auto w-full max-w-lg">
                <Link to="/login" className="mb-12 inline-flex min-h-11 items-center gap-2 rounded-lg pr-3 text-sm font-semibold text-ink hover:bg-gray-100">
                    <ArrowLeft aria-hidden="true" className="h-5 w-5" /> Back to sign in
                </Link>

                <div className="mb-8 flex items-center gap-3">
                    <span className="grid h-12 w-12 place-items-center rounded-xl bg-brand-700 text-white">
                        <Store aria-hidden="true" className="h-6 w-6" />
                    </span>
                    <div>
                        <p className="text-sm font-semibold text-brand-700">Private store setup</p>
                        <h1 className="text-2xl font-semibold tracking-tight text-ink">Create the owner account</h1>
                    </div>
                </div>

                {setupState === 'checking' && <LoadingState label="Checking store setup" />}

                {setupState === 'closed' && (
                    <div className="border-y border-line py-8">
                        <h2 className="text-lg font-semibold text-ink">Store setup is complete</h2>
                        <p className="mt-2 text-muted">For security, additional accounts can only be created by a signed-in administrator.</p>
                        <Button className="mt-6" onClick={() => navigate('/login')}>Go to sign in</Button>
                    </div>
                )}

                {setupState === 'error' && (
                    <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
                        The backend could not be reached. Start it and refresh this page.
                    </div>
                )}

                {setupState === 'available' && (
                    <>
                        <p className="mb-7 text-muted">This one-time setup is only available before the first user exists. The account will receive administrator access.</p>
                        <form onSubmit={handleSubmit} noValidate className="space-y-5">
                            <TextField
                                id="owner-username"
                                label="Owner username"
                                autoComplete="username"
                                value={username}
                                onChange={(event) => setUsername(event.target.value)}
                                disabled={isSubmitting}
                                hint="Use 3–50 letters, numbers, dots, underscores, or hyphens."
                                leadingIcon={<UserRound aria-hidden="true" className="h-5 w-5" />}
                            />
                            <TextField
                                id="owner-password"
                                label="Password"
                                type={showPassword ? 'text' : 'password'}
                                autoComplete="new-password"
                                value={password}
                                onChange={(event) => setPassword(event.target.value)}
                                disabled={isSubmitting}
                                hint="Use at least 8 characters."
                                leadingIcon={<LockKeyhole aria-hidden="true" className="h-5 w-5" />}
                                trailingAction={(
                                    <button type="button" onClick={() => setShowPassword((visible) => !visible)} aria-label={showPassword ? 'Hide password' : 'Show password'} className="grid h-11 w-11 place-items-center rounded-lg text-muted hover:text-ink">
                                        {showPassword ? <EyeOff aria-hidden="true" className="h-5 w-5" /> : <Eye aria-hidden="true" className="h-5 w-5" />}
                                    </button>
                                )}
                            />
                            <TextField
                                id="owner-confirm-password"
                                label="Confirm password"
                                type={showPassword ? 'text' : 'password'}
                                autoComplete="new-password"
                                value={confirmPassword}
                                onChange={(event) => setConfirmPassword(event.target.value)}
                                disabled={isSubmitting}
                                leadingIcon={<LockKeyhole aria-hidden="true" className="h-5 w-5" />}
                            />
                            {error && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
                            <Button type="submit" size="lg" loading={isSubmitting} className="w-full">
                                {isSubmitting ? 'Creating owner account' : 'Create owner account'}
                            </Button>
                        </form>
                    </>
                )}
            </div>
        </main>
    );
}
