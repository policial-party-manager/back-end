import request from '@/utils/request'

export function getBranchList() {
  return request.get('/branch/list')
}

export function addBranch(data: any) {
  return request.post('/branch', data)
}

export function updateBranch(id: number, data: any) {
  return request.put(`/branch/${id}`, data)
}

export function deleteBranch(id: number) {
  return request.delete(`/branch/${id}`)
}
