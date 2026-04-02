<template>
    <div class="users-page">
        <el-card class="table-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <div class="header-left">
                        <el-icon><UserFilled /></el-icon>
                        <span>用户列表</span>
                    </div>
                    <el-button type="primary" @click="openAddDialog">
                        <el-icon><Plus /></el-icon>
                        添加用户
                    </el-button>
                </div>
            </template>

            <el-table :data="users" stripe style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="编号" width="80" />
                <el-table-column prop="username" label="用户名" min-width="120">
                    <template #default="{ row }">
                        <div class="user-cell">
                            <el-avatar :size="32" style="background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);">
                                {{ (row.nickname || row.username || 'U').charAt(0).toUpperCase() }}
                            </el-avatar>
                            <div class="user-info">
                                <span class="username">{{ row.nickname || row.username || '微信用户' }}</span>
                                <span class="openid" v-if="row.openid">{{ row.openid.substring(0, 8) }}...</span>
                            </div>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="nickname" label="昵称" min-width="120">
                    <template #default="{ row }">
                        {{ row.nickname || '-' }}
                    </template>
                </el-table-column>
                <el-table-column prop="imageCount" label="图片数" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag type="info" effect="plain">{{ row.imageCount || 0 }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="totalToken" label="Token使用" width="120" align="center">
                    <template #default="{ row }">
                        <el-tag type="warning" effect="plain">{{ formatToken(row.totalToken) }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="printCount" label="打印次数" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag type="success" effect="plain">{{ row.printCount || 0 }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="创建时间" min-width="160">
                    <template #default="{ row }">
                        <span style="color: #64748b;">
                            <el-icon><Clock /></el-icon>
                            {{ formatTime(row.createdAt) }}
                        </span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" link size="small" @click="viewUserImages(row)">
                            <el-icon><Picture /></el-icon>
                            查看图片
                        </el-button>
                        <el-button type="primary" link size="small" @click="openEditDialog(row)">
                            <el-icon><Edit /></el-icon>
                            编辑
                        </el-button>
                        <el-button type="danger" link size="small" @click="handleDelete(row.id)">
                            <el-icon><Delete /></el-icon>
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-empty v-if="!loading && users.length === 0" description="暂无用户数据" />
        </el-card>

        <!-- 添加/编辑用户弹窗 -->
        <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '添加用户'" width="480px" @close="resetForm" destroy-on-close>
            <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名">
                        <template #prefix><el-icon><User /></el-icon></template>
                    </el-input>
                </el-form-item>
                <el-form-item :label="isEdit ? '密码' : '密码'" :prop="isEdit ? '' : 'password'">
                    <el-input v-model="form.password" type="password" :placeholder="isEdit ? '留空则不修改' : '请输入密码'" show-password>
                        <template #prefix><el-icon><Lock /></el-icon></template>
                    </el-input>
                </el-form-item>
                <el-form-item label="昵称" prop="nickname">
                    <el-input v-model="form.nickname" placeholder="请输入昵称">
                        <template #prefix><el-icon><Postcard /></el-icon></template>
                    </el-input>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">
                    {{ submitting ? '提交中...' : '确定' }}
                </el-button>
            </template>
        </el-dialog>

        <!-- 用户图片记录弹窗 -->
        <el-dialog v-model="imageDialogVisible" :title="`${currentUser?.nickname || currentUser?.username || '用户'} 的图片记录`" width="900px" destroy-on-close>
            <div class="stats-summary">
                <el-row :gutter="20">
                    <el-col :span="8">
                        <div class="stat-item">
                            <span class="stat-label">图片总数</span>
                            <span class="stat-value">{{ imageTotal }}</span>
                        </div>
                    </el-col>
                    <el-col :span="8">
                        <div class="stat-item">
                            <span class="stat-label">Token使用</span>
                            <span class="stat-value">{{ formatToken(imageTotalToken) }}</span>
                        </div>
                    </el-col>
                </el-row>
            </div>

            <el-table :data="userImages" stripe style="width: 100%" v-loading="imageLoading" max-height="400">
                <el-table-column prop="prompt" label="提示词" min-width="200">
                    <template #default="{ row }">
                        <span class="prompt-text">{{ row.prompt || row.text || '-' }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="tokenUsage" label="Token" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag type="warning" size="small">{{ row.tokenUsage || 0 }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="time" label="生成时间" width="180">
                    <template #default="{ row }">
                        <span style="color: #64748b; font-size: 13px;">{{ row.time || formatTime(row.createdAt) }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="图片" width="100" align="center">
                    <template #default="{ row }">
                        <el-image
                            v-if="row.url"
                            :src="row.url"
                            :preview-src-list="[row.url]"
                            fit="cover"
                            style="width: 50px; height: 50px; border-radius: 4px;"
                            :preview-teleported="true"
                        />
                        <span v-else>-</span>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-wrap" v-if="imageTotal > imagePageSize">
                <el-pagination
                    v-model:current-page="imagePage"
                    :page-size="imagePageSize"
                    :total="imageTotal"
                    layout="prev, pager, next"
                    @current-change="loadUserImages"
                />
            </div>

            <el-empty v-if="!imageLoading && userImages.length === 0" description="暂无图片记录" />
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { Plus, Edit, Delete, Clock, User, Lock, Postcard, UserFilled, Picture } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getUsers, createUser, updateUser, deleteUser, getUserImages } from '../api/user';

const users = ref([]);
const loading = ref(false);

// 弹窗相关
const dialogVisible = ref(false);
const isEdit = ref(false);
const editId = ref(null);
const submitting = ref(false);
const formRef = ref(null);

const form = reactive({
    username: '',
    password: '',
    nickname: ''
});

const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

// 用户图片弹窗相关
const imageDialogVisible = ref(false);
const currentUser = ref(null);
const userImages = ref([]);
const imageLoading = ref(false);
const imagePage = ref(1);
const imagePageSize = ref(10);
const imageTotal = ref(0);
const imageTotalToken = ref(0);

function formatTime(timestamp) {
    if (!timestamp) return '-';
    return new Date(timestamp).toLocaleString('zh-CN');
}

function formatToken(token) {
    if (!token) return '0';
    return token >= 1000 ? (token / 1000).toFixed(1) + 'K' : token;
}

async function loadUsers() {
    loading.value = true;
    try {
        const data = await getUsers();
        users.value = data.list || [];
    } catch (err) {
        console.error(err);
    } finally {
        loading.value = false;
    }
}

function openAddDialog() {
    isEdit.value = false;
    editId.value = null;
    resetForm();
    dialogVisible.value = true;
}

function openEditDialog(row) {
    isEdit.value = true;
    editId.value = row.id;
    form.username = row.username;
    form.password = '';
    form.nickname = row.nickname || '';
    dialogVisible.value = true;
}

function resetForm() {
    form.username = '';
    form.password = '';
    form.nickname = '';
    formRef.value?.resetFields();
}

async function handleSubmit() {
    if (!isEdit.value && !form.password) {
        ElMessage.warning('请输入密码');
        return;
    }

    submitting.value = true;
    try {
        const data = {};
        if (isEdit.value) {
            if (form.password) data.password = form.password;
            if (form.nickname) data.nickname = form.nickname;
            await updateUser(editId.value, data);
        } else {
            data.username = form.username;
            if (form.password) data.password = form.password;
            if (form.nickname) data.nickname = form.nickname;
            await createUser(data);
        }
        dialogVisible.value = false;
        loadUsers();
        ElMessage.success('操作成功');
    } catch (err) {
        ElMessage.error(err.response?.data?.message || '操作失败');
    } finally {
        submitting.value = false;
    }
}

async function handleDelete(id) {
    try {
        await ElMessageBox.confirm('确定要删除该用户吗？此操作不可恢复。', '删除确认', {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning'
        });
        await deleteUser(id);
        loadUsers();
        ElMessage.success('删除成功');
    } catch (err) {
        if (err !== 'cancel') {
            ElMessage.error(err.response?.data?.message || '删除失败');
        }
    }
}

async function viewUserImages(row) {
    currentUser.value = row;
    imagePage.value = 1;
    imageDialogVisible.value = true;
    await loadUserImages();
}

async function loadUserImages() {
    if (!currentUser.value) return;

    imageLoading.value = true;
    try {
        const data = await getUserImages(currentUser.value.id, {
            page: imagePage.value,
            pageSize: imagePageSize.value
        });
        userImages.value = data.list || [];
        imageTotal.value = data.total || 0;
        // 计算总token
        imageTotalToken.value = userImages.value.reduce((sum, img) => sum + (img.tokenUsage || 0), 0);
    } catch (err) {
        console.error(err);
    } finally {
        imageLoading.value = false;
    }
}

onMounted(loadUsers);
</script>

<style scoped>
.users-page {
    max-width: 1400px;
}
.table-card {
    border-radius: 16px;
    border: none;
}
.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.header-left {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    font-size: 16px;
}
.user-cell {
    display: flex;
    align-items: center;
    gap: 10px;
}
.user-info {
    display: flex;
    flex-direction: column;
}
.username {
    font-weight: 500;
}
.openid {
    font-size: 11px;
    color: #10b981;
}
.stats-summary {
    background: #f8fafc;
    padding: 16px;
    border-radius: 8px;
    margin-bottom: 16px;
}
.stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
}
.stat-label {
    font-size: 13px;
    color: #64748b;
}
.stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #0ea5e9;
}
.prompt-text {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 13px;
    color: #475569;
}
.pagination-wrap {
    display: flex;
    justify-content: center;
    margin-top: 16px;
}
</style>
