<template>
	<view class="feedback-container">
		<!-- 常见问题 -->
		<view class="section">
			<text class="section-title">常见问题</text>
			<view class="faq-list">
				<view
					class="faq-item"
					v-for="(item, index) in faqList"
					:key="index"
					@click="toggleFaq(index)"
				>
					<view class="faq-question">
						<text>{{ item.question }}</text>
						<image
							class="faq-arrow"
							:class="{ expanded: item.expanded }"
							src="/static/arrow-down.png"
							mode="aspectFit"
						></image>
					</view>
					<view class="faq-answer" v-if="item.expanded">
						<text>{{ item.answer }}</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 反馈表单 -->
		<view class="section">
			<text class="section-title">意见反馈</text>
			<view class="feedback-form">
				<textarea
					class="feedback-input"
					v-model="feedbackContent"
					placeholder="请描述您遇到的问题或建议..."
					maxlength="500"
				></textarea>
				<text class="word-count">{{ feedbackContent.length }}/500</text>

				<input
					class="contact-input"
					v-model="contactInfo"
					placeholder="请输入联系方式（选填）"
				/>

				<button class="submit-btn" @click="submitFeedback" :disabled="!feedbackContent.trim()">
					提交反馈
				</button>
			</view>
		</view>

		<!-- 联系方式 -->
		<view class="section contact-section">
			<text class="section-title">联系我们</text>
			<view class="contact-item">
				<text class="contact-label">客服热线</text>
				<text class="contact-value">400-123-4567</text>
			</view>
			<view class="contact-item">
				<text class="contact-label">工作时间</text>
				<text class="contact-value">周一至周五 9:00-18:00</text>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				faqList: [
					{
						question: '如何连接打印机？',
						answer: '在首页点击右上角打印机图标，进入打印机管理页面，点击"刷新"搜索设备，选择设备后点击连接即可。',
						expanded: false
					},
					{
						question: '语音识别失败怎么办？',
						answer: '请确保录音环境安静，语音清晰。如果持续失败，可以手动输入文字进行生成。',
						expanded: false
					},
					{
						question: '图片生成需要多长时间？',
						answer: '一般情况下图片生成需要5-10秒，具体时间取决于网络状况和服务器负载。',
						expanded: false
					},
					{
						question: '如何保存图片到相册？',
						answer: '在图片详情页点击"保存"按钮，系统会请求相册权限，授权后即可保存到手机相册。',
						expanded: false
					},
					{
						question: '打印出来的图片不清晰怎么办？',
						answer: '请检查打印机纸张是否充足，清洁打印头，或尝试更换更高质量的图片。',
						expanded: false
					}
				],
				feedbackContent: '',
				contactInfo: ''
			}
		},
		methods: {
			toggleFaq(index) {
				this.faqList[index].expanded = !this.faqList[index].expanded
			},
			submitFeedback() {
				if (!this.feedbackContent.trim()) {
					uni.showToast({
						title: '请输入反馈内容',
						icon: 'none'
					})
					return
				}

				uni.showLoading({
					title: '提交中...'
				})

				setTimeout(() => {
					uni.hideLoading()
					uni.showToast({
						title: '提交成功',
						icon: 'success'
					})
					this.feedbackContent = ''
					this.contactInfo = ''
				}, 1500)
			}
		}
	}
</script>

<style>
	.feedback-container {
		min-height: 100vh;
		background-color: #f8f9fa;
		padding: 30rpx;
	}

	.section {
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
		margin-bottom: 24rpx;
	}

	.faq-list {
		display: flex;
		flex-direction: column;
	}

	.faq-item {
		border-bottom: 1rpx solid #f5f5f5;
	}

	.faq-item:last-child {
		border-bottom: none;
	}

	.faq-question {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 24rpx 0;
	}

	.faq-question text {
		font-size: 28rpx;
		color: #333;
		flex: 1;
	}

	.faq-arrow {
		width: 32rpx;
		height: 32rpx;
		transition: transform 0.3s;
	}

	.faq-arrow.expanded {
		transform: rotate(180deg);
	}

	.faq-answer {
		padding-bottom: 24rpx;
	}

	.faq-answer text {
		font-size: 26rpx;
		color: #666;
		line-height: 1.6;
	}

	.feedback-form {
		display: flex;
		flex-direction: column;
	}

	.feedback-input {
		width: 100%;
		height: 240rpx;
		background-color: #f8f9fa;
		border-radius: 12rpx;
		padding: 20rpx;
		font-size: 28rpx;
		box-sizing: border-box;
	}

	.word-count {
		font-size: 24rpx;
		color: #999;
		text-align: right;
		margin-top: 10rpx;
		margin-bottom: 30rpx;
	}

	.contact-input {
		width: 100%;
		height: 88rpx;
		background-color: #f8f9fa;
		border-radius: 12rpx;
		padding: 0 20rpx;
		font-size: 28rpx;
		box-sizing: border-box;
		margin-bottom: 40rpx;
	}

	.submit-btn {
		width: 100%;
		height: 96rpx;
		background: linear-gradient(135deg, #1890ff 0%, #36a3f9 100%);
		color: #fff;
		border-radius: 48rpx;
		font-size: 32rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		border: none;
	}

	.submit-btn::after {
		border: none;
	}

	.submit-btn[disabled] {
		opacity: 0.6;
	}

	.contact-section .contact-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 20rpx 0;
	}

	.contact-label {
		font-size: 28rpx;
		color: #666;
	}

	.contact-value {
		font-size: 28rpx;
		color: #333;
	}
</style>
