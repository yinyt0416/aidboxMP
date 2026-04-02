<template>
    <div class="login-page">
        <el-card class="login-card">
            <template #header>
                <div class="card-header">
                    <div class="logo-icon">
                        <el-icon size="32"><Monitor /></el-icon>
                    </div>
                    <h2>语音打印</h2>
                    <p>管理后台</p>
                </div>
            </template>
            <el-form @submit.prevent="handleLogin" :model="form" :rules="rules" ref="formRef">
                <el-form-item prop="username">
                    <el-input v-model="form.username" placeholder="请输入用户名" size="large" prefix-icon="User" />
                </el-form-item>
                <el-form-item prop="password">
                    <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" prefix-icon="Lock" show-password />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" size="large" :loading="loading" native-type="submit" class="login-btn">
                        登 录
                    </el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { Monitor } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { login } from '../api/user';

const router = useRouter();
const formRef = ref(null);
const loading = ref(false);

const form = reactive({
    username: '',
    password: ''
});

const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

async function handleLogin() {
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) return;

    loading.value = true;
    try {
        const res = await login({
            username: form.username,
            password: form.password
        });
        localStorage.setItem('token', res.token);
        localStorage.setItem('user', JSON.stringify(res.user));
        ElMessage.success('登录成功');
        router.push('/dashboard');
    } catch (err) {
        ElMessage.error(err.response?.data?.message || '登录失败');
    } finally {
        loading.value = false;
    }
}
</script>

<style scoped>
.login-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #0c4a6e 0%, #0369a1 50%, #0284c7 100%);
}
.login-card {
    width: 420px;
    border-radius: 16px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    background: rgba(255, 255, 255, 0.98);
}
.card-header {
    text-align: center;
    padding: 20px 0;
}
.logo-icon {
    width: 64px;
    height: 64px;
    background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    margin: 0 auto 16px;
}
.card-header h2 {
    color: #1e293b;
    margin-bottom: 8px;
    font-size: 24px;
}
.card-header p {
    color: #64748b;
    font-size: 14px;
}
.login-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);
    border: none;
    border-radius: 8px;
}
.login-btn:hover {
    background: linear-gradient(135deg, #38bdf8 0%, #0ea5e9 100%);
}
</style>
