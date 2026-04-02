<template>
    <el-container class="layout-container">
        <el-aside width="240px" class="sidebar">
            <div class="logo">
                <div class="logo-icon">
                    <el-icon size="20"><Monitor /></el-icon>
                </div>
                <span>语音打印</span>
            </div>
            <el-menu
                :default-active="currentRoute"
                class="sidebar-menu"
                background-color="#0f172a"
                text-color="rgba(255,255,255,0.65)"
                active-text-color="#fff"
                :router="true"
            >
                <el-menu-item index="/dashboard">
                    <el-icon><HomeFilled /></el-icon>
                    <span>首页</span>
                </el-menu-item>
                <el-menu-item index="/users">
                    <el-icon><UserFilled /></el-icon>
                    <span>用户管理</span>
                </el-menu-item>
            </el-menu>
            <div class="sidebar-footer">
                <div class="user-info">
                    <el-avatar :size="32" style="background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);">
                        {{ username.charAt(0) }}
                    </el-avatar>
                    <div class="user-detail">
                        <div class="user-name">{{ username }}</div>
                        <div class="user-role">管理员</div>
                    </div>
                </div>
                <el-button type="danger" plain size="small" @click="handleLogout" class="logout-btn">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                </el-button>
            </div>
        </el-aside>

        <el-container>
            <el-header class="header">
                <div class="header-left">
                    <h3>欢迎使用管理后台</h3>
                </div>
                <div class="header-right">
                    <el-tag type="success" effect="plain" round>
                        <el-icon><CircleCheck /></el-icon>
                        系统正常
                    </el-tag>
                </div>
            </el-header>
            <el-main class="main-content">
                <router-view />
            </el-main>
        </el-container>
    </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { HomeFilled, UserFilled, Monitor, SwitchButton, CircleCheck } from '@element-plus/icons-vue';
import { getCurrentUser, logout } from '../api/user';

const router = useRouter();
const route = useRoute();
const username = ref('管理员');

const currentRoute = computed(() => route.path);

onMounted(async () => {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login.html';
        return;
    }
    try {
        const data = await getCurrentUser();
        if (data.user) {
            username.value = data.user.nickname || data.user.username || '管理员';
        }
    } catch (err) {
        console.error(err);
    }
});

async function handleLogout() {
    try {
        await logout();
    } catch (err) {
        console.error(err);
    }
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login.html';
}
</script>

<style scoped>
.layout-container {
    min-height: 100vh;
}
.sidebar {
    background: #0f172a;
    display: flex;
    flex-direction: column;
}
.logo {
    height: 64px;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 20px;
    color: #fff;
    font-size: 16px;
    font-weight: 500;
    border-bottom: 1px solid rgba(255,255,255,0.06);
}
.logo-icon {
    width: 36px;
    height: 36px;
    background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
}
.sidebar-menu {
    border-right: none;
    flex: 1;
    padding-top: 12px;
}
.sidebar-menu .el-menu-item {
    margin: 4px 12px;
    border-radius: 8px;
    height: 48px;
}
.sidebar-menu .el-menu-item:hover {
    background: rgba(255,255,255,0.08) !important;
}
.sidebar-menu .el-menu-item.is-active {
    background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%) !important;
}
.sidebar-footer {
    padding: 16px;
    border-top: 1px solid rgba(255,255,255,0.06);
}
.user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
}
.user-detail {
    flex: 1;
    min-width: 0;
}
.user-name {
    color: #fff;
    font-size: 14px;
    font-weight: 500;
}
.user-role {
    color: rgba(255,255,255,0.45);
    font-size: 12px;
}
.logout-btn {
    width: 100%;
}
.header {
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    border-bottom: 1px solid #f0f0f0;
    height: 64px;
}
.header-left h3 {
    color: #333;
    font-size: 16px;
    font-weight: 500;
}
.main-content {
    background: #f1f5f9;
    padding: 24px;
}
</style>
