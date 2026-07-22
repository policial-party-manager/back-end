import request from '@/utils/request'

export function getMemberList(params: any) {
  return request.get('/member/list', { params })
}

export function getMemberById(id: number) {
  return request.get(`/member/${id}`)
}

export function addMember(data: any) {
  return request.post('/member', data)
}

export function updateMember(id: number, data: any) {
  return request.put(`/member/${id}`, data)
}

export function deleteMember(id: number) {
  return request.delete(`/member/${id}`)
}
