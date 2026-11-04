import {
    useEffect,
    useState,
    type FormEvent
} from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import './SettingsPage.css';

const SHOP_NAME_KEY = 'shopName';

export default function SettingsPage() {
    const {currentUser, logout} = useAuth();

    const [shopName, setShopName] = useState('');
    const [showPasswordModal, setShowPasswordModal] = useState(false);
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [passwordError, setPasswordError] = useState('');
    const [passwordSuccess, setPasswordSuccess] = useState('');
    const [isChangingPassword, setIsChangingPassword] = useState(false);

    useEffect(() => {

    }, []);

    const handleShopNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const value = event.target.value;
        setShopName(value);
        localStorage.setItem(SHOP_NAME_KEY, value);
    };

    const openPasswordModal = () => {
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
        setPasswordError('');
        setPasswordSuccess('');
        setShowPasswordModal(true);
    };

    const closePasswordModal = () => {
        if (!isChangingPassword) {
            setShowPasswordModal(false);
        }
    };

    const handleLogout = () => {
        if (window.confirm('Are you sure you want to log out?')) {
            logout();
        }
    };

    const handleChangePassword = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        setPasswordError('');
        setPasswordSuccess('');

        if (!currentPassword || !newPassword || !confirmPassword) {
            setPasswordError('Please complete all password fields.');
            return;
        }

        if (newPassword.length < 8) {
            setPasswordError('New password must contain at least 8 characters.');
            return;
        }

        if (newPassword !== confirmPassword) {
            setPasswordError('New passwords do not match.');
            return;
        }

        if (currentPassword === newPassword) {
            setPasswordError('New password must be different from your current password.');
            return;
        }

        if (!currentUser?.userId) {
            setPasswordError('Unable to identify the current user.');
            return;
        }

        setIsChangingPassword(true);

        try {
            await api.patch(`/users/${currentUser.userId}/password`, {
                currentPassword,
                newPassword,
            });

            setPasswordSuccess('Password changed successfully.');
            setCurrentPassword('');
            setNewPassword('');
            setConfirmPassword('');
        } catch (error: unknown) {
            if (axios.isAxiosError(error)) {
                setPasswordError(error.response?.data?.message ?? error.message);
            } else if (error instanceof Error) {
                setPasswordError(error.message);
            } else {
                setPasswordError('An unexpected error occurred.');
            }
        } finally {
            setIsChangingPassword(false);
        }
    };

    const isAdmin = currentUser?.role === 'ADMIN';

    return (
        <div className="settings-page">
            <div className="settings-shell">
                <header className="settings-header">
                    <div>
                        <p className="eyebrow">Account</p>
                        <h1>Settings</h1>
                    </div>
                </header>

                <section className="settings-section">
                    <h2>Profile</h2>
                    <div className="settings-card">
                        <div className="setting-row">
                            <div>
                                <h3>Username</h3>
                                <p>{currentUser?.username || 'Not available'}</p>
                            </div>
                        </div>

                        <div className="setting-row">
                            <div>
                                <h3>Role</h3>
                                <p>{currentUser?.role || 'Not available'}</p>
                            </div>
                        </div>

                        <div className="setting-row action-row">
                            <div>
                                <h3>Password</h3>
                                <p>Update your account password.</p>
                            </div>
                            <button type="button" className="secondary-button" onClick={openPasswordModal}>
                                Change Password
                            </button>
                        </div>
                    </div>
                </section>

                <section className="settings-section">
                    <h2>Shop</h2>
                    <div className="settings-card">
                        <div className="form-group">
                            <label htmlFor="shopName">Shop Name</label>
                            <input
                                id="shopName"
                                type="text"
                                value={shopName}
                                onChange={handleShopNameChange}
                                placeholder="Enter shop name"
                            />
                            <small>Your shop name is saved locally on this device.</small>
                        </div>
                    </div>
                </section>

                {isAdmin && (
                    <section className="settings-section">
                        <h2>Administration</h2>
                        <div className="settings-card">
                            <div className="setting-row action-row">
                                <div>
                                    <h3>Manage Users</h3>
                                    <p>Add, edit and manage application users.</p>
                                </div>
                                <Link to="/settings/users" className="secondary-button">
                                    Manage Users
                                </Link>
                            </div>
                        </div>
                    </section>
                )}

                <section className="settings-section">
                    <h2>About</h2>
                    <div className="settings-card">
                        <div className="setting-row">
                            <div>
                                <h3>Application</h3>
                                <p>Stock Management System</p>
                            </div>
                        </div>
                        <div className="setting-row">
                            <div>
                                <h3>Version</h3>
                                <p>1.0.0</p>
                            </div>
                        </div>
                    </div>
                </section>

                <section className="settings-section logout-section">
                    <h2>Logout</h2>
                    <div className="settings-card">
                        <div className="setting-row action-row">
                            <div>
                                <h3>Sign out of your account</h3>
                                <p>You will need to sign in again to access the application.</p>
                            </div>
                            <button type="button" className="danger-button" onClick={handleLogout}>
                                Logout
                            </button>
                        </div>
                    </div>
                </section>
            </div>

            {showPasswordModal && (
                <div className="modal-backdrop" onClick={closePasswordModal}>
                    <div className="modal-card" onClick={(event) => event.stopPropagation()}>
                        <div className="modal-header">
                            <h2>Change Password</h2>
                            <button
                                type="button"
                                className="close-button"
                                onClick={closePasswordModal}
                                disabled={isChangingPassword}
                                aria-label="Close"
                            >
                                ×
                            </button>
                        </div>

                        <form onSubmit={handleChangePassword}>
                            <div className="form-group">
                                <label htmlFor="currentPassword">Current Password</label>
                                <input
                                    id="currentPassword"
                                    type="password"
                                    value={currentPassword}
                                    onChange={(event) => setCurrentPassword(event.target.value)}
                                    placeholder="Enter current password"
                                    disabled={isChangingPassword}
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="newPassword">New Password</label>
                                <input
                                    id="newPassword"
                                    type="password"
                                    value={newPassword}
                                    onChange={(event) => setNewPassword(event.target.value)}
                                    placeholder="Enter new password"
                                    disabled={isChangingPassword}
                                />
                                <small>Password must contain at least 8 characters.</small>
                            </div>

                            <div className="form-group">
                                <label htmlFor="confirmPassword">Confirm New Password</label>
                                <input
                                    id="confirmPassword"
                                    type="password"
                                    value={confirmPassword}
                                    onChange={(event) => setConfirmPassword(event.target.value)}
                                    placeholder="Confirm new password"
                                    disabled={isChangingPassword}
                                />
                            </div>

                            {passwordError && <div className="error-message">{passwordError}</div>}
                            {passwordSuccess && <div className="success-message">{passwordSuccess}</div>}

                            <div className="modal-actions">
                                <button type="button" className="secondary-button" onClick={closePasswordModal}
                                        disabled={isChangingPassword}>
                                    Cancel
                                </button>
                                <button type="submit" className="primary-button" disabled={isChangingPassword}>
                                    {isChangingPassword ? 'Changing...' : 'Change Password'}
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
