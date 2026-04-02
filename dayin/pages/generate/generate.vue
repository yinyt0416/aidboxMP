<template>
	<view class="generate-container">
		<view class="text-section">
			<text class="section-title">识别文字</text>
			<textarea
				class="text-input"
				v-model="recognizedText"
				placeholder="请确认或修改识别的文字"
				maxlength="500"
			/>
			<text class="word-count">{{ recognizedText.length }}/500</text>
		</view>

		<view class="tips-section">
			<text class="tips-title">生成提示</text>
			<text class="tips-text">图片风格：默认风格（可在设置中修改）</text>
			<text class="tips-text">生成时间：约5-10秒</text>
		</view>

		<view class="btn-section">
			<button class="btn retry-btn" @click="retryRecord">重录</button>
			<button class="btn generate-btn" @click="generateImage" :disabled="isGenerating">
				{{ isGenerating ? '生成中...' : '生成图片' }}
			</button>
		</view>

		<!-- 生成中遮罩 -->
		<view class="loading-mask" v-if="isGenerating">
			<view class="loading-content">
				<view class="loading-spinner"></view>
				<text class="loading-text">正在生成图片...</text>
				<text class="loading-hint">大约需要5-10秒</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { textToImage } from '@/utils/api.js';

	export default {
		data() {
			return {
				recognizedText: '',
				isGenerating: false,
				generatedImageUrl: ''
			};
		},
		onLoad(options) {
			if (options.text) {
				try {
					this.recognizedText = decodeURIComponent(options.text);
				} catch (e) {
					console.error('解码失败:', e);
					this.recognizedText = options.text;
				}
			}
		},
		methods: {
			retryRecord() {
				uni.navigateBack();
			},
			async generateImage() {
				if (!this.recognizedText.trim()) {
					uni.showToast({
						title: '请输入文字',
						icon: 'none'
					});
					return;
				}

				this.isGenerating = true;

				try {
					uni.showLoading({ title: '生成中...' });
					const result = await textToImage(this.recognizedText);
					uni.hideLoading();
					
					const imageUrl = result.url;
					this.generatedImageUrl = imageUrl;

					uni.navigateTo({
						url: `/pages/detail/detail?id=${result.id || Date.now()}&imageUrl=${encodeURIComponent(imageUrl)}&text=${encodeURIComponent(this.recognizedText)}`
					});
				} catch (err) {
					uni.hideLoading();
					uni.showToast({
						title: '生成失败: ' + err.message,
						icon: 'none'
					});
				} finally {
					this.isGenerating = false;
				}
			}
		}
	};
</script>

<style>
	.generate-container {
		min-height: 100vh;
		background-color: #f8f9fa;
		padding: 30rpx;
	}

	.text-section {
		background-color: #fff;
		border-radius: 24rpx;
		padding: 30rpx;
		margin-bottom: 30rpx;
		box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.03);
	}

	.section-title {
		font-size: 28rpx;
		color: #333;
		font-weight: bold;
		display: block;
		margin-bottom: 20rpx;
	}

	.text-input {
		width: 100%;
		height: 200rpx;
		font-size: 28rpx;
		line-height: 1.6;
	}

	.word-count {
		font-size: 24rpx;
		color: #999;
		display: block;
		text-align: right;
		margin-top: 10rpx;
	}

	.tips-section {
		background-color: #fff;
		border-radius: 24rpx;
		padding: 30rpx;
		margin-bottom: 60rpx;
		box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.03);
	}

	.tips-title {
		font-size: 28rpx;
		color: #333;
		font-weight: bold;
		display: block;
		margin-bottom: 20rpx;
	}

	.tips-text {
		font-size: 26rpx;
		color: #666;
		display: block;
		margin-bottom: 10rpx;
	}

	.btn-section {
		display: flex;
		gap: 30rpx;
	}

	.btn {
		flex: 1;
		height: 96rpx;
		border-radius: 48rpx;
		font-size: 32rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border: none;
	}

	.btn::after {
		border: none;
	}

	.retry-btn {
		background-color: #fff;
		color: #666;
		border: 2rpx solid #e8e8e8;
	}

	.generate-btn {
		background: linear-gradient(135deg, #FF7A00 0%, #FFC300 100%);
		color: #fff;
	}

	.generate-btn[disabled] {
		opacity: 0.6;
	}

	.loading-mask {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background-color: rgba(255, 255, 255, 0.95);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 999;
	}

	.loading-content {
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.loading-spinner {
		width: 80rpx;
		height: 80rpx;
		border: 6rpx solid #e8e8e8;
		border-top-color: #1890ff;
		border-radius: 50%;
		animation: spin 1s linear infinite;
		margin-bottom: 30rpx;
	}

	@keyframes spin {
		to {
			transform: rotate(360deg);
		}
	}

	.loading-text {
		font-size: 32rpx;
		color: #333;
		margin-bottom: 16rpx;
	}

	.loading-hint {
		font-size: 26rpx;
		color: #999;
	}
</style>
