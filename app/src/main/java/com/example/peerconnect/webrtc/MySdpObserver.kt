package com.example.peerconnect.webrtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class MySdpObserver: SdpObserver {
    override fun onCreateSuccess(p0: SessionDescription?) {
    }

    override fun onSetSuccess() {
    }

    override fun onCreateFailure(p0: String?) {
    }

    override fun onSetFailure(p0: String?) {
    }
}