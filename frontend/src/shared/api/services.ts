import { request } from './http'
import type {
  AnalyticsPeriod,
  AuthResponse,
  CreateProjectRequest,
  GetProjectAnalyticsParams,
  GetProjectsParams,
  ProjectAdvanceRequest,
  ProjectAnalytics,
  Project,
  ProjectHistory,
  ProjectTransitionRequest,
  SignInRequest,
  UpdateProjectRequest,
  User,
} from './types'

export const authApi = {
  signIn(payload: SignInRequest) {
    return request<AuthResponse>('/api/auth/sign-in', {
      method: 'POST',
      body: payload,
      auth: false,
    })
  },
}

export const usersApi = {
  getMe() {
    return request<User>('/api/users/me')
  },
  getUsers() {
    return request<User[]>('/api/users')
  },
}

export const projectsApi = {
  getProjects(params?: GetProjectsParams) {
    return request<Project[]>('/api/projects', {
      query: params,
    })
  },
  createProject(payload: CreateProjectRequest) {
    return request<Project>('/api/projects', {
      method: 'POST',
      body: payload,
    })
  },
  getProject(projectId: string) {
    return request<Project>(`/api/projects/${projectId}`)
  },
  updateProject(projectId: string, payload: UpdateProjectRequest) {
    return request<Project>(`/api/projects/${projectId}`, {
      method: 'PATCH',
      body: payload,
    })
  },
  transitionProject(projectId: string, payload: ProjectTransitionRequest) {
    return request<Project>(`/api/projects/${projectId}/transition`, {
      method: 'POST',
      body: payload,
    })
  },
  advanceProject(projectId: string, payload?: ProjectAdvanceRequest) {
    return request<Project>(`/api/projects/${projectId}/advance`, {
      method: 'POST',
      body: payload ?? {},
    })
  },
  getProjectHistory(projectId: string) {
    return request<ProjectHistory[]>(`/api/projects/${projectId}/history`)
  },
  getProjectAnalytics(params: GetProjectAnalyticsParams & { period: AnalyticsPeriod }) {
    return request<ProjectAnalytics>('/api/projects/analytics', {
      query: params,
    })
  },
}
