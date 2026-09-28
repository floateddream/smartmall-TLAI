import { api } from '@/api/client'
import type { ApiResponse } from '@/types/api'

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface Product {
  id: number
  sku: string
  name: string
  description: string | null
  price: number
  stock: number
  active: boolean
  createdAt: string
  updatedAt: string
}

export type ProductInput = Pick<Product, 'sku' | 'name' | 'description' | 'price' | 'stock' | 'active'>

export interface Customer {
  id: number
  name: string
  phone: string | null
  email: string | null
  createdAt: string
  updatedAt: string
}

export type CustomerInput = Pick<Customer, 'name' | 'phone' | 'email'>

export const productApi = {
  list: async (page: number, keyword: string, size = 20) =>
    (await api.get<ApiResponse<PageResult<Product>>>('/products', { params: { page, size, keyword } })).data.data,
  create: async (input: ProductInput) =>
    (await api.post<ApiResponse<Product>>('/products', input)).data.data,
  update: async (id: number, input: ProductInput) =>
    (await api.put<ApiResponse<Product>>(`/products/${id}`, input)).data.data,
}

export const customerApi = {
  list: async (page: number, keyword: string, size = 20) =>
    (await api.get<ApiResponse<PageResult<Customer>>>('/customers', { params: { page, size, keyword } })).data.data,
  create: async (input: CustomerInput) =>
    (await api.post<ApiResponse<Customer>>('/customers', input)).data.data,
  update: async (id: number, input: CustomerInput) =>
    (await api.put<ApiResponse<Customer>>(`/customers/${id}`, input)).data.data,
}
