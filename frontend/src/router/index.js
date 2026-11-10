import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory('/tool/'),
  routes: [
    {
      path: '/',
      redirect: '/illustration',
    },
    {
      path: '/illustration',
      name: 'illustration',
      component: () => import('../pages/illustration/index.vue'),
    },
    {
      path: '/translate',
      name: 'translate',
      component: () => import('../pages/imageTranslate/index.vue'),
    },
  ],
})

export default router
