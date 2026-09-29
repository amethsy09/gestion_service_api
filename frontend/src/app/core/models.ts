export type Role = 'ROLE_USER' | 'ROLE_ADMIN' | 'ROLE_RESPONSIBLE';
export interface ApiResponse<T> { success: boolean; message: string; data: T; timestamp?: string }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; last: boolean }
export interface AuthResponse { token: string; role: Role }
export interface ApiError { status: number; error: string; message: string; errors?: { field: string; message: string }[] }
export interface ServiceItem { id: string; name: string; description?: string; basePrice: number; active: boolean }
export interface ServiceRequest { id: string; serviceCatalogId: string; serviceName: string; title: string; description?: string; amount: number; status: string; paymentStatus: string; createdAt: string }
export interface PaymentResult { serviceRequestId: string; serviceRequestPaymentStatus: string; paymentAttemptId: string; attemptStatus: 'PENDING'|'PROCESSING'|'SUCCESS'|'FAILED'; idempotencyKey: string; walletTransactionReference?: string; amount: number; attemptNumber: number; failureReason?: string; processedAt?: string }
export interface Prestation { id: string; serviceRequestId: string; responsibleId: string; responsibleName: string; name: string; status: string; estimatedEndDate: string }
export interface Resource { id: string; firstName: string; lastName: string; email: string; phoneNumber?: string; specialtyId: string; availabilityStatus: string; active: boolean }
export interface Specialty { id: string; name: string; description?: string }
export interface Responsible { id: string; firstName: string; lastName: string; email: string; phoneNumber: string; active: boolean }
export interface Task { id: string; prestationId: string; resourceId?: string; resourceName?: string; title: string; description?: string; status: string; priority: string; startDate?: string; dueDate: string }
