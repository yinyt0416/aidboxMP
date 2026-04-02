<template>
	<view class="index-container">
		<!-- 顶部状态栏 -->
		<view class="header">
			<view class="logo-section">
				<image class="logo-img" src="/static/icons/logo.jpg" mode="aspectFill"></image>
				<view class="title-info">
					<text class="app-title">AIDBOX</text>
					<text class="app-subtitle">语音绘打印</text>
				</view>
			</view>
			<view class="printer-status" @click="goPrinter">
				<image class="icon-printer" src="/static/icons/printer.svg" mode="aspectFit"></image>
				<text class="status-text">{{ printerStatus }}</text>
			</view>
		</view>

		<!-- 最近生成图片展示 -->
		<view class="recent-card" v-if="recentImage && recentImage.url" @click="goDetail(recentImage.id)">
			<image class="recent-img" :src="recentImage.url" mode="aspectFill"></image>
			<view class="recent-info">
				<text class="recent-text">{{ recentImage.text }}</text>
				<text class="recent-time">{{ recentImage.time }}</text>
			</view>
		</view>

		<!-- 空白状态 -->
		<view class="empty-state" v-else>
			<image class="icon-empty" src="/static/icons/image.svg" mode="aspectFit"></image>
			<text class="empty-text">还没有生成过图片哦</text>
			<text class="empty-hint">按住下方按钮开始录音</text>
		</view>

		<!-- 录音按钮区域 -->
		<view class="record-area">
			<view class="record-wrapper">
				<view class="record-btn" :class="{ recording: isRecording }" @touchstart="startRecord" @touchend="stopRecord">
					<image class="icon-mic" src="/static/icons/mic.svg" mode="aspectFit"></image>
				</view>
				<text class="record-hint">{{ isRecording ? '松开结束录音' : '按住说话' }}</text>
			</view>
		</view>

		<!-- 录音中提示 -->
		<view class="recording-tip" v-if="isRecording">
			<view class="recording-dot"></view>
			<text>正在录音...</text>
		</view>

		<!-- 历史记录入口 -->
		<view class="history-entry" @click="goHistory">
			<text>查看历史记录</text>
			<image class="icon-arrow" src="/static/icons/chevron-right.svg" mode="aspectFit"></image>
		</view>
	</view>
</template>

<script>
	import { speechToText, getHistoryList } from '@/utils/api.js';

	export default {
		data() {
			return {
				userInfo: { nickname: '' },
				printerStatus: '未连接',
				isRecording: false,
				isCanceling: false,
				recordingStartTime: 0,
				recentImage: null,
				recorderManager: null
			};
		},
		onLoad() {
			const token = uni.getStorageSync('token');
			if (!token) {
				uni.redirectTo({ url: '/pages/login/login' });
			} else {
				this.userInfo = uni.getStorageSync('userInfo') || {};
			}

			this.initRecorder();
			this.loadRecentImage();
		},
		onShow() {
			this.loadRecentImage();
		},
		methods: {
			async loadRecentImage() {
				try {
					const res = await getHistoryList(1, 1);
					this.recentImage = (res.list && res.list[0]) || null;
				} catch (e) {
					this.recentImage = null;
				}
			},
			initRecorder() {
				const recorderManager = uni.getRecorderManager();

				recorderManager.onStart(() => {
					console.log('录音开始');
				});

				recorderManager.onStop((res) => {
					console.log('录音结束', res.tempFilePath);
					
					// 如果是取消或时间太短，不进行识别
					if (this.isCanceling) {
						this.isCanceling = false;
						return;
					}

					this.recognizeSpeech(res.tempFilePath);
				});

				recorderManager.onError((err) => {
					console.error('录音错误', err);
					uni.showToast({ title: '录音失败', icon: 'none' });
				});

				this.recorderManager = recorderManager;
			},
			startRecord() {
				this.isRecording = true;
				this.recordingStartTime = Date.now();
				if (this.recorderManager) {
					this.recorderManager.start({
						format: 'wav',
						sampleRate: 16000,
						numberOfChannels: 1,
						encodeBitRate: 64000,
						duration: 60000
					});
				}
			},
			stopRecord() {
				const duration = Date.now() - this.recordingStartTime;
				if (duration < 1000) {
					uni.showToast({ title: '说话时间太短', icon: 'none' });
					this.isRecording = false;
					this.isCanceling = true;
					if (this.recorderManager) {
						this.recorderManager.stop();
					}
					return;
				}

				this.isRecording = false;
				if (this.recorderManager) {
					this.recorderManager.stop();
				}
			},
			recognizeSpeech(audioPath) {
				uni.showLoading({ title: '识别中...' });
				speechToText(audioPath).then(text => {
					uni.hideLoading();
					uni.navigateTo({
						url: `/pages/generate/generate?text=${encodeURIComponent(text)}`
					});
				}).catch(err => {
					uni.hideLoading();
					uni.showToast({ title: err.message || '识别失败', icon: 'none' });
				});
			},
			goProfile() {
				uni.switchTab({ url: '/pages/profile/profile' });
			},
			goPrinter() {
				uni.navigateTo({ url: '/pages/printer/printer' });
			},
			goHistory() {
				uni.switchTab({ url: '/pages/history/history' });
			},
			goDetail(id) {
				uni.navigateTo({ url: `/pages/detail/detail?id=${id}` });
			}
		}
	};
</script>

<style>
	.index-container {
		min-height: 100vh;
		background-color: #FFF8F3;
		padding: 30rpx;
		padding-bottom: 140rpx;
	}

	.header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-bottom: 40rpx;
	}

	.logo-section {
		display: flex;
		align-items: center;
	}

	.logo-img {
		width: 80rpx;
		height: 80rpx;
		border-radius: 20rpx;
		margin-right: 20rpx;
		box-shadow: 0 4rpx 12rpx rgba(239, 168, 124, 0.3);
	}

	.title-info {
		display: flex;
		flex-direction: column;
	}

	.app-title {
		font-size: 32rpx;
		font-weight: bold;
		color: #EFA87C;
		letter-spacing: 2rpx;
	}

	.app-subtitle {
		font-size: 22rpx;
		color: #999;
		margin-top: 4rpx;
	}

	.printer-status {
		display: flex;
		align-items: center;
		background-color: #fff;
		padding: 12rpx 24rpx;
		border-radius: 30rpx;
		box-shadow: 0 4rpx 12rpx rgba(239, 168, 124, 0.15);
	}

	.icon-printer {
		width: 32rpx;
		height: 32rpx;
		margin-right: 10rpx;
	}

	.status-text {
		font-size: 24rpx;
		color: #EFA87C;
		font-weight: 500;
	}

	.recent-card {
		background-color: #fff;
		border-radius: 24rpx;
		overflow: hidden;
		margin-bottom: 60rpx;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
	}

	.recent-img {
		width: 100%;
		height: 400rpx;
	}

	.recent-info {
		padding: 24rpx;
	}

	.recent-text {
		font-size: 28rpx;
		color: #333;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
		margin-bottom: 12rpx;
	}

	.recent-time {
		font-size: 24rpx;
		color: #999;
	}

	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 120rpx 0;
	}

	.icon-empty {
		width: 120rpx;
		height: 120rpx;
		margin-bottom: 30rpx;
		opacity: 0.3;
	}

	.empty-text {
		font-size: 32rpx;
		color: #666;
		margin-bottom: 16rpx;
	}

	.empty-hint {
		font-size: 26rpx;
		color: #999;
	}

	.record-area {
		display: flex;
		justify-content: center;
		padding: 60rpx 0;
	}

	.record-wrapper {
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.record-btn {
		width: 200rpx;
		height: 200rpx;
		border-radius: 50%;
		background: linear-gradient(135deg, #EFA87C 0%, #FCE089 100%);
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 10rpx 40rpx rgba(239, 168, 124, 0.4);
		transition: transform 0.2s;
	}

	.record-btn.recording {
		transform: scale(1.1);
	}

	.icon-mic {
		width: 80rpx;
		height: 80rpx;
	}

	.record-hint {
		font-size: 28rpx;
		color: #666;
		margin-top: 30rpx;
	}

	.recording-tip {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 20rpx;
	}

	.recording-dot {
		width: 16rpx;
		height: 16rpx;
		background-color: #EFA87C;
		border-radius: 50%;
		margin-right: 10rpx;
		animation: pulse 1s infinite;
	}

	@keyframes pulse {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.5; }
	}

	.recording-tip text {
		font-size: 28rpx;
		color: #EFA87C;
	}

	.history-entry {
		display: flex;
		justify-content: center;
		align-items: center;
		padding: 30rpx;
	}

	.history-entry text {
		font-size: 28rpx;
		color: #EFA87C;
		margin-right: 10rpx;
	}

	.icon-arrow {
		width: 32rpx;
		height: 32rpx;
	}
</style>
