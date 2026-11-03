# Breaking Change Notice: Authentication Error Response Shape

**Date:** 2026-11-03  
**Author:** Amanda (Backend)  
**Recipient:** Avuyile (Frontend)  
**Target Endpoint:** `POST /api/v1/auth/login`  

---

### What Changed

Previously, when authentication failed due to invalid credentials (e.g., bad username or password), `POST /api/v1/auth/login` returned an HTTP 401 response with a **plain text string body**:
```text
Incorrect username or password
```

As part of the error-contract standardization (AMA-01), **every** error response across the entire API now conforms to the unified JSON `ErrorResponse` contract. `POST /api/v1/auth/login` now returns HTTP 401 with **`Content-Type: application/json`** and a JSON object body:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Incorrect username or password",
  "path": "/api/v1/auth/login",
  "timestamp": "2026-11-03T21:49:27"
}
```

---

### Affected Frontend Files

1. **`frontend/src/pages/LoginPage.tsx` (Lines 55–63)**
   - In `handleSubmit`, errors are caught and evaluated:
     ```ts
     if (axios.isAxiosError(err) && err.response?.status === 401) {
         setError({ message: 'Incorrect username or password. Please try again.' });
     }
     ```
   - If `LoginPage.tsx` or any error toaster reads `err.response?.data` directly (or plans to display the backend's message via `err.response?.data.message`), please note that `err.response.data` is now a parsed JSON object `{ status, error, message, path, timestamp }`, NOT a string.
2. **`frontend/src/services/api.ts` (Lines 17–28)**
   - Response interceptor inspects `isLoginRequest = error.config?.url === '/auth/login'` to avoid auto-redirecting to `/login`. The status remains `401`, so this interceptor continues to function as expected.
3. **`frontend/src/context/AuthContext.tsx` (Lines 16–21)**
   - `login(token, user)` is only invoked on HTTP 200, so it is unaffected, but any future auth error state should expect `ErrorResponse`.

---

### Recommended Action for Frontend

If you wish to display the exact server message or structured error:
```ts
if (axios.isAxiosError(err) && err.response?.status === 401) {
    const serverMessage = err.response.data?.message || 'Incorrect username or password. Please try again.';
    setError({ message: serverMessage });
}
```
Or continue checking `err.response?.status === 401` while being aware that `err.response.data` is now an object, avoiding any code that treats `err.response.data` as a primitive string (e.g. `typeof data === 'string'`).
