import axios from 'axios';

const request = axios.create({
    baseURL: '/api',
    timeout: 10000
});

request.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

request.interceptors.response.use(
    response => response.data,
    error => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login.html';
        }
        return Promise.reject(error);
    }
);

export function login(data) {
    return request.post('/auth/login', data);
}

export function getUsers() {
    return request.get('/users');
}

export function createUser(data) {
    return request.post('/users', data);
}

export function updateUser(id, data) {
    return request.put(`/users/${id}`, data);
}

export function deleteUser(id) {
    return request.delete(`/users/${id}`);
}

export function getImages(params) {
    return request.get('/images', { params });
}

export function getUserImages(userId, params) {
    return request.get(`/users/${userId}/images`, { params });
}

export function getCurrentUser() {
    return request.get('/auth/me');
}

export function logout() {
    return request.post('/auth/logout');
}
