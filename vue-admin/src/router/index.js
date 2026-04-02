import { createRouter, createWebHistory } from 'vue-router';
import Layout from '../components/Layout.vue';
import Dashboard from '../views/Dashboard.vue';
import Users from '../views/Users.vue';
import Login from '../views/Login.vue';

const routes = [
    {
        path: '/login',
        component: Login
    },
    {
        path: '/',
        component: Layout,
        children: [
            { path: '', redirect: '/dashboard' },
            { path: 'dashboard', component: Dashboard },
            { path: 'users', component: Users }
        ]
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token');
    if (to.path !== '/login' && !token) {
        next('/login');
    } else {
        next();
    }
});

export default router;
