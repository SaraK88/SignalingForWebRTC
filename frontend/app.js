// Agora WebRTC Video Call Application
class AgoraVideoCall {
    constructor() {
        // Check if Agora SDKs are available
        if (typeof AgoraRTC === 'undefined' || typeof AgoraRTM === 'undefined') {
            throw new Error('Agora SDKs are not loaded. Please check your internet connection.');
        }
        
        this.appId = 'eafd4a0f87c44b4590452eb833466899';
        this.backendUrl = 'http://localhost:8080';
        
        this.rtmClient = null;
        this.rtmChannel = null;
        this.rtcClient = null;
        this.localAudioTrack = null;
        this.localVideoTrack = null;
        this.remoteUsers = {};
        
        this.username = '';
        this.roomName = '';
        this.uid = '';
        
        this.isMuted = false;
        this.isVideoOff = false;
        this.isScreenSharing = false;
        
        this.log('Agora Video Call App initialized');
    }
    
    // Generate unique user ID
    generateUid() {
        return Math.random().toString(36).substr(2, 9);
    }
    
    // Logging function
    log(message, type = 'info') {
        const timestamp = new Date().toLocaleTimeString();
        const logElement = document.getElementById('logs');
        const logEntry = document.createElement('div');
        logEntry.className = `log-entry ${type}`;
        logEntry.textContent = `[${timestamp}] ${message}`;
        logElement.appendChild(logEntry);
        logElement.scrollTop = logElement.scrollHeight;
        console.log(`[${timestamp}] ${message}`);
    }
    
    // Update connection status
    updateStatus(status, message) {
        const statusElement = document.getElementById('connection-status');
        statusElement.textContent = message;
        statusElement.className = `status ${status}`;
    }
    
    // Get tokens from backend
    async getTokens(uid, channelName) {
        try {
            this.log('Getting tokens from backend...');
            
            // Get RTM token
            const rtmResponse = await fetch(`${this.backendUrl}/rtm-token?uid=${uid}`);
            const rtmData = await rtmResponse.json();
            
            // Get Media token
            const mediaResponse = await fetch(`${this.backendUrl}/media-token?channelName=${channelName}&uid=${uid}`);
            const mediaData = await mediaResponse.json();
            
            this.log('Tokens received successfully');
            return {
                rtmToken: rtmData.token,
                mediaToken: mediaData.token
            };
        } catch (error) {
            this.log(`Failed to get tokens: ${error.message}`, 'error');
            throw error;
        }
    }
    
    // Initialize RTM client
    async initializeRTM(token) {
        try {
            this.log('Initializing RTM client...');
            this.rtmClient = AgoraRTM.createInstance(this.appId);
            
            await new Promise((resolve, reject) => {
                this.rtmClient.login({ token, uid: this.uid })
                    .then(() => {
                        this.log('RTM login successful');
                        resolve();
                    })
                    .catch(reject);
            });
            
            // Listen for connection state changes
            this.rtmClient.on('ConnectionStateChanged', (state, reason) => {
                this.log(`RTM connection state: ${state}, reason: ${reason}`);
                if (state === 'CONNECTED') {
                    this.updateStatus('connected', '🟢 Connected');
                } else {
                    this.updateStatus('disconnected', '🔴 Disconnected');
                }
            });
            
            // Listen for messages from remote users
            this.rtmClient.on('MessageFromPeer', (message, peerId) => {
                this.handlePeerMessage(message, peerId);
            });
            
        } catch (error) {
            this.log(`RTM initialization failed: ${error.message}`, 'error');
            throw error;
        }
    }
    
    // Join RTM channel
    async joinRTMChannel() {
        try {
            this.log(`Joining RTM channel: ${this.roomName}`);
            this.rtmChannel = this.rtmClient.createChannel(this.roomName);
            
            await new Promise((resolve, reject) => {
                this.rtmChannel.join()
                    .then(() => {
                        this.log('Joined RTM channel successfully');
                        resolve();
                    })
                    .catch(reject);
            });
            
            // Listen for channel messages
            this.rtmChannel.on('ChannelMessage', (message, memberId) => {
                this.handleChannelMessage(message, memberId);
            });
            
            // Listen for member joined
            this.rtmChannel.on('MemberJoined', (memberId) => {
                this.log(`User joined: ${memberId}`);
                this.addMessage(`${memberId} joined the room`, 'system');
            });
            
            // Listen for member left
            this.rtmChannel.on('MemberLeft', (memberId) => {
                this.log(`User left: ${memberId}`);
                this.addMessage(`${memberId} left the room`, 'system');
                this.handleUserLeft(memberId);
            });
            
        } catch (error) {
            this.log(`Failed to join RTM channel: ${error.message}`, 'error');
            throw error;
        }
    }
    
    // Initialize RTC client
    async initializeRTC(token) {
        try {
            this.log('Initializing RTC client...');
            this.rtcClient = AgoraRTC.createClient({ mode: 'rtc', codec: 'vp8' });
            
            // Listen for remote users joining
            this.rtcClient.on('user-published', async (user, mediaType) => {
                this.log(`Remote user published: ${user.uid}, media: ${mediaType}`);
                await this.subscribe(user, mediaType);
            });
            
            // Listen for remote users leaving
            this.rtcClient.on('user-unpublished', (user, mediaType) => {
                this.log(`Remote user unpublished: ${user.uid}, media: ${mediaType}`);
                if (mediaType === 'video') {
                    document.getElementById('remote-video').innerHTML = '';
                }
            });
            
            // Join RTC channel
            await this.rtcClient.join(this.appId, token, this.roomName, this.uid);
            this.log('Joined RTC channel successfully');
            
        } catch (error) {
            this.log(`RTC initialization failed: ${error.message}`, 'error');
            throw error;
        }
    }
    
    // Subscribe to remote user
    async subscribe(user, mediaType) {
        try {
            await this.rtcClient.subscribe(user, mediaType);
            
            if (mediaType === 'video') {
                const remoteVideoContainer = document.getElementById('remote-video');
                remoteVideoContainer.innerHTML = '';
                user.videoTrack.play(remoteVideoContainer);
                document.getElementById('remote-info').textContent = `Remote user: ${user.uid}`;
            }
            
            if (mediaType === 'audio') {
                user.audioTrack.play();
            }
            
            this.remoteUsers[user.uid] = user;
            this.log(`Subscribed to ${user.uid}'s ${mediaType}`);
            
        } catch (error) {
            this.log(`Failed to subscribe to ${user.uid}: ${error.message}`, 'error');
        }
    }
    
    // Create and publish local tracks
    async createAndPublishTracks() {
        try {
            this.log('Creating local tracks...');
            
            // Create tracks
            [this.localAudioTrack, this.localVideoTrack] = await AgoraRTC.createMicrophoneAndCameraTracks();
            
            // Publish tracks
            await this.rtcClient.publish([this.localAudioTrack, this.localVideoTrack]);
            
            // Play local video
            const localVideoContainer = document.getElementById('local-video');
            localVideoContainer.innerHTML = '';
            this.localVideoTrack.play(localVideoContainer);
            
            this.log('Local tracks created and published');
            
        } catch (error) {
            this.log(`Failed to create tracks: ${error.message}`, 'error');
            throw error;
        }
    }
    
    // Handle peer messages
    handlePeerMessage(message, peerId) {
        const text = message.getText();
        this.log(`Received peer message from ${peerId}: ${text}`);
        
        try {
            const data = JSON.parse(text);
            
            // Handle different message types
            switch (data.type) {
                case 'offer':
                case 'answer':
                case 'ice-candidate':
                    // Handle WebRTC signaling
                    this.handleSignalingMessage(data, peerId);
                    break;
                default:
                    this.addMessage(`${peerId}: ${data.message}`, 'remote');
            }
        } catch (error) {
            // If it's not JSON, treat as plain text
            this.addMessage(`${peerId}: ${text}`, 'remote');
        }
    }
    
    // Handle channel messages
    handleChannelMessage(message, memberId) {
        const text = message.getText();
        this.log(`Received channel message from ${memberId}: ${text}`);
        
        try {
            const data = JSON.parse(text);
            if (data.type === 'chat') {
                this.addMessage(`${memberId}: ${data.message}`, 'remote');
            }
        } catch (error) {
            this.addMessage(`${memberId}: ${text}`, 'remote');
        }
    }
    
    // Handle user leaving
    handleUserLeft(userId) {
        if (this.remoteUsers[userId]) {
            delete this.remoteUsers[userId];
            document.getElementById('remote-video').innerHTML = '';
            document.getElementById('remote-info').textContent = 'Waiting for remote user...';
        }
    }
    
    // Add message to chat
    addMessage(message, type = 'remote') {
        const messageList = document.getElementById('message-list');
        const messageElement = document.createElement('div');
        messageElement.className = `message ${type === 'own' ? 'own' : ''}`;
        messageElement.textContent = message;
        messageList.appendChild(messageElement);
        messageList.scrollTop = messageList.scrollHeight;
    }
    
    // Send message
    async sendMessage() {
        const input = document.getElementById('message-input');
        const message = input.value.trim();
        
        if (!message || !this.rtmChannel) return;
        
        try {
            const messageData = {
                type: 'chat',
                message: message,
                sender: this.username
            };
            
            await this.rtmChannel.sendMessage({ text: JSON.stringify(messageData) });
            this.addMessage(`${this.username}: ${message}`, 'own');
            input.value = '';
            
        } catch (error) {
            this.log(`Failed to send message: ${error.message}`, 'error');
        }
    }
    
    // Toggle mute
    toggleMute() {
        if (!this.localAudioTrack) return;
        
        this.isMuted = !this.isMuted;
        this.localAudioTrack.setMuted(this.isMuted);
        
        const muteBtn = document.getElementById('mute-btn');
        muteBtn.textContent = this.isMuted ? '🔇 Unmute' : '🔊 Mute';
        
        this.log(`Microphone ${this.isMuted ? 'muted' : 'unmuted'}`);
    }
    
    // Toggle video
    toggleVideo() {
        if (!this.localVideoTrack) return;
        
        this.isVideoOff = !this.isVideoOff;
        this.localVideoTrack.setEnabled(!this.isVideoOff);
        
        const videoBtn = document.getElementById('video-btn');
        videoBtn.textContent = this.isVideoOff ? '📹 Video Off' : '📹 Video On';
        
        this.log(`Camera ${this.isVideoOff ? 'off' : 'on'}`);
    }
    
    // Leave room
    async leaveRoom() {
        try {
            this.log('Leaving room...');
            
            // Leave RTC channel
            if (this.rtcClient) {
                await this.rtcClient.leave();
                this.rtcClient = null;
            }
            
            // Leave RTM channel
            if (this.rtmChannel) {
                await this.rtmChannel.leave();
                this.rtmChannel = null;
            }
            
            // Logout RTM
            if (this.rtmClient) {
                await this.rtmClient.logout();
                this.rtmClient = null;
            }
            
            // Clean up tracks
            if (this.localAudioTrack) {
                this.localAudioTrack.close();
                this.localAudioTrack = null;
            }
            
            if (this.localVideoTrack) {
                this.localVideoTrack.close();
                this.localVideoTrack = null;
            }
            
            // Clear UI
            document.getElementById('local-video').innerHTML = '';
            document.getElementById('remote-video').innerHTML = '';
            document.getElementById('remote-info').textContent = 'Waiting for remote user...';
            document.getElementById('message-list').innerHTML = '';
            
            // Show join section, hide video section
            document.getElementById('join-section').style.display = 'block';
            document.getElementById('video-section').style.display = 'none';
            
            this.updateStatus('disconnected', '🔴 Not Connected');
            this.log('Left room successfully');
            
        } catch (error) {
            this.log(`Error leaving room: ${error.message}`, 'error');
        }
    }
}

// Global functions for HTML onclick handlers
let app = null;

async function joinRoom() {
    try {
        const username = document.getElementById('username').value.trim();
        const roomName = document.getElementById('room-name').value.trim();
        
        if (!username || !roomName) {
            alert('Please enter both username and room name');
            return;
        }
        
        // Initialize app
        if (!app) {
            app = new AgoraVideoCall();
        }
        
        app.username = username;
        app.roomName = roomName;
        app.uid = app.generateUid();
        
        app.updateStatus('connecting', '🟡 Connecting...');
        
        // Get tokens
        const tokens = await app.getTokens(app.uid, roomName);
        
        // Initialize RTM
        await app.initializeRTM(tokens.rtmToken);
        await app.joinRTMChannel();
        
        // Initialize RTC
        await app.initializeRTC(tokens.mediaToken);
        await app.createAndPublishTracks();
        
        // Update UI
        document.getElementById('join-section').style.display = 'none';
        document.getElementById('video-section').style.display = 'block';
        
        app.addMessage(`Welcome to ${roomName}!`, 'system');
        app.log('Successfully joined room');
        
    } catch (error) {
        alert(`Failed to join room: ${error.message}`);
        console.error(error);
    }
}

function toggleMute() {
    if (app) app.toggleMute();
}

function toggleVideo() {
    if (app) app.toggleVideo();
}

function toggleScreenShare() {
    if (app) {
        app.log('Screen sharing not implemented yet', 'warning');
    }
}

function leaveRoom() {
    if (app) app.leaveRoom();
}

// Handle Enter key in message input
document.addEventListener('DOMContentLoaded', function() {
    const messageInput = document.getElementById('message-input');
    if (messageInput) {
        messageInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                sendMessage();
            }
        });
    }
});
