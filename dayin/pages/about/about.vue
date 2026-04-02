<template>
	<view class="about-container">
		<!-- Logo和名称 -->
		<view class="logo-section">
			<image class="app-logo" src="/static/logo.png" mode="aspectFit"></image>
			<text class="app-name">语音绘打印</text>
			<text class="app-version">版本 {{ version }}</text>
		</view>

		<!-- 简介 -->
		<view class="intro-section">
			<text class="section-title">应用简介</text>
			<text class="intro-text">
				语音绘打印是一款基于人工智能技术的创新应用，通过语音识别您的将语音转换为文字，并利用AI生成与之匹配的精美图片。您可以将生成的图片保存到相册，或通过蓝牙连接打印机进行打印，让创意变为现实。
			</text>
		</view>

		<!-- 协议链接 -->
		<view class="link-section">
			<view class="link-item" @click="openProtocol('privacy')">
				<text>隐私政策</text>
				<image class="arrow" src="/static/arrow-right.png" mode="aspectFit"></image>
			</view>
			<view class="link-item" @click="openProtocol('user')">
				<text>用户协议</text>
				<image class="arrow" src="/static/arrow-right.png" mode="aspectFit"></image>
			</view>
			<view class="link-item" @click="openProtocol('copyright')">
				<text>版权声明</text>
				<image class="arrow" src="/static/arrow-right.png" mode="aspectFit"></image>
			</view>
		</view>

		<!-- 底部信息 -->
		<view class="footer-section">
			<text class="copyright">Copyright © 2024 语音绘打印</text>
			<text class="company">All Rights Reserved</text>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				version: '1.0.0'
			}
		},
		methods: {
			openProtocol(type) {
				let title = ''
				switch (type) {
					case 'privacy':
						title = '隐私政策'
						break
					case 'user':
						title = '用户协议'
						break
					case 'copyright':
						title = '版权声明'
						break
				}

				uni.showModal({
					title: title,
					content: `${title}内容开发中...`,
					showCancel: false
				})
			}
		}
	}
</script>

<style>
	.about-container {
		min-height: 100vh;
		background-color: #f8f9fa;
		padding: 30rpx;
	}

	.logo-section {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 80rpx 0 60rpx;
	}

	.app-logo {
		width: 160rpx;
		height: 160rpx;
		border-radius: 32rpx;
		margin-bottom: 24rpx;
	}

	.app-name {
		font-size: 36rpx;
		color: #333;
		font-weight: bold;
		margin-bottom: 12rpx;
	}

	.app-version {
		font-size: 26rpx;
		color: #999;
	}

	.intro-section {
		background-color: #fff;
		border-radius: 16rpx;
		padding: 30rpx;
		margin-bottom: 30rpx;
		box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.03);
	}

	.section-title {
		font-size: 30rpx;
		color: #333;
		font-weight: bold;
		display: block;
		margin-bottom: 20rpx;
	}

	.intro-text {
		font-size: 28rpx;
		color: #666;
		line-height: 1.8;
	}

	.link-section {
		background-color: #fff;
		border-radius: 16rpx;
		padding: 0 30rpx;
		margin-bottom: 60rpx;
		box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.03);
	}

	.link-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 30rpx 0;
		border-bottom: 1rpx solid #f5f5f5;
	}

	.link-item:last-child {
		border-bottom: none;
	}

	.link-item text {
		font-size: 28rpx;
		color: #333;
	}

	.arrow {
		width: 24rpx;
		height: 24rpx;
	}

	.footer-section {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 40rpx 0;
	}

	.copyright {
		font-size: 24rpx;
		color: #999;
		margin-bottom: 8rpx;
	}

	.company {
		font-size: 22rpx;
		color: #ccc;
	}
</style>
