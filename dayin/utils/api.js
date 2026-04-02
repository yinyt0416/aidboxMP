import config from './config.js';

function request({ url, method = 'GET', data = {}, header = {} }) {
	return new Promise((resolve, reject) => {
		const token = uni.getStorageSync('token');
		uni.request({
			url: `${config.backend.baseUrl}${url}`,
			method,
			data,
			header: {
				'Content-Type': 'application/json',
				...(token ? { Authorization: `Bearer ${token}` } : {}),
				...header
			},
			success: (res) => {
				if (res.statusCode >= 200 && res.statusCode < 300) {
					resolve(res.data);
					return;
				}
				reject(new Error(res.data?.message || `请求失败(${res.statusCode})`));
			},
			fail: (err) => reject(err)
		});
	});
}

export function login(nickname = '用户', avatar = '') {
	return new Promise((resolve, reject) => {
		// 1. 调用微信接口获取 code
		uni.login({
			provider: 'weixin',
			success: (loginRes) => {
				if (loginRes.code) {
					// 2. 将 code 发送给后端
					request({
						url: '/api/auth/login',
						method: 'POST',
						data: { 
							code: loginRes.code,
							nickname: nickname,
							avatar: avatar
						}
					}).then(resolve).catch(reject);
				} else {
					reject(new Error('获取微信code失败'));
				}
			},
			fail: (err) => reject(new Error('微信登录调用失败'))
		});
	});
}

export function speechToText(audioFilePath) {
	return new Promise((resolve, reject) => {
		const token = uni.getStorageSync('token');
		uni.uploadFile({
			url: `${config.backend.baseUrl}/api/asr/recognize`,
			filePath: audioFilePath,
			name: 'audio',
			formData: {},
			header: token ? { Authorization: `Bearer ${token}` } : {},
			success: (res) => {
				try {
					const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
					if (res.statusCode >= 200 && res.statusCode < 300 && data?.text) {
						resolve(data.text);
						return;
					}
					reject(new Error(data?.message || '语音识别失败'));
				} catch (e) {
					reject(new Error('语音识别响应解析失败'));
				}
			},
			fail: (err) => reject(err)
		});
	});
}

export function textToImage(prompt) {
	const userInfo = uni.getStorageSync('userInfo') || {};
	return request({
		url: '/api/images/generate',
		method: 'POST',
		data: {
			text: prompt,
			openid: userInfo.openid || ''
		}
	});
}

export function getHistoryList(page = 1, pageSize = 10) {
	const userInfo = uni.getStorageSync('userInfo') || {};
	return request({
		url: `/api/images?page=${page}&pageSize=${pageSize}&openid=${encodeURIComponent(userInfo.openid || '')}`,
		method: 'GET'
	});
}

export function getImageDetail(id) {
	return request({
		url: `/api/images/${id}`,
		method: 'GET'
	});
}

export function deleteImage(id) {
	return request({
		url: `/api/images/${id}`,
		method: 'DELETE'
	});
}

export function recordPrint(imageId) {
	const userInfo = uni.getStorageSync('userInfo') || {};
	return request({
		url: '/api/prints',
		method: 'POST',
		data: {
			imageId: imageId,
			openid: userInfo.openid || ''
		}
	});
}

export function getPrintCount() {
	const userInfo = uni.getStorageSync('userInfo') || {};
	const openid = userInfo.openid || '';
	return request({
		url: '/api/prints/count?openid=' + encodeURIComponent(openid),
		method: 'GET'
	});
}

export function updateUserInfo(nickname, avatar) {
	const userInfo = uni.getStorageSync('userInfo') || {};
	const openid = userInfo.openid || '';
	return request({
		url: '/api/user/update',
		method: 'POST',
		data: { 
			nickname: nickname,
			avatar: avatar,
			openid: openid
		}
	});
}

export function translateToEnglish(text) {
	return request({
		url: '/api/translate',
		method: 'POST',
		data: { text: text }
	});
}
