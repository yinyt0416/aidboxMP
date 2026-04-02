<template>
	<view class="history-container">
		<!-- 视图切换 -->
		<view class="tab-bar">
			<view class="tab-item" :class="{ active: viewMode === 'timeline' }" @click="viewMode = 'timeline'">
				<text>时间轴</text>
			</view>
			<view class="tab-item" :class="{ active: viewMode === 'gallery' }" @click="viewMode = 'gallery'">
				<text>画册</text>
			</view>
		</view>

		<!-- 时间轴模式 -->
		<scroll-view v-if="viewMode === 'timeline'" class="scroll-view" scroll-y @scrolltolower="loadMore">
			<view class="timeline-list">
				<view class="timeline-item" v-for="(item, index) in historyList" :key="index" @click="goDetail(item.id)">
					<view class="timeline-dot"></view>
					<view class="timeline-content">
						<image class="timeline-img" v-if="item.url" :src="item.url" mode="aspectFill"></image>
						<view class="timeline-info">
							<text class="timeline-text">{{ item.text }}</text>
							<text class="timeline-time">{{ item.time }}</text>
						</view>
					</view>
				</view>
			</view>
			<view class="load-more" v-if="loadingMore"><text>加载中...</text></view>
			<view class="no-more" v-if="noMore"><text>没有更多了</text></view>
		</scroll-view>

		<!-- 画册模式 -->
		<scroll-view v-else class="scroll-view" scroll-y @scrolltolower="loadMore">
			<view class="gallery-list">
				<view class="gallery-item" v-for="(item, index) in historyList" :key="index" @click="goDetail(item.id)">
					<view class="gallery-card">
						<image class="gallery-img" v-if="item.url" :src="item.url" mode="aspectFill"></image>
						<view class="gallery-info">
							<text class="gallery-text">{{ item.text }}</text>
						</view>
					</view>
				</view>
			</view>
			<view class="load-more" v-if="loadingMore"><text>加载中...</text></view>
			<view class="no-more" v-if="noMore"><text>没有更多了</text></view>
		</scroll-view>

		<!-- 空白状态 -->
		<view class="empty-state" v-if="historyList.length === 0 && !loadingMore">
			<image class="icon-empty" src="/static/icons/image.svg" mode="aspectFit"></image>
			<text class="empty-text">暂无历史记录</text>
		</view>
	</view>
</template>

<script>
	import { getHistoryList as fetchHistoryList } from '@/utils/api.js';

	export default {
		data() {
			return {
				viewMode: 'timeline',
				historyList: [],
				page: 1,
				pageSize: 10,
				loadingMore: false,
				noMore: false
			}
		},
		onLoad() { this.getHistoryList() },
		onReachBottom() { this.loadMore() },
		methods: {
			async getHistoryList() {
				this.page = 1;
				this.noMore = false;
				try {
					const res = await fetchHistoryList(this.page, this.pageSize);
					this.historyList = res.list || [];
					this.noMore = !res.hasMore;
				} catch (e) {
					this.historyList = [];
				}
			},
			async loadMore() {
				if (this.loadingMore || this.noMore) return
				this.loadingMore = true
				try {
					this.page += 1;
					const res = await fetchHistoryList(this.page, this.pageSize);
					const moreList = res.list || [];
					this.historyList = [...this.historyList, ...moreList];
					this.noMore = !res.hasMore;
				} catch (e) {
					this.noMore = true;
				} finally {
					this.loadingMore = false
				}
			},
			goDetail(id) { uni.navigateTo({ url: `/pages/detail/detail?id=${id}` }) },
			goIndex() { uni.switchTab({ url: '/pages/index/index' }) },
			goProfile() { uni.switchTab({ url: '/pages/profile/profile' }) }
		}
	}
</script>

<style>
	.history-container { min-height: 100vh; background-color: #FFF8F3; padding-bottom: 120rpx; }
	.scroll-view { height: calc(100vh - 100rpx - 120rpx); }
	.tab-bar { display: flex; background-color: #fff; padding: 0 30rpx; border-bottom: 1rpx solid #FFF8F3; height: 100rpx; }
	.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 30rpx; color: #666; position: relative; transition: all 0.3s; }
	.tab-item.active { color: #EFA87C; font-weight: bold; font-size: 32rpx; }
	.tab-item.active::after { content: ''; position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 40rpx; height: 6rpx; background-color: #EFA87C; border-radius: 4rpx; }
	
	/* 时间轴模式 */
	.timeline-list { padding: 40rpx 30rpx; }
	.timeline-item { position: relative; padding-left: 60rpx; margin-bottom: 50rpx; }
	.timeline-item::before { content: ''; position: absolute; top: 40rpx; bottom: -50rpx; left: 19rpx; width: 2rpx; background-color: #e0e0e0; }
	.timeline-item:last-child::before { display: none; }
	.timeline-dot { width: 24rpx; height: 24rpx; background-color: #EFA87C; border-radius: 50%; position: absolute; left: 8rpx; top: 12rpx; z-index: 2; box-shadow: 0 0 0 6rpx rgba(239, 168, 124, 0.2); }
	
	.timeline-content { background-color: #fff; border-radius: 20rpx; overflow: hidden; box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06); transition: transform 0.2s; }
	.timeline-content:active { transform: scale(0.98); }
	.timeline-img { width: 100%; height: 320rpx; display: block; }
	.timeline-info { padding: 24rpx; }
	.timeline-text { font-size: 28rpx; color: #333; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin-bottom: 12rpx; font-weight: 500; }
	.timeline-time { font-size: 24rpx; color: #999; }
	
	/* 画册模式 */
	.gallery-list { display: flex; flex-wrap: wrap; padding: 15rpx; }
	.gallery-item { width: 50%; padding: 15rpx; box-sizing: border-box; }
	.gallery-card { background-color: #fff; border-radius: 20rpx; overflow: hidden; box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06); transition: transform 0.2s; }
	.gallery-card:active { transform: scale(0.96); }
	.gallery-img { width: 100%; height: 340rpx; display: block; }
	.gallery-info { padding: 16rpx; background: linear-gradient(to bottom, rgba(255,255,255,0.9), #fff); }
	.gallery-text { font-size: 24rpx; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; display: block; }
	
	/* 其他 */
	.load-more, .no-more { text-align: center; padding: 30rpx; font-size: 24rpx; color: #ccc; }
	.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 200rpx 0; }
	.icon-empty { width: 200rpx; height: 200rpx; margin-bottom: 40rpx; opacity: 0.5; }
	.empty-text { font-size: 28rpx; color: #999; }
</style>
