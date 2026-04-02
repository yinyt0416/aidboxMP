<template>
	<view class="bind-container">
		<view class="header">
			<text class="title">完善个人信息</text>
			<text class="subtitle">请设置您的头像和昵称</text>
		</view>

		<view class="form-area">
			<view class="avatar-section">
				<button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
					<image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill"></image>
					<view v-else class="avatar-placeholder">
						<text class="plus">+</text>
						<text class="hint">点击选择头像</text>
					</view>
				</button>
				<text class="avatar-tip">点击头像可选择微信头像、相册或拍照</text>
			</view>

			<view class="input-area">
				<text class="label">昵称</text>
				<input 
					class="nickname-input" 
					type="nickname"
					v-model="nickname" 
					placeholder="点击获取微信昵称或手动输入" 
					maxlength="20"
					@blur="onNicknameBlur"
					@input="onNicknameInput"
				/>
			</view>

			<button class="submit-btn" :disabled="!canSubmit" @click="handleSubmit">
				确认绑定
			</button>
		</view>
	</view>
</template>

<script>
	import { updateUserInfo } from '@/utils/api.js';

	export default {
		data() {
			return {
				avatarUrl: '',
				nickname: '',
				userInfo: null
			}
		},
		computed: {
			canSubmit() {
				return this.avatarUrl && this.nickname.trim().length > 0;
			}
		},
		onLoad() {
			this.userInfo = uni.getStorageSync('userInfo') || {};
			if (this.userInfo.avatar) {
				this.avatarUrl = this.userInfo.avatar;
			}
			if (this.userInfo.nickname && this.userInfo.nickname !== '微信用户') {
				this.nickname = this.userInfo.nickname;
			}
		},
		methods: {
			onChooseAvatar(e) {
				const avatarUrl = e.detail.avatarUrl;
				if (avatarUrl) {
					this.uploadAvatar(avatarUrl);
				}
			},
			uploadAvatar(filePath) {
				uni.showLoading({ title: '上传中...' });
				const userInfo = uni.getStorageSync('userInfo') || {};
				const token = uni.getStorageSync('token');
				uni.uploadFile({
					url: 'https://aidbox.top/api/user/avatar',
					filePath: filePath,
					name: 'avatar',
					formData: {
						openid: userInfo.openid || ''
					},
					header: token ? { Authorization: `Bearer ${token}` } : {},
					success: (res) => {
						uni.hideLoading();
						try {
							const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
							if (res.statusCode >= 200 && res.statusCode < 300 && data.avatar) {
								this.avatarUrl = data.avatar;
								uni.showToast({ title: '头像设置成功', icon: 'success' });
							} else {
								this.avatarUrl = filePath;
								uni.showToast({ title: '头像已选择', icon: 'success' });
							}
						} catch (e) {
							this.avatarUrl = filePath;
						}
					},
					fail: (err) => {
						uni.hideLoading();
						this.avatarUrl = filePath;
						uni.showToast({ title: '头像已选择', icon: 'success' });
					}
				});
			},
			onNicknameBlur(e) {
				if (e.detail.value) {
					this.nickname = e.detail.value;
				}
			},
			onNicknameInput(e) {
				this.nickname = e.detail.value;
			},
			async handleSubmit() {
				if (!this.canSubmit) {
					uni.showToast({ title: '请完善信息', icon: 'none' });
					return;
				}

				uni.showLoading({ title: '保存中...' });
				try {
					const result = await updateUserInfo(this.nickname.trim(), this.avatarUrl);
					const serverUser = result.user || {};
					const updatedUser = {
						...this.userInfo,
						id: serverUser.id || this.userInfo.id,
						username: serverUser.username || this.userInfo.username,
						nickname: serverUser.nickname || this.nickname.trim(),
						avatar: serverUser.avatar || this.avatarUrl,
						openid: this.userInfo.openid || serverUser.openid
					};
					uni.setStorageSync('userInfo', updatedUser);
					uni.hideLoading();
					uni.showToast({ title: '绑定成功', icon: 'success' });
					setTimeout(() => {
						uni.reLaunch({ url: '/pages/index/index' });
					}, 1000);
				} catch (e) {
					uni.hideLoading();
					uni.showToast({ title: '保存失败', icon: 'none' });
				}
			}
		}
	}
</script>

<style>
	.bind-container {
		min-height: 100vh;
		display: flex;
		flex-direction: column;
		align-items: center;
		background-color: #FFF8F3;
		padding: 80rpx 60rpx;
	}

	.header {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-bottom: 60rpx;
	}

	.title {
		font-size: 44rpx;
		font-weight: bold;
		color: #333;
		margin-bottom: 20rpx;
	}

	.subtitle {
		font-size: 28rpx;
		color: #888;
	}

	.form-area {
		width: 100%;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.avatar-section {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-bottom: 40rpx;
	}

	.avatar-btn {
		width: 200rpx;
		height: 200rpx;
		border-radius: 100rpx;
		background-color: #f5f5f5;
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		border: 4rpx dashed #ddd;
		padding: 0;
		margin: 0;
	}

	.avatar-btn::after {
		border: none;
	}

	.avatar {
		width: 100%;
		height: 100%;
		border-radius: 100rpx;
	}

	.avatar-placeholder {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.plus {
		font-size: 60rpx;
		color: #ccc;
		line-height: 1;
	}

	.hint {
		font-size: 22rpx;
		color: #999;
		margin-top: 10rpx;
	}

	.avatar-tip {
		font-size: 24rpx;
		color: #999;
		margin-top: 16rpx;
	}

	.input-area {
		width: 100%;
		margin-bottom: 60rpx;
	}

	.label {
		font-size: 28rpx;
		color: #666;
		margin-bottom: 16rpx;
		display: block;
	}

	.nickname-input {
		width: 100%;
		height: 96rpx;
		background-color: #fff;
		border-radius: 16rpx;
		padding: 0 30rpx;
		font-size: 32rpx;
		border: 2rpx solid #eee;
		box-sizing: border-box;
	}

	.nickname-input:focus {
		border-color: #EFA87C;
	}

	.submit-btn {
		width: 100%;
		height: 96rpx;
		background: linear-gradient(135deg, #EFA87C 0%, #FCE089 100%);
		color: #fff;
		border-radius: 48rpx;
		font-size: 32rpx;
		border: none;
		box-shadow: 0 8rpx 20rpx rgba(239, 168, 124, 0.4);
	}

	.submit-btn[disabled] {
		background: #ccc;
		box-shadow: none;
	}

	.submit-btn::after {
		border: none;
	}
</style>
