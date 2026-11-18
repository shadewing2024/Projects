/*  This is a Kotlin class of Ai Model*/

package com.smartaqi.myapplication

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class AiModel(context: Context) {

    private var interpreter: Interpreter? = null

    init {
        try {
            // Load the model from assets correctly
            val fileDescriptor = context.assets.openFd("aqi_model.tflite")
            val inputStream = fileDescriptor.createInputStream()
            val fileChannel = inputStream.channel
            val modelBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength
            )
            interpreter = Interpreter(modelBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun predict(input: FloatArray): FloatArray {
        val interpreter = this.interpreter ?: return floatArrayOf()

        // Prepare input buffer (8 floats * 4 bytes each)
        val inputBuffer = ByteBuffer.allocateDirect(input.size * 4).apply {
            order(ByteOrder.nativeOrder())
            for (value in input) {
                putFloat(value)
            }
        }

        // Prepare output buffer (1 float * 4 bytes)
        val outputBuffer = ByteBuffer.allocateDirect(4).apply {
            order(ByteOrder.nativeOrder())
        }

        // Run inference
        interpreter.run(inputBuffer, outputBuffer)

        // Read result
        outputBuffer.rewind()
        return floatArrayOf(outputBuffer.float)
    }

    fun close() {
        interpreter?.close()
    }
}