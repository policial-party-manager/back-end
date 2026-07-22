import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
        { path: 'members', name: 'MemberList', component: () => import('@/views/MemberList.vue') },
        { path: 'members/:id', name: 'MemberDetail', component: () => import('@/views/MemberDetail.vue') },
        { path: 'branches', name: 'BranchList', component: () => import('@/views/BranchList.vue') },
        { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue') }
      ]
    }
  ]
})

router.beforeEach((to, _from) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    return '/login'
  }
})

export default router
