export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

export interface User {
  id: number
  username: string
  email: string
  roles: string[]
  permissions: string[]
  enabled: boolean
}

export interface LoginResult {
  tokenType: 'Bearer'
  accessToken: string
  expiresIn: number
  user: User
}

export interface RegisterInput {
  username: string
  email: string
  password: string
}

export interface LoginInput {
  account: string
  password: string
}
