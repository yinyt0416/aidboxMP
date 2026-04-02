<template>
    <div class="dashboard">
        <el-row :gutter="24" class="stats-row">
            <el-col :span="6">
                <div class="stat-card stat-card-1">
                    <div class="stat-bg-icon">
                        <el-icon size="48"><UserFilled /></el-icon>
                    </div>
                    <div class="stat-content">
                        <div class="stat-label">总用户数</div>
                        <div class="stat-value">{{ totalUsers }}</div>
                        <div class="stat-desc">小程序授权用户</div>
                    </div>
                </div>
            </el-col>
            <el-col :span="6">
                <div class="stat-card stat-card-2">
                    <div class="stat-bg-icon">
                        <el-icon size="48"><Picture /></el-icon>
                    </div>
                    <div class="stat-content">
                        <div class="stat-label">总生成图片</div>
                        <div class="stat-value">{{ totalImages }}</div>
                        <div class="stat-desc">AI绘图记录</div>
                    </div>
                </div>
            </el-col>
            <el-col :span="6">
                <div class="stat-card stat-card-3">
                    <div class="stat-bg-icon">
                        <el-icon size="48"><Coin /></el-icon>
                    </div>
                    <div class="stat-content">
                        <div class="stat-label">总消耗 Token</div>
                        <div class="stat-value">{{ totalToken }}</div>
                        <div class="stat-desc">大模型消耗</div>
                    </div>
                </div>
            </el-col>
            <el-col :span="6">
                <div class="stat-card stat-card-4">
                    <div class="stat-bg-icon">
                        <el-icon size="48"><Avatar /></el-icon>
                    </div>
                    <div class="stat-content">
                        <div class="stat-label">当前管理员</div>
                        <div class="stat-value" style="font-size: 22px;">{{ username }}</div>
                        <div class="stat-desc">后台在线管理</div>
                    </div>
                </div>
            </el-col>
        </el-row>

        <el-row :gutter="24">
            <el-col :span="16">
                <el-card class="card" shadow="never">
                    <template #header>
                        <div class="card-header">
                            <span>系统信息</span>
                        </div>
                    </template>
                    <el-descriptions :column="2" border>
                        <el-descriptions-item label="系统名称">语音打印管理后台</el-descriptions-item>
                        <el-descriptions-item label="技术栈">Spring Boot + Vue 3</el-descriptions-item>
                        <el-descriptions-item label="前端框架">Vue 3 + Element Plus</el-descriptions-item>
                        <el-descriptions-item label="数据库">MySQL 8.0</el-descriptions-item>
                    </el-descriptions>
                </el-card>
            </el-col>
            <el-col :span="8">
                <el-card class="card" shadow="never">
                    <template #header>
                        <div class="card-header">
                            <span>快捷操作</span>
                        </div>
                    </template>
                    <div class="quick-actions">
                        <el-button type="primary" plain @click="$router.push('/users')">
                            <el-icon><User /></el-icon>
                            用户管理
                        </el-button>
                        <el-button type="default" plain @click="refresh">
                            <el-icon><Refresh /></el-icon>
                            刷新数据
                        </el-button>
                    </div>
                </el-card>
            </el-col>
        </el-row>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Picture, User, Refresh, UserFilled, Coin, Avatar } from '@element-plus/icons-vue';
import { getImages, getCurrentUser, getUsers } from '../api/user';

const username = ref('管理员');
const totalUsers = ref('-');
const totalImages = ref('-');
const totalToken = ref('-');

async function loadData() {
    try {
        const data = await getCurrentUser();
        if (data.user) {
            username.value = data.user.nickname || data.user.username || '管理员';
        }
    } catch (err) {
        console.error(err);
    }

    try {
        const imgData = await getImages({ page: 1, pageSize: 1 });
        totalImages.value = imgData.total || 0;
    } catch (err) {
        totalImages.value = '0';
    }

    try {
        const usersData = await getUsers();
        if (usersData.list) {
            totalUsers.value = usersData.list.length;
            let tokens = 0;
            usersData.list.forEach(u => {
                tokens += (u.totalToken || 0);
            });
            totalToken.value = tokens;
        }
    } catch (err) {
        totalUsers.value = '0';
        totalToken.value = '0';
    }
}

function refresh() {
    loadData();
}

onMounted(() => {
    loadData();
});
</script>

<style scoped>
.stats-row {
    margin-bottom: 24px;
}
.stat-card {
    position: relative;
    padding: 28px;
    border-radius: 16px;
    color: #fff;
    overflow: hidden;
    min-height: 160px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}
.stat-card-1 {
    background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
}
.stat-card-2 {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}
.stat-card-3 {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}
.stat-card-4 {
    background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
}
.stat-bg-icon {
    position: absolute;
    right: -10px;
    bottom: -10px;
    opacity: 0.3;
}
.stat-content {
    position: relative;
    z-index: 1;
}
.stat-label {
    font-size: 14px;
    opacity: 0.9;
    margin-bottom: 8px;
}
.stat-value {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 4px;
}
.stat-desc {
    font-size: 12px;
    opacity: 0.7;
}
.card {
    border-radius: 12px;
    border: none;
}
.card-header {
    font-weight: 600;
    font-size: 16px;
}
.quick-actions {
    display: flex;
    flex-direction: column;
    gap: 12px;
}
.quick-actions .el-button {
    justify-content: flex-start;
}
</style>
