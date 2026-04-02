<template>
	<view class="printer-container">
		<!-- 打印画布 - 使用 canvas-2d -->
		<canvas type="2d" canvas-id="printCanvas" id="printCanvas" style="width:400px;height:640px;position:fixed;left:0;top:0;opacity:0;pointer-events:none;"></canvas>

		<!-- 已连接设备 -->
		<view class="section" v-if="connectedDevice">
			<text class="section-title">已连接设备</text>
			<view class="device-card connected">
				<image class="device-icon" src="/static/icons/printer.svg" mode="aspectFit"></image>
				<view class="device-info">
					<text class="device-name">{{ connectedDevice.name }}</text>
					<text class="device-status">已连接</text>
				</view>
				<button class="disconnect-btn" @click="disconnect">断开</button>
			</view>
		</view>

		<!-- 可用设备列表 -->
		<view class="section">
			<view class="section-header">
				<text class="section-title">可用设备</text>
				<view class="refresh-btn" @click="searchDevices">
					<text>刷新</text>
				</view>
			</view>

			<view class="device-list" v-if="deviceList.length > 0">
				<view class="device-card" v-for="(device, index) in deviceList" :key="index" @click="connectDevice(device)">
					<image class="device-icon" src="/static/icons/printer.svg" mode="aspectFit"></image>
					<view class="device-info">
						<text class="device-name">{{ device.name }}</text>
						<text class="device-signal">信号: {{ device.signal }}%</text>
					</view>
					<text class="connect-text">点击连接</text>
				</view>
			</view>

			<!-- 搜索中 -->
			<view class="searching" v-else-if="isSearching">
				<view class="search-spinner"></view>
				<text>正在搜索设备...</text>
			</view>

			<!-- 空白状态 -->
			<view class="empty-state" v-else>
				<image class="empty-icon" src="/static/icons/printer.svg" mode="aspectFit"></image>
				<text class="empty-text">未发现可用设备</text>
				<text class="empty-hint">请确保打印机已开启</text>
			</view>
		</view>

		<!-- 已保存设备 -->
		<view class="section" v-if="savedDevices.length > 0">
			<text class="section-title">已保存设备</text>
			<view class="device-list">
				<view class="device-card" v-for="(device, index) in savedDevices" :key="index" @click="quickConnect(device)">
					<image class="device-icon" src="/static/icons/printer.svg" mode="aspectFit"></image>
					<view class="device-info">
						<text class="device-name">{{ device.name }}</text>
						<text class="device-status">已保存</text>
					</view>
					<text class="connect-text">快速连接</text>
				</view>
			</view>
		</view>

		<!-- 打印按钮 -->
		<view class="print-section" v-if="connectedDevice && printImage">
			<button class="print-btn" @click="startPrint">开始打印</button>
		</view>
	</view>
</template>

<script>
	import { usePrint } from '@/utils/print.js';
	import { recordPrint } from '@/utils/api.js';

	export default {
		data() {
			return {
				connectedDevice: null,
				deviceList: [],
				savedDevices: [],
				isSearching: false,
				printImage: null,
				printImageId: null,
				printModule: null
			}
		},
		onLoad(options) {
			console.log('printer onLoad options:', options);
			if (options.image) this.printImage = decodeURIComponent(options.image);
			if (options.id) this.printImageId = options.id;
			console.log('printImageId 设置为:', this.printImageId);
			this.getSavedDevices()

			// 初始化打印 SDK
			this.printModule = usePrint();
			// 设置平台为微信小程序
			this.printModule.setPlatform('微信');
			// uniapp需要设置此项
			this.printModule.setBuildPlatform('uniapp');
		},
		onShow() {
			// 临时注释掉自动重连，避免卡住
			// if (this.printModule) {
			// 	const connDevice = this.printModule.getConnName();
			// 	if (connDevice) {
			// 		this.connectedDevice = connDevice;
			// 	}
			// }
		},
		methods: {
			searchDevices() {
				if (this.isSearching) return;
				this.isSearching = true;
				this.deviceList = [];
				
				uni.showLoading({ title: '搜索蓝牙设备...' });
				
				// 调用 SDK 搜索蓝牙打印机
				this.printModule.scanedPrinters((printers) => {
					uni.hideLoading();
					this.isSearching = false;
					
					// 过滤只保留包含 B1 PRO 或 B1_PRO 的设备
					printers = printers.filter((val) => {
						if (!val.name) return false;
						const upperName = val.name.toUpperCase();
						return upperName.includes("B1 PRO") || upperName.includes("B1_PRO");
					});
					
					// 去重
					this.deviceList = printers.reduce((res, item) => {
						if (!res.some((val) => val.deviceId === item.deviceId)) {
							res.push(item);
						}
						return res;
					}, []);
					
					if (this.deviceList.length === 0) {
						uni.showToast({ title: '未找到打印机', icon: 'none' });
					}
				});
			},
			connectDevice(device) {
				uni.showLoading({ title: '连接打印机...' })
				
				// 调用 SDK 连接打印机
				this.printModule.openPrinter(
					device.name,
					() => {
						uni.hideLoading();
						uni.showToast({ title: '连接成功', icon: 'success' });
						this.connectedDevice = device;
						
						// 保存到历史记录
						if (!this.savedDevices.some(d => d.deviceId === device.deviceId)) {
							this.savedDevices.push(device);
							uni.setStorageSync('savedDevices', this.savedDevices);
						}
					},
					() => {
						uni.hideLoading();
						uni.showToast({ title: '连接失败', icon: 'none' });
					}
				);
			},
			disconnect() { 
				if (this.printModule) {
					this.printModule.closePrinter();
				}
				this.connectedDevice = null; 
				uni.showToast({ title: '已断开连接', icon: 'none' });
			},
			quickConnect(device) { 
				this.connectDevice(device) 
			},
			getSavedDevices() { 
				const saved = uni.getStorageSync('savedDevices'); 
				if (saved) this.savedDevices = saved 
			},
		async startPrint() {
				console.log('========== 打印流程开始 ==========');

				if (!this.connectedDevice) {
					uni.showToast({ title: '请先连接打印机', icon: 'none' });
					return;
				}
				if (!this.printImage) {
					uni.showToast({ title: '没有可打印的图片', icon: 'none' });
					return;
				}

				uni.showLoading({ title: '准备打印...' });

				try {
					console.log('步骤1: 下载图片...');
					console.log('图片URL:', this.printImage);
					const tempFilePath = await this.downloadImage(this.printImage);
					console.log('下载成功:', tempFilePath);

					const canvasWidth = 500;
					const canvasHeight = 900;
					console.log('画布尺寸:', canvasWidth, 'x', canvasHeight, '像素');

					console.log('步骤2: 获取 canvas-2d 节点...');
					const canvasNode = await new Promise((resolve, reject) => {
						uni.createSelectorQuery()
							.in(this)
							.select('#printCanvas')
							.fields({ node: true, size: true })
							.exec((res) => {
								if (res && res[0] && res[0].node) {
									resolve(res[0].node);
								} else {
									reject(new Error('获取canvas节点失败'));
								}
							});
					});
					console.log('canvas节点获取成功');

					canvasNode.width = canvasWidth;
					canvasNode.height = canvasHeight;
					const ctx = canvasNode.getContext('2d');

					console.log('步骤3: 绘制白色背景...');
					ctx.fillStyle = '#FFFFFF';
					ctx.fillRect(0, 0, canvasWidth, canvasHeight);

					console.log('步骤4: 加载并绘制图片...');
					const img = canvasNode.createImage();
					await new Promise((resolve, reject) => {
						img.onload = resolve;
						img.onerror = reject;
						img.src = tempFilePath;
					});

					console.log('原始图片尺寸:', img.width, 'x', img.height);
					console.log('拉伸填满画布:', canvasWidth, 'x', canvasHeight);
					ctx.drawImage(img, 0, 0, canvasWidth, canvasHeight);
					console.log('图片绘制完成');

					console.log('步骤5: 获取 canvas 像素数据...');
					const imageData = ctx.getImageData(0, 0, canvasWidth, canvasHeight);
					console.log('像素数据大小:', imageData.data.length);
					
					let nonWhitePixels = 0;
					for (let i = 0; i < imageData.data.length; i += 4) {
						if (imageData.data[i] < 250 || imageData.data[i+1] < 250 || imageData.data[i+2] < 250) {
							nonWhitePixels++;
						}
					}
					console.log('非白色像素数量:', nonWhitePixels);

					console.log('步骤6: 创建打印任务 (间隙纸, 50mm x 80mm)...');
					this.printModule.startJob(1, 8, 1, () => {
						console.log('startJob 回调');

						console.log('步骤7: 使用 printImageData 打印...');
						this.printModule.printImageData(1, canvasWidth, canvasHeight, imageData.data, () => {
							console.log('printImageData 回调');
						});

						uni.hideLoading();
						uni.showToast({ title: '打印完成', icon: 'success' });
						console.log('========== 打印流程完成 ==========');
						if (this.printImageId) {
							console.log('调用 recordPrint...');
							recordPrint(this.printImageId).then(() => {
								console.log('recordPrint 成功');
							}).catch(e => {
								console.error('记录打印失败', e);
							});
						}
					});

				} catch (err) {
					uni.hideLoading();
					console.error('打印流程异常:', err);
					uni.showToast({ title: '打印失败: ' + err.message, icon: 'none' });
				}
			},
			downloadImage(url) {
				return new Promise((resolve, reject) => {
					uni.downloadFile({
						url: url,
						timeout: 10000, // 10秒超时
						success: (res) => {
							if (res.statusCode === 200) {
								resolve(res.tempFilePath);
							} else {
								reject(new Error('下载失败, statusCode: ' + res.statusCode));
							}
						},
						fail: (err) => {
							console.error('下载图片失败:', err);
							reject(new Error('下载图片失败'));
						}
					});
				});
			}
		}
	}
</script>

<style>
	.printer-container { min-height: 100vh; background-color: #FFF8F3; padding: 30rpx; }
	.section { margin-bottom: 40rpx; }
	.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
	.section-title { font-size: 30rpx; color: #333; font-weight: bold; display: block; margin-bottom: 20rpx; }
	.section-header .section-title { margin-bottom: 0; }
	.refresh-btn { display: flex; align-items: center; color: #EFA87C; font-size: 26rpx; }
	.device-card { background-color: #fff; border-radius: 16rpx; padding: 30rpx; display: flex; align-items: center; margin-bottom: 20rpx; box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.03); }
	.device-card.connected { border: 2rpx solid #EFA87C; }
	.device-icon { width: 80rpx; height: 80rpx; margin-right: 20rpx; }
	.device-info { flex: 1; }
	.device-name { font-size: 28rpx; color: #333; display: block; margin-bottom: 8rpx; }
	.device-status { font-size: 24rpx; color: #EFA87C; }
	.device-signal { font-size: 24rpx; color: #999; }
	.disconnect-btn { padding: 12rpx 30rpx; background-color: #FFF8F3; color: #EFA87C; border-radius: 30rpx; font-size: 26rpx; border: none; }
	.disconnect-btn::after { border: none; }
	.connect-text { font-size: 26rpx; color: #EFA87C; }
	.searching { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80rpx 0; color: #999; }
	.search-spinner { width: 60rpx; height: 60rpx; border: 4rpx solid #e8e8e8; border-top-color: #EFA87C; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 20rpx; }
	@keyframes spin { to { transform: rotate(360deg); } }
	.empty-state { display: flex; flex-direction: column; align-items: center; padding: 80rpx 0; }
	.empty-icon { width: 160rpx; height: 160rpx; margin-bottom: 20rpx; opacity: 0.3; }
	.empty-text { font-size: 28rpx; color: #666; margin-bottom: 10rpx; }
	.empty-hint { font-size: 24rpx; color: #999; }
	.print-section { position: fixed; bottom: 0; left: 0; right: 0; padding: 30rpx; padding-bottom: calc(30rpx + env(safe-area-inset-bottom)); background-color: #fff; box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05); }
	.print-btn { width: 100%; height: 96rpx; background: linear-gradient(135deg, #EFA87C 0%, #FCE089 100%); color: #fff; border-radius: 48rpx; font-size: 32rpx; display: flex; align-items: center; justify-content: center; border: none; box-shadow: 0 10rpx 30rpx rgba(239, 168, 124, 0.4); }
	.print-btn::after { border: none; }
</style>
