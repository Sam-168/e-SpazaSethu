import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { User, AuthContextType } from '../types';
import api from '../services/api';

const AuthContext = createContext<AuthContextType | null>(null);

interface StoredSession {
    token: string | null;
    currentUser: User | null;
}

function clearStoredSession() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
}

function isExpired(token: string): boolean {
    try {
        const payload = JSON.parse(atob(token.split('.')[1])) as { exp?: number };
        return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now();
    } catch {
        return true;
    }
}

function loadStoredSession(): StoredSession {
    const token = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');

    if (!token || !storedUser || isExpired(token)) {
        clearStoredSession();
        return { token: null, currentUser: null };
    }

    try {
        const currentUser = JSON.parse(storedUser) as User;
        if (!currentUser.userId || !currentUser.username || !currentUser.role) throw new Error('Invalid user');
        return { token, currentUser };
    } catch {
        clearStoredSession();
        return { token: null, currentUser: null };
    }
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
    const [session, setSession] = useState<StoredSession>(loadStoredSession);

    useEffect(() => {
        if (!session.token) return;
        api.get<{shopName:string;currencyCode:string}>('/settings/store').then(({data}) => {
            localStorage.setItem('storeName', data.shopName);
            localStorage.setItem('storeCurrency', data.currencyCode);
        }).catch(() => { /* Defaults keep the app usable if settings are temporarily unavailable. */ });
    }, [session.token]);

    const login = (newToken: string, user: User) => {
        localStorage.setItem('token', newToken);
        localStorage.setItem('user', JSON.stringify(user));
        setSession({ token: newToken, currentUser: user });
    };

    const logout = () => {
        clearStoredSession();
        setSession({ token: null, currentUser: null });
        window.location.href = '/login';
    };

    return (
        <AuthContext.Provider value={{ ...session, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within AuthProvider');
    return context;
};
