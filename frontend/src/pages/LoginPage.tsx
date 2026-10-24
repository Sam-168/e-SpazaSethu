import { useEffect, useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, LockKeyhole, Store, UserRound } from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import type { LoginResponse } from '../types';
import { Button } from '../components/ui/Button';
import { TextField } from '../components/ui/TextField';
import { getApiErrorMessage } from '../utils/api-error';

interface LocationState {
    from?: { pathname: string };
    message?: string;
}

export default function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const { token, currentUser, login } = useAuth();
    const state = location.state as LocationState | null;
    const from = state?.from?.pathname ?? '/';

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [setupRequired, setSetupRequired] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        api.get<{ setupRequired: boolean }>('/auth/setup-status')
            .then(({ data }) => setSetupRequired(data.setupRequired))
            .catch(() => setSetupRequired(false));
    }, []);

    if (token && currentUser) return <Navigate to={from} replace />;

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (isSubmitting) return;

        if (!username.trim() || !password) {
            setError('Enter your username and password.');
            return;
        }

        setError(null);
        setIsSubmitting(true);

        try {
            const { data } = await api.post<LoginResponse>('/auth/login', {
                username: username.trim(),
                password,
            });
            login(data.token, data.user);
            navigate(from, { replace: true });
        } catch (requestError) {
            setError(getApiErrorMessage(requestError, 'Incorrect username or password.'));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <main className="grid min-h-screen bg-canvas lg:grid-cols-[minmax(0,1fr)_minmax(30rem,0.82fr)]">
            <section className="hidden border-r border-brand-800 bg-brand-700 px-12 py-10 text-white lg:flex lg:flex-col lg:justify-between">
                <div className="flex items-center gap-3 text-lg font-semibold">
                    <span className="grid h-11 w-11 place-items-center rounded-xl bg-white text-brand-700">
                        <Store aria-hidden="true" className="h-6 w-6" />
                    </span>
                    e-SpazaSethu
                </div>
                <div className="max-w-xl pb-14">
                    <p className="mb-4 text-sm font-semibold uppercase tracking-[0.2em] text-brand-100">Your shop, clearly managed</p>
                    <h1 className="text-5xl font-semibold leading-[1.08] tracking-tight">Sales and stock without the paperwork.</h1>
                    <p className="mt-6 max-w-lg text-lg leading-8 text-brand-50">A straightforward workspace built for the pace of a neighbourhood spaza shop.</p>
                </div>
                <p className="text-sm text-brand-100">Secure access for owners and cashiers</p>
            </section>

            <section className="flex items-center justify-center px-5 py-10 sm:px-10">
                <div className="w-full max-w-md">
                    <div className="mb-10 flex items-center gap-3 lg:hidden">
                        <span className="grid h-11 w-11 place-items-center rounded-xl bg-brand-700 text-white">
                            <Store aria-hidden="true" className="h-6 w-6" />
                        </span>
                        <span className="text-lg font-semibold text-ink">e-SpazaSethu</span>
                    </div>

                    <div className="mb-8">
                        <p className="text-sm font-semibold text-brand-700">Welcome back</p>
                        <h2 className="mt-2 text-3xl font-semibold tracking-tight text-ink">Sign in to your store</h2>
                        <p className="mt-2 text-base text-muted">Use the account provided by your store administrator.</p>
                    </div>

                    {state?.message && (
                        <div role="status" className="mb-5 rounded-lg border border-brand-200 bg-brand-50 px-4 py-3 text-sm text-brand-800">
                            {state.message}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} noValidate className="space-y-5">
                        <TextField
                            id="username"
                            label="Username"
                            type="text"
                            autoComplete="username"
                            value={username}
                            onChange={(event) => setUsername(event.target.value)}
                            disabled={isSubmitting}
                            leadingIcon={<UserRound aria-hidden="true" className="h-5 w-5" />}
                        />
                        <TextField
                            id="password"
                            label="Password"
                            type={showPassword ? 'text' : 'password'}
                            autoComplete="current-password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            disabled={isSubmitting}
                            leadingIcon={<LockKeyhole aria-hidden="true" className="h-5 w-5" />}
                            trailingAction={(
                                <button
                                    type="button"
                                    onClick={() => setShowPassword((visible) => !visible)}
                                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                                    className="grid h-11 w-11 place-items-center rounded-lg text-muted hover:text-ink"
                                >
                                    {showPassword ? <EyeOff aria-hidden="true" className="h-5 w-5" /> : <Eye aria-hidden="true" className="h-5 w-5" />}
                                </button>
                            )}
                        />

                        {error && <p role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}

                        <Button type="submit" size="lg" loading={isSubmitting} className="w-full">
                            {isSubmitting ? 'Signing in' : 'Sign in'}
                        </Button>
                    </form>

                    {setupRequired && (
                        <p className="mt-8 border-t border-line pt-6 text-center text-sm text-muted">
                            Setting up a new store?{' '}
                            <Link to="/setup-owner" className="font-semibold text-brand-700 hover:text-brand-800">Create the owner account</Link>
                        </p>
                    )}
                </div>
            </section>
        </main>
    );
}
