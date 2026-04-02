<template>
	<view class="detail-container">
		<!-- 图片展示 -->
		<view class="image-section">
			<image class="detail-img" :src="imageInfo.url" mode="aspectFit" @click="previewImage"></image>
		</view>

		<!-- 文字描述 -->
		<view class="text-section">
			<text class="section-label">文字描述</text>
			<text class="detail-text">{{ imageInfo.text }}</text>
		</view>

		<!-- 生成信息 -->
		<view class="info-section">
			<view class="info-item">
				<text class="info-label">生成时间</text>
				<text class="info-value">{{ imageInfo.time }}</text>
			</view>
		</view>

		<!-- 底部操作栏 -->
		<view class="action-bar">
			<view class="action-item" @click="handleDelete">
				<image class="action-icon" src="/static/icons/trash.svg" mode="aspectFit"></image>
				<text class="action-text">删除</text>
			</view>
			<view class="action-item" @click="handleSave">
				<image class="action-icon" src="/static/icons/download.svg" mode="aspectFit"></image>
				<text class="action-text">保存</text>
			</view>
			<view class="action-item" @click="handleShare">
				<image class="action-icon" src="/static/icons/share.svg" mode="aspectFit"></image>
				<text class="action-text">分享</text>
			</view>
			<view class="action-item print-btn" @click="handlePrint">
				<image class="action-icon" src="/static/icons/printer.svg" mode="aspectFit"></image>
				<text class="action-text">打印</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getImageDetail } from '@/utils/api.js';

	export default {
		data() {
			return {
				imageInfo: {
					id: '',
					url: '',
					text: '',
					time: ''
				}
			};
		},
		onLoad(options) {
			if (options.id) {
				this.imageInfo.id = options.id;
				this.fetchImageDetail(options.id);
			}
			// 如果通过分享链接进入没有id，但有直接传的图片信息
			if (options.imageUrl) {
				this.imageInfo.url = decodeURIComponent(options.imageUrl);
			}
			if (options.text) {
				this.imageInfo.text = decodeURIComponent(options.text);
			}
		},
		onShareAppMessage() {
			return {
				title: this.imageInfo.text,
				path: `/pages/detail/detail?id=${this.imageInfo.id}`,
				imageUrl: this.imageInfo.url
			};
		},
		methods: {
			async fetchImageDetail(id) {
				try {
					uni.showLoading({ title: '加载中...' });
					const res = await getImageDetail(id);
					uni.hideLoading();

					if (res.url) {
						this.imageInfo.url = res.url;
					}
					if (res.text) {
						this.imageInfo.text = res.text;
					}
					if (res.created_at) {
						this.imageInfo.time = this.formatTime(new Date(res.created_at));
					} else {
						this.imageInfo.time = this.formatTime(new Date());
					}
				} catch (err) {
					uni.hideLoading();
					uni.showToast({ title: '获取详情失败', icon: 'none' });
					console.error('获取图片详情失败:', err);
					// 使用默认数据
					this.imageInfo.url = 'https://picsum.photos/400/640?random=' + Date.now();
					this.imageInfo.text = '春天的花园里百花齐放，美丽极了';
					this.imageInfo.time = this.formatTime(new Date());
				}
			},
			formatTime(date) {
				const year = date.getFullYear();
				const month = String(date.getMonth() + 1).padStart(2, '0');
				const day = String(date.getDate()).padStart(2, '0');
				const hour = String(date.getHours()).padStart(2, '0');
				const minute = String(date.getMinutes()).padStart(2, '0');
				const second = String(date.getSeconds()).padStart(2, '0');
				return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
			},
			previewImage() {
				uni.previewImage({
					urls: [this.imageInfo.url],
					current: 0
				});
			},
			handleSave() {
				uni.saveImageToPhotosAlbum({
					filePath: this.imageInfo.url,
					success: () => {
						uni.showToast({ title: '保存成功', icon: 'success' });
					},
					fail: (err) => {
						if (err.errMsg && err.errMsg.includes('auth deny')) {
							uni.showModal({
								title: '提示',
								content: '需要授权保存图片到相册',
								success: (res) => {
									if (res.confirm) {
										uni.openSetting();
									}
								}
							});
						}
					}
				});
			},
			handleShare() {
				// 分享功能
			},
			handleDelete() {
				uni.showModal({
					title: '确认删除',
					content: '确定要删除这张图片吗？',
					success: (res) => {
						if (res.confirm) {
							uni.showToast({ title: '删除成功', icon: 'success' });
							setTimeout(() => {
								uni.navigateBack();
							}, 1500);
						}
					}
				});
			},
			handlePrint() {
				const url = `/pages/printer/printer?image=${encodeURIComponent(this.imageInfo.url)}&id=${this.imageInfo.id}`;
				uni.navigateTo({ url });
			}
		}
	};
</script>

<style>
	.detail-container {
		min-height: 100vh;
		background-color: #f8f9fa;
		padding-bottom: 140rpx;
	}

	.image-section {
		background-color: #fff;
		padding: 20rpx;
	}

	.detail-img {
		width: 100%;
		height: 600rpx;
		border-radius: 16rpx;
	}

	.text-section {
		background-color: #fff;
		margin-top: 20rpx;
		padding: 30rpx;
	}

	.section-label {
		font-size: 26rpx;
		color: #999;
		display: block;
		margin-bottom: 16rpx;
	}

	.detail-text {
		font-size: 30rpx;
		color: #333;
		line-height: 1.6;
	}

	.info-section {
		background-color: #fff;
		margin-top: 20rpx;
		padding: 30rpx;
	}

	.info-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.info-label {
		font-size: 28rpx;
		color: #666;
	}

	.info-value {
		font-size: 28rpx;
		color: #333;
	}

	.action-bar {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		display: flex;
		background-color: #fff;
		padding: 20rpx 30rpx;
		padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
		box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
	}

	.action-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
	}

	.action-icon {
		width: 48rpx;
		height: 48rpx;
		margin-bottom: 8rpx;
	}

	.action-text {
		font-size: 24rpx;
		color: #666;
	}

	.print-btn {
		background: linear-gradient(135deg, #EFA87C 0%, #FCE089 100%);
		padding: 20rpx;
		border-radius: 16rpx;
	}

	.print-btn .action-text {
		color: #fff;
	}
</style>
