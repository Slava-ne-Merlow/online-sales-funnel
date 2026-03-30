import { useMemo } from 'react'
import { useQueries, useQuery } from '@tanstack/react-query'
import { projectsApi } from '../../shared/api/services'
import type {
  AnalyticsPeriod,
  GetProjectAnalyticsParams,
  GetProjectsParams,
  ProjectHistory,
} from '../../shared/api/types'

export function useProjectsQuery(filters?: GetProjectsParams) {
  return useQuery({
    queryKey: ['projects', 'list', filters ?? {}],
    queryFn: () => projectsApi.getProjects(filters),
  })
}

export function useProjectsLookupQuery() {
  return useQuery({
    queryKey: ['projects', 'lookup'],
    queryFn: () => projectsApi.getProjects(),
  })
}

export function useProjectQuery(projectId?: string) {
  return useQuery({
    queryKey: ['projects', 'detail', projectId],
    queryFn: () => projectsApi.getProject(projectId!),
    enabled: Boolean(projectId),
  })
}

export function useProjectHistoryQuery(projectId?: string) {
  return useQuery({
    queryKey: ['projects', 'history', projectId],
    queryFn: () => projectsApi.getProjectHistory(projectId!),
    enabled: Boolean(projectId),
  })
}

export function useProjectsHistoryQuery(projectIds: string[]) {
  const uniqueProjectIds = useMemo(() => Array.from(new Set(projectIds)).filter(Boolean), [projectIds])

  const queries = useQueries({
    queries: uniqueProjectIds.map((projectId) => ({
      queryKey: ['projects', 'history', projectId],
      queryFn: () => projectsApi.getProjectHistory(projectId),
      enabled: uniqueProjectIds.length > 0,
      staleTime: 60_000,
    })),
  })

  const data = useMemo(() => {
    return queries.flatMap((query) => (query.data ?? []) as ProjectHistory[])
  }, [queries])

  return {
    data,
    isPending: queries.some((query) => query.isPending),
    isError: queries.some((query) => query.isError),
    error: queries.find((query) => query.error)?.error ?? null,
  }
}

export function useProjectAnalyticsQuery(
  params: (GetProjectAnalyticsParams & { period: AnalyticsPeriod }) | null,
) {
  return useQuery({
    queryKey: ['projects', 'analytics', params],
    queryFn: () => projectsApi.getProjectAnalytics(params!),
    enabled: Boolean(params?.period),
  })
}
