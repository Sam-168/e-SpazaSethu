// --- Auth ---
export type Role = 'ADMIN' | 'CASHIER';

export interface User {
    userId: string;
    username: string;
    role: Role;
}

export interface AuthContextType {
    currentUser: User | null;
    token: string | null;
    login: (token: string, user: User) => void;
    logout: () => void;
}

export interface LoginResponse {
    token: string;
    user: User;
}

// --- Products ---
export interface Product {
    productId: string;
    name: string;
    barcode?: string;
    sellingPrice: number;
    costPrice?: number;
    stockQuantity: number;
    lowStockThreshold?: number;
    categoryId?: string;
    categoryName?: string;
    description?: string;
    isActive: boolean;
    lowStock?: boolean;
    active?: boolean;
}

export interface LowStockProduct {
    productId: string;
    name: string;
    stockQuantity: number;
    lowStockThreshold: number;
}

export interface Category {
    categoryId: string;
    name: string;
    description: string;
}

// --- Cart (POS) ---
export interface CartItem {
    productId: string;
    name: string;
    unitPrice: number;
    quantity: number;
    stockQuantity: number;
}

// --- Dashboard ---
export interface DashboardData {
    totalSales: number;
    totalProfit: number;
    transactionCount: number;
}

export interface SaleSummary {
    saleId: string;
    saleDateTime: string;
    totalAmount: number;
    paymentMethod: string;
    status: string;
}

// --- Sales ---
export type PaymentMethod = 'CASH' | 'CARD' | 'MOBILE_PAYMENT';
export type SaleStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';

export interface SaleItem {
    productId: string;
    productName: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
}

export interface Sale {
    saleId: string;
    saleDateTime: string;
    cashierId: string;
    cashierName: string;
    items: SaleItem[];
    totalAmount: number;
    paymentMethod: PaymentMethod;
    status: SaleStatus;
}

export interface SalesReportSummary {
    totalAmount: number;
    totalProfit: number;
}

export interface TopProductReport {
    productId: string;
    productName: string;
    quantitySold: number;
    revenue: number;
}

// --- Generic API shapes ---
export interface PaginatedResponse<T> {
    data: T[];
    page: number;
    totalPages: number;
    totalItems: number;
}
