import axios from 'axios';

interface ApiErrorBody {
    message?: string;
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
    if (!axios.isAxiosError<ApiErrorBody>(error)) return fallback;
    if (!error.response) return 'Could not connect to the server. Check that the backend is running.';
    return error.response.data?.message || fallback;
}
