<template>
	<view class="login-container">
		<view class="logo-area">
			<image class="logo" src="/static/icons/logo.jpg" mode="aspectFill"></image>
			<text class="app-name">AIDBOX</text>
			<text class="slogan">语音绘打印，用声音描绘美好生活</text>
		</view>

		<button class="login-btn" @click="handleLogin">
			<image class="icon-wx" src="/static/icons/wechat.png" mode="aspectFit"></image>
			<text>微信授权登录</text>
		</button>

		<text class="agreement">登录即表示同意</text>
		<view class="agreement-links">
			<text class="link">《用户协议》</text>
			<text>和</text>
			<text class="link">《隐私政策》</text>
		</view>
	</view>
</template>

<script>
	import { login } from '@/utils/api.js';

	export default {
		data() { return {} },
		methods: {
			async handleLogin() {
				uni.showLoading({ title: '登录中...' })
				try {
					const data = await login();
					uni.setStorageSync('token', data.token);
					uni.setStorageSync('userInfo', data.user || {});
					uni.hideLoading()
					
					const user = data.user || {};
					const needBind = !user.avatar || user.avatar === '' || 
									 !user.nickname || user.nickname === '微信用户' || user.nickname === '';
					
					if (needBind) {
						uni.redirectTo({ url: '/pages/login/bind-profile' })
					} else {
						uni.reLaunch({ url: '/pages/index/index' })
					}
				} catch (e) {
					uni.hideLoading();
					uni.showToast({ title: '登录失败', icon: 'none' });
				}
			}
		}
	}
</script>

<style>
	.login-container { height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; background-color: #FFF8F3; padding: 60rpx; }
	.logo-area { display: flex; flex-direction: column; align-items: center; margin-bottom: 120rpx; }
	.logo { width: 180rpx; height: 180rpx; border-radius: 36rpx; margin-bottom: 30rpx; box-shadow: 0 10rpx 30rpx rgba(239, 168, 124, 0.3); }
	.app-name { font-size: 48rpx; font-weight: bold; color: #EFA87C; margin-bottom: 16rpx; letter-spacing: 4rpx; }
	.slogan { font-size: 28rpx; color: #888; }
	.login-btn { width: 560rpx; height: 96rpx; background: linear-gradient(135deg, #EFA87C 0%, #FCE089 100%); color: #fff; border-radius: 48rpx; display: flex; align-items: center; justify-content: center; font-size: 32rpx; margin-bottom: 40rpx; border: none; box-shadow: 0 8rpx 20rpx rgba(239, 168, 124, 0.4); }
	.login-btn::after { border: none; }
	.icon-wx { width: 48rpx; height: 48rpx; margin-right: 16rpx; }
	.agreement { font-size: 24rpx; color: #999; margin-bottom: 10rpx; }
	.agreement-links { font-size: 24rpx; color: #999; }
	.link { color: #EFA87C; text-decoration: underline; }
</style>
