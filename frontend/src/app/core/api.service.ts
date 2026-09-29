import { Injectable } from '@angular/core'; import { HttpClient, HttpParams } from '@angular/common/http'; import { Observable, map } from 'rxjs'; import { ApiResponse, Page, PaymentResult, Prestation, Resource, Responsible, ServiceItem, ServiceRequest, Specialty, Task } from './models'; import { environment } from '../environment';
@Injectable({providedIn:'root'}) export class ApiService {
 private readonly base = environment.apiUrl; constructor(private http:HttpClient){}
 private data<T>(obs:Observable<ApiResponse<T>>):Observable<T>{return obs.pipe(map(x=>x.data));}
 services():Observable<ServiceItem[]>{return this.data(this.http.get<ApiResponse<ServiceItem[]>>(`${this.base}/services/active`));}
 catalog(page=0):Observable<Page<ServiceItem>>{return this.data(this.http.get<ApiResponse<Page<ServiceItem>>>(`${this.base}/services`,{params:{page,size:100}}));}
 saveService(body:Partial<ServiceItem>,id?:string):Observable<ServiceItem>{return this.data(id?this.http.put<ApiResponse<ServiceItem>>(`${this.base}/services/${id}`,body):this.http.post<ApiResponse<ServiceItem>>(`${this.base}/services`,body));}
 deleteService(id:string):Observable<void>{return this.data(this.http.delete<ApiResponse<void>>(`${this.base}/services/${id}`));}
 myRequests():Observable<Page<ServiceRequest>>{return this.data(this.http.get<ApiResponse<Page<ServiceRequest>>>(`${this.base}/service-requests/my`,{params:{page:0,size:100}}));}
 allRequests():Observable<Page<ServiceRequest>>{return this.data(this.http.get<ApiResponse<Page<ServiceRequest>>>(`${this.base}/admin/service-requests`,{params:{page:0,size:100}}));}
 createRequest(body:{serviceId:string;title:string;description:string}):Observable<ServiceRequest>{return this.data(this.http.post<ApiResponse<ServiceRequest>>(`${this.base}/service-requests`,body));}
 cancelRequest(id:string):Observable<void>{return this.data(this.http.delete<ApiResponse<void>>(`${this.base}/service-requests/${id}`));}
 pay(id:string,pin:string,retry=false):Observable<PaymentResult>{return this.data(this.http.post<ApiResponse<PaymentResult>>(`${this.base}/service-requests/${id}/${retry?'retry-payment':'pay'}`,{pin}));}
 syncPayment(id:string):Observable<PaymentResult>{return this.data(this.http.post<ApiResponse<PaymentResult>>(`${this.base}/service-requests/${id}/payment/sync`,{}));}
 resources():Observable<Page<Resource>>{return this.data(this.http.get<ApiResponse<Page<Resource>>>(`${this.base}/resources`,{params:{page:0,size:100}}));}
 saveResource(body:Partial<Resource>,id?:string):Observable<Resource>{return this.data(id?this.http.put<ApiResponse<Resource>>(`${this.base}/resources/${id}`,body):this.http.post<ApiResponse<Resource>>(`${this.base}/resources`,body));}
 deleteResource(id:string):Observable<void>{return this.data(this.http.delete<ApiResponse<void>>(`${this.base}/resources/${id}`));}
 specialties():Observable<Page<Specialty>>{return this.data(this.http.get<ApiResponse<Page<Specialty>>>(`${this.base}/specialties`,{params:{page:0,size:100}}));}
 saveSpecialty(body:Partial<Specialty>,id?:string):Observable<Specialty>{return this.data(id?this.http.put<ApiResponse<Specialty>>(`${this.base}/specialties/${id}`,body):this.http.post<ApiResponse<Specialty>>(`${this.base}/specialties`,body));}
 deleteSpecialty(id:string):Observable<void>{return this.data(this.http.delete<ApiResponse<void>>(`${this.base}/specialties/${id}`));}
 responsibles():Observable<Page<Responsible>>{return this.data(this.http.get<ApiResponse<Page<Responsible>>>(`${this.base}/responsibles`,{params:{page:0,size:100}}));}
 saveResponsible(body:Partial<Responsible>,id?:string):Observable<Responsible>{return this.data(id?this.http.put<ApiResponse<Responsible>>(`${this.base}/responsibles/${id}`,body):this.http.post<ApiResponse<Responsible>>(`${this.base}/responsibles`,body));}
 deleteResponsible(id:string):Observable<void>{return this.data(this.http.delete<ApiResponse<void>>(`${this.base}/responsibles/${id}`));}
 prestations():Observable<Page<Prestation>>{return this.data(this.http.get<ApiResponse<Page<Prestation>>>(`${this.base}/prestations`,{params:{page:0,size:100}}));}
 prestationAction(id:string,action:'plan'|'start'|'complete'|'cancel'):Observable<Prestation>{return this.data(this.http.put<ApiResponse<Prestation>>(`${this.base}/prestations/${id}/${action}`,{}));}
 tasks(prestationId:string):Observable<Page<Task>>{return this.data(this.http.get<ApiResponse<Page<Task>>>(`${this.base}/prestations/${prestationId}/tasks`,{params:{page:0,size:100}}));}
 createTask(prestationId:string,body:Partial<Task>):Observable<Task>{return this.data(this.http.post<ApiResponse<Task>>(`${this.base}/prestations/${prestationId}/tasks`,body));}
 taskStatus(id:string,status:string):Observable<Task>{return this.data(this.http.patch<ApiResponse<Task>>(`${this.base}/tasks/${id}/status`,{status}));}
 assignTask(taskId:string,resourceId:string):Observable<Task>{return this.data(this.http.post<ApiResponse<Task>>(`${this.base}/tasks/${taskId}/assign/${resourceId}`,{}));}
 assignPrestationResource(prestationId:string,resourceId:string):Observable<unknown>{return this.data(this.http.post<ApiResponse<unknown>>(`${this.base}/prestations/${prestationId}/resources/${resourceId}`,{}));}
 removePrestationResource(prestationId:string,resourceId:string):Observable<void>{return this.data(this.http.delete<ApiResponse<void>>(`${this.base}/prestations/${prestationId}/resources/${resourceId}`));}
 prestationResources(prestationId:string):Observable<unknown[]>{return this.data(this.http.get<ApiResponse<unknown[]>>(`${this.base}/prestations/${prestationId}/resources`));}
 assignResourceToTask(taskId:string,resourceId:string):Observable<Task>{return this.assignTask(taskId,resourceId);}
 availableResources():Observable<Resource[]>{return this.data(this.http.get<ApiResponse<Resource[]>>(`${this.base}/resources/available`));}
 createPrestation(body:Partial<Prestation>):Observable<Prestation>{return this.data(this.http.post<ApiResponse<Prestation>>(`${this.base}/prestations`,body));}
 firstChangePassword(body:{telephone:string;temporaryPassword:string;newPassword:string}):Observable<unknown>{return this.http.post(`${this.base}/auth/first-change-password`,body);}
 changePassword(body:{currentPassword:string;newPassword:string}):Observable<unknown>{return this.http.put(`${this.base}/auth/change-password`,body);}
}
