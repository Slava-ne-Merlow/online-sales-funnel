import { useQuery } from '@tanstack/react-query'
import { usersApi } from '../../shared/api/services'

export function useUsersQuery(enabled = true) {
  return useQuery({
    queryKey: ['users', 'list'],
    queryFn: () => usersApi.getUsers(),
    enabled,
    staleTime: 60_000,
  })
}
