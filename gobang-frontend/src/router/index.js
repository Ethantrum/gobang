import { createRouter, createWebHistory } from 'vue-router'
import UserLogin from '@/views/UserLogin.vue'
import UserRegister from '@/views/UserRegister.vue'
import GobangGame from "@/views/GobangGame.vue";
import GameHall from "@/views/GameHall.vue";

const routes = [
    {
        path: '/user/login',
        name: 'Login',
        component: UserLogin,
        meta: {
            title: '用户登录',
            requiresAuth: false
        }
    },
    {
        path: '/user/register',
        name: 'Register',
        component: UserRegister,
        meta: {
            title: '用户注册',
            requiresAuth: false
        }
    },
    {
        path: '/game/hall',
        name: 'GameHall',
        component: GameHall,
        meta:{
            title: '游戏大厅',
            requiresAuth: true
        }
    },
    {
        path: '/game/gobang/:roomId',
        name: 'Gobang',
        component: GobangGame,
        meta: {
            title: '在线对局',
            requiresAuth: true,
        }
    },
    {
        path: '/:pathMatch(.*)*', // Vue Router 4+ 通配符语法
        redirect: '/user/login',
        meta: {
            hidden: true
        }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes,
    scrollBehavior(to, from, savedPosition) {
        // 路由切换时的滚动行为
        if (savedPosition) {
            return savedPosition
        } else {
            return { top: 0 }
        }
    }
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
    // 设置页面标题
    if (to.meta.title) {
        document.title = to.meta.title
    }

    const isAuthenticated = localStorage.getItem('token')
    // 已登录用户不能访问登录/注册页
    if ((to.name === 'Login' || to.name === 'Register') && isAuthenticated) {
        next({ name: 'GameHall', replace: true })
    } else if (to.meta.requiresAuth && !isAuthenticated) {
        // 未登录用户不能访问需要登录的页面
        next({ name: 'Login', replace: true })
    } else {
        next()
    }
})

export default router