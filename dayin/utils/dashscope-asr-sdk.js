import config from './config.js';

/**
 * 阿里云百炼 DashScope 实时语音识别 SDK (Mini Program Version)
 * 模拟官方 SDK 的调用方式，底层使用 WebSocket 连接 DashScope
 */
class DashScopeASR {
    constructor(options = {}) {
        this.apiKey = options.apiKey || config.aliyun.apiKey;
        this.url = 'wss://dashscope.aliyuncs.com/api-ws/v1/inference/';
        this.socketTask = null;
        this.taskId = null;
        this.eventHandlers = {
            started: [],
            changed: [],
            completed: [],
            failed: [],
            closed: []
        };
        this.status = 'closed'; // closed, connecting, open, finishing
        this.audioBuffer = []; // 缓存未发送的音频数据
    }

    /**
     * 注册事件回调
     * @param {string} event - 事件名: started, changed, completed, failed, closed
     * @param {function} handler - 回调函数
     */
    on(event, handler) {
        if (this.eventHandlers[event]) {
            this.eventHandlers[event].push(handler);
        }
    }

    /**
     * 触发事件
     */
    emit(event, data) {
        if (this.eventHandlers[event]) {
            this.eventHandlers[event].forEach(handler => handler(data));
        }
    }

    /**
     * 生成UUID
     */
    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    /**
     * 开始识别
     * @param {object} params - 识别参数
     */
    start(params = {}) {
        if (this.status !== 'closed') {
            console.warn('ASR session already active');
            return;
        }
        
        this.status = 'connecting';
        this.taskId = this.generateUUID();
        this.audioBuffer = []; // 清空缓存
        this.lastText = ''; // 清空上一次结果

        try {
            this.socketTask = uni.connectSocket({
                url: this.url,
                header: {
                    'Authorization': `Bearer ${this.apiKey}`
                },
                success: () => {
                    console.log('WebSocket connection initiated');
                },
                fail: (err) => {
                    console.error('WebSocket connection failed', err);
                    this.emit('failed', { error_message: 'Connection failed', detail: err });
                    this.status = 'closed';
                }
            });

            this.socketTask.onOpen(() => {
                console.log('WebSocket Open');
                this.status = 'open';
                this.emit('started', { task_id: this.taskId });
                
                // 发送 run-task 指令
                const startFrame = {
                    header: {
                        task_id: this.taskId,
                        action: 'run-task',
                        streaming: 'duplex'
                    },
                    payload: {
                        task_group: 'audio',
                        task: 'asr',
                        function: 'recognition',
                        model: 'paraformer-realtime-v1',
                        parameters: {
                            format: params.format || 'pcm',
                            sample_rate: params.sample_rate || 16000,
                            language_hints: ['zh'],
                            enable_intermediate_result: true, // 开启中间结果
                            enable_punctuation_prediction: true, // 开启标点预测
                            enable_inverse_text_normalization: true // 开启ITN
                        },
                        input: {} // 必须包含 input
                    }
                };
                this.socketTask.send({ data: JSON.stringify(startFrame) });
                
                // 发送缓存的音频数据
                if (this.audioBuffer.length > 0) {
                    console.log(`Sending buffered audio: ${this.audioBuffer.length} chunks`);
                    this.audioBuffer.forEach(chunk => {
                        this.socketTask.send({ data: chunk });
                    });
                    this.audioBuffer = [];
                }
            });

            this.socketTask.onMessage((res) => {
                try {
                    const msg = JSON.parse(res.data);
                    console.log('WebSocket Message Event:', msg.header.event); // Enable logging

                    if (msg.header.event === 'task-finished') {
                        console.log('Task Finished, Final Text:', this.lastText || '(empty)');
                        // 确保在任务完成时关闭连接
                        this.closeSocket(); 
                    } else if (msg.header.event === 'task-failed') {
                        console.error('Task Failed:', msg.header.error_message);
                        this.emit('failed', { error_message: msg.header.error_message });
                        this.closeSocket();
                    } else if (msg.header.event === 'result-generated') {
                        // 实时结果
                        if (msg.payload && msg.payload.output && msg.payload.output.sentence) {
                            const text = msg.payload.output.sentence.text;
                            // 触发 changed 事件（中间结果）
                            this.emit('changed', text);
                            this.lastText = text;
                        }
                    } else {
                        // Log other events
                        console.log('Unhandled Event:', msg.header.event, msg);
                    }
                } catch (e) {
                    console.error('Parse message failed', e);
                }
            });

            this.socketTask.onError((err) => {
                console.error('WebSocket Error', err);
                this.emit('failed', { error_message: 'WebSocket Error', detail: err });
                this.closeSocket();
            });
        } catch (e) {
            console.error('Start ASR failed', e);
            this.emit('failed', { error_message: 'Exception in start', detail: e });
            this.status = 'closed';
        }
    }

    /**
     * 发送音频数据
     * @param {ArrayBuffer} buffer 
     */
    sendAudio(buffer) {
        if (this.socketTask && this.status === 'open') {
            this.socketTask.send({
                data: buffer,
                fail: (err) => {
                    console.error('Send audio failed', err);
                }
            });
        } else if (this.status === 'connecting') {
            // 如果正在连接中，缓存数据
            this.audioBuffer.push(buffer);
            // 限制缓存大小，防止内存溢出 (例如最多缓存100个包，约12秒)
            if (this.audioBuffer.length > 100) {
                this.audioBuffer.shift();
            }
        }
    }

    /**
     * 停止识别 (发送 finish-task)
     */
    stop() {
        if (this.socketTask && this.status === 'open') {
            this.status = 'finishing';
            const finishFrame = {
                header: {
                    task_id: this.taskId,
                    action: 'finish-task',
                    streaming: 'duplex'
                },
                payload: {
                    input: {}
                }
            };
            this.socketTask.send({
                data: JSON.stringify(finishFrame),
                success: () => {
                    console.log('Sent finish-task');
                }
            });
            // 监听 task-finished 来触发 completed
        }
    }

    /**
     * 强制关闭
     */
    closeSocket() {
        if (this.socketTask) {
            this.socketTask.close();
            this.socketTask = null;
        }
        this.status = 'closed';
        // 触发 completed 事件，传递最后识别的文本
        if (this.lastText) {
            this.emit('completed', this.lastText);
            this.lastText = '';
        }
    }
}

export default DashScopeASR;
