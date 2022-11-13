package f.cking.software.domain.interactor

import f.cking.software.domain.model.BleRecordFrame

class GetBleRecordFramesFromRawInteractor {

    fun execute(raw: ByteArray): List<BleRecordFrame> {
        val frames = mutableListOf<BleRecordFrame>()

        var frameSize: Byte? = null
        var currentFrameByteNumber: Byte = 0

        var currentFrameType: Byte? = null
        val currentFrameDataBuffer = mutableListOf<Byte>()

        for (i in 0..raw.lastIndex) {
            val byte = raw[i]
            currentFrameByteNumber++

            val isFrameSizeByte = frameSize == null
            val isTypeByte = currentFrameByteNumber == 1.toByte()
            val isDataByte = !isFrameSizeByte && !isTypeByte
            val isLastByte = currentFrameByteNumber == frameSize

            when {
                isFrameSizeByte -> {
                    frameSize = byte
                    currentFrameByteNumber = 0
                }
                isTypeByte -> {
                    currentFrameType = byte
                }
                isDataByte -> {
                    currentFrameDataBuffer.add(byte)
                }
            }

            if (isLastByte) {
                frames.add(BleRecordFrame(type = currentFrameType!!, data = currentFrameDataBuffer.toByteArray()))
                frameSize = null
                currentFrameType = null
                currentFrameDataBuffer.clear()
            }
        }
        return frames
    }
}