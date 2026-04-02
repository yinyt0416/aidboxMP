<template>
	<view class="profile-container">
		<!-- 用户信息头部 -->
		<view class="profile-header">
			<view class="header-bg"></view>
			<view class="user-section">
				<button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
					<image class="avatar-img" :src="userInfo.avatar || 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'" mode="aspectFill"></image>
				</button>
				<view class="user-info">
					<view class="nickname-wrapper">
						<input type="nickname" class="nickname-input" v-model="userInfo.nickname" @blur="onNicknameBlur" placeholder="点击设置昵称"/>
						<image class="edit-icon" src="/static/icons/edit.svg" mode="aspectFit" v-if="!userInfo.nickname"></image>
					</view>
					<text class="user-id" v-if="userInfo.id">ID: {{ userInfo.id }}</text>
				</view>
			</view>
		</view>

		<!-- 统计数据 -->
		<view class="stats-section">
			<view class="stat-item">
				<text class="stat-num">{{ stats.imageCount }}</text>
				<text class="stat-label">图片数量</text>
			</view>
			<view class="stat-item">
				<text class="stat-num">{{ stats.printCount }}</text>
				<text class="stat-label">打印次数</text>
			</view>
			<view class="stat-item">
				<text class="stat-num">{{ stats.dayCount }}</text>
				<text class="stat-label">使用天数</text>
			</view>
		</view>

		<!-- 功能菜单 -->
		<view class="menu-section">
			<view class="menu-item" @click="goHistory">
				<image class="menu-icon" src="/static/icons/clock.svg" mode="aspectFit"></image>
				<text class="menu-text">历史记录</text>
				<image class="icon-arrow" src="/static/icons/chevron-right.svg" mode="aspectFit"></image>
			</view>
			<view class="menu-item" @click="goPrinter">
				<image class="menu-icon" src="/static/icons/printer.svg" mode="aspectFit"></image>
				<text class="menu-text">打印机管理</text>
				<image class="icon-arrow" src="/static/icons/chevron-right.svg" mode="aspectFit"></image>
			</view>
			<view class="menu-item" @click="goFeedback">
				<image class="menu-icon" src="/static/icons/message-circle.svg" mode="aspectFit"></image>
				<text class="menu-text">帮助与反馈</text>
				<image class="icon-arrow" src="/static/icons/chevron-right.svg" mode="aspectFit"></image>
			</view>
			<view class="menu-item" @click="goAbout">
				<image class="menu-icon" src="/static/icons/info.svg" mode="aspectFit"></image>
				<text class="menu-text">关于我们</text>
				<image class="icon-arrow" src="/static/icons/chevron-right.svg" mode="aspectFit"></image>
			</view>
		</view>

		<!-- 退出登录 -->
		<view class="logout-section">
			<button class="logout-btn" @click="handleLogout" v-if="isLoggedIn">退出登录</button>
		</view>
	</view>
</template>

<script>
	import { getHistoryList, getPrintCount, updateUserInfo } from '@/utils/api.js';

	export default {
		data() {
			return {
				userInfo: {
					id: '',
					nickname: '',
					avatar: '',
					createdAt: 0
				},
				stats: { imageCount: 0, printCount: 0, dayCount: 0 },
				isLoggedIn: false
			}
		},
		onLoad() { this.initData() },
		onShow() { this.initData() },
		methods: {
			async initData() {
				const token = uni.getStorageSync('token');
				if (token) {
					this.isLoggedIn = true;
					const storedUser = uni.getStorageSync('userInfo');
					if (storedUser) {
						this.userInfo = { ...this.userInfo, ...storedUser };
					}
					this.calculateDays();
					await this.fetchRealStats();
				} else {
					this.isLoggedIn = false;
				}
			},
			calculateDays() {
				if (this.userInfo.createdAt) {
					const diff = Date.now() - this.userInfo.createdAt;
					this.stats.dayCount = Math.max(1, Math.ceil(diff / (1000 * 60 * 60 * 24)));
				} else {
					this.stats.dayCount = 1;
				}
			},
			async fetchRealStats() {
				try {
					const res = await getHistoryList(1, 1);
					if (res && typeof res.total !== 'undefined') {
						this.stats.imageCount = res.total;
					}
					const printRes = await getPrintCount();
					this.stats.printCount = printRes.count || 0;
				} catch (e) {
					console.error('获取统计数据失败', e);
				}
			},
			async onChooseAvatar(e) {
				const { avatarUrl } = e.detail;
				this.userInfo.avatar = avatarUrl;
				uni.setStorageSync('userInfo', this.userInfo);
				await this.saveUserInfo();
			},
			async onNicknameBlur(e) {
				const name = e.detail.value;
				if (name && name !== this.userInfo.nickname) {
					this.userInfo.nickname = name;
					uni.setStorageSync('userInfo', this.userInfo);
					await this.saveUserInfo();
				}
			},
			async saveUserInfo() {
				try {
					const res = await updateUserInfo(this.userInfo.nickname, this.userInfo.avatar);
					if (res && res.user) {
						this.userInfo = { ...this.userInfo, ...res.user };
						uni.setStorageSync('userInfo', this.userInfo);
						uni.showToast({ title: '保存成功', icon: 'success' });
					}
				} catch (e) {
					console.error('保存用户信息失败', e);
					uni.showToast({ title: '保存失败', icon: 'none' });
				}
			},
			goIndex() { uni.switchTab({ url: '/pages/index/index' }) },
			goHistory() { uni.switchTab({ url: '/pages/history/history' }) },
			goPrinter() { uni.navigateTo({ url: '/pages/printer/printer' }) },
			goFeedback() { uni.navigateTo({ url: '/pages/feedback/feedback' }) },
			goAbout() { uni.navigateTo({ url: '/pages/about/about' }) },
			handleLogout() {
				uni.showModal({
					title: '确认退出',
					content: '确定要退出登录吗？',
					success: (res) => {
						if (res.confirm) {
							uni.clearStorageSync();
							uni.reLaunch({ url: '/pages/login/login' });
						}
					}
				})
			}
		}
	}
</script>

<style>
	.profile-container { min-height: 100vh; background-color: #FFF8F3; padding-bottom: 140rpx; }
	.profile-header { position: relative; height: 400rpx; overflow: hidden; }
	.header-bg { position: absolute; top: 0; left: 0; right: 0; height: 300rpx; background: linear-gradient(135deg, #EFA87C 0%, #FCE089 100%); }
	.user-section { position: absolute; bottom: 40rpx; left: 30rpx; display: flex; align-items: center; }
	
	.avatar-btn { padding: 0; margin: 0; border: none; background: transparent; display: block; border-radius: 50%; }
	.avatar-btn::after { border: none; }
	.avatar-img { width: 120rpx; height: 120rpx; border-radius: 50%; border: 4rpx solid #fff; margin-right: 24rpx; background-color: #fff; display: block; }
	
	.user-info { display: flex; flex-direction: column; }
	.nickname-wrapper { display: flex; align-items: center; margin-bottom: 8rpx; }
	.nickname-input { font-size: 36rpx; color: #fff; font-weight: bold; width: 300rpx; }
	.edit-icon { width: 32rpx; height: 32rpx; margin-left: 10rpx; opacity: 0.8; }
	
	.user-id { font-size: 24rpx; color: rgba(255, 255, 255, 0.8); }
	.stats-section { display: flex; background-color: #fff; padding: 40rpx 0; margin-bottom: 20rpx; }
	.stat-item { flex: 1; display: flex; flex-direction: column; align-items: center; border-right: 1rpx solid #FFF8F3; }
	.stat-item:last-child { border-right: none; }
	.stat-num { font-size: 40rpx; color: #EFA87C; font-weight: bold; margin-bottom: 10rpx; }
	.stat-label { font-size: 24rpx; color: #999; }
	.menu-section { background-color: #fff; padding: 0 30rpx; }
	.menu-item { display: flex; align-items: center; padding: 30rpx 0; border-bottom: 1rpx solid #f5f5f5; }
	.menu-item:last-child { border-bottom: none; }
	.menu-icon { width: 40rpx; height: 40rpx; margin-right: 20rpx; }
	.menu-text { flex: 1; font-size: 28rpx; color: #333; }
	.icon-arrow { width: 32rpx; height: 32rpx; opacity: 0.3; }
	.logout-section { padding: 60rpx 30rpx; }
	.logout-btn { width: 100%; height: 96rpx; background-color: #fff; color: #ff4d4f; border-radius: 48rpx; font-size: 32rpx; display: flex; align-items: center; justify-content: center; border: none; }
</style>
