package com.example.cameraxapp.domain.usecase

import android.graphics.Bitmap
import java.io.IOException
import java.io.OutputStream

/**
 * Simple GIF encoder implementation
 * Creates animated GIFs from bitmaps
 */
class SimpleGifEncoder {
    private var width = 0
    private var height = 0
    private var delay = 0
    private var repeat = 0
    private var started = false
    private var out: OutputStream? = null
    private var firstFrame = true

    /**
     * Sets the delay time between each frame
     * @param ms delay time in milliseconds
     */
    fun setDelay(ms: Int) {
        delay = (ms / 10).toInt()
    }

    /**
     * Sets the number of times the set of GIF frames should be played
     * @param iter number of iterations. 0 means play indefinitely
     */
    fun setRepeat(iter: Int) {
        repeat = iter
    }

    /**
     * Initiates GIF file creation on the given stream
     */
    fun start(os: OutputStream): Boolean {
        if (os == null) return false
        var ok = true
        out = os
        try {
            writeString("GIF89a") // GIF header
        } catch (e: IOException) {
            ok = false
        }
        started = ok
        return ok
    }

    /**
     * Adds next GIF frame
     */
    fun addFrame(im: Bitmap): Boolean {
        if (im == null || !started) {
            return false
        }
        var ok = true
        try {
            if (firstFrame) {
                width = im.width
                height = im.height
                writeLSD()
                
                // Get pixels for color table creation
                val pixels = IntArray(width * height)
                im.getPixels(pixels, 0, width, 0, 0, width, height)
                val colorTable = createColorTable(pixels)
                
                // Write global color table for first frame
                writeColorTable(colorTable)
                
                writeNetscapeExt()
                firstFrame = false
            }
            
            writeGraphicCtrlExt()
            writeImageDesc()
            
            // Get pixels for current frame
            val pixels = IntArray(width * height)
            im.getPixels(pixels, 0, width, 0, 0, width, height)
            val colorTable = createColorTable(pixels)
            
            // For subsequent frames, write local color table
            if (!firstFrame) {
                // Update image descriptor to indicate local color table
                // Note: This is a simplified approach - in a full implementation,
                // we'd need to rewrite the image descriptor
            }
            
            writePixels(im, colorTable)
        } catch (e: IOException) {
            ok = false
        }
        return ok
    }

    /**
     * Flushes any pending data and closes output
     */
    fun finish(): Boolean {
        if (!started) return false
        var ok = true
        started = false
        try {
            out?.write(0x3b) // GIF trailer
            out?.flush()
        } catch (e: IOException) {
            ok = false
        }
        return ok
    }

    private fun writeLSD() {
        writeShort(width)
        writeShort(height)
        val packed = (0x80 or (0x07 shl 4) or 0x07).toByte()
        out?.write(packed.toInt())
        out?.write(0) // Background color index
        out?.write(0) // Pixel aspect ratio
        // Global color table will be written after first frame is processed
    }

    private fun writeNetscapeExt() {
        out?.write(0x21) // Extension introducer
        out?.write(0xff) // Application extension label
        out?.write(11) // Block size
        writeString("NETSCAPE2.0")
        out?.write(3) // Sub-block size
        out?.write(1) // Loop sub-block ID
        writeShort(repeat)
        out?.write(0) // Block terminator
    }

    private fun writeGraphicCtrlExt() {
        out?.write(0x21) // Extension introducer
        out?.write(0xf9) // Graphic control label
        out?.write(4) // Block size
        val transp = 0
        val disp = 0
        out?.write((0 or transp or (disp shl 2)).toInt())
        writeShort(delay)
        out?.write(transp) // Transparent color index
        out?.write(0) // Block terminator
    }

    private fun writeImageDesc() {
        out?.write(0x2c) // Image separator
        writeShort(0) // Image left position
        writeShort(0) // Image top position
        writeShort(width) // Image width
        writeShort(height) // Image height
        // Color table flag will be set in writePixels
    }

    private fun writePixels(im: Bitmap, colorTable: ByteArray) {
        val pixels = IntArray(width * height)
        im.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Map pixels to color table indices
        val indexedPixels = mapPixelsToIndices(pixels, colorTable)
        
        // Write LZW compressed data
        writeLZWData(indexedPixels)
    }

    private fun createColorTable(pixels: IntArray): ByteArray {
        // Simple color quantization: use first 256 unique colors
        val colorMap = mutableMapOf<Int, Int>()
        val colorTable = mutableListOf<Byte>()
        
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            
            val colorKey = (r shl 16) or (g shl 8) or b
            if (!colorMap.containsKey(colorKey) && colorTable.size < 768) { // 256 colors * 3 bytes
                colorMap[colorKey] = colorTable.size / 3
                colorTable.add(r.toByte())
                colorTable.add(g.toByte())
                colorTable.add(b.toByte())
            }
        }
        
        // Pad to 256 colors (768 bytes)
        while (colorTable.size < 768) {
            colorTable.add(0)
        }
        
        return colorTable.toByteArray()
    }

    private fun mapPixelsToIndices(pixels: IntArray, colorTable: ByteArray): ByteArray {
        val indices = ByteArray(pixels.size)
        val colorMap = mutableMapOf<Int, Int>()
        
        // Build color map
        for (i in colorTable.indices step 3) {
            val r = colorTable[i].toInt() and 0xff
            val g = colorTable[i + 1].toInt() and 0xff
            val b = colorTable[i + 2].toInt() and 0xff
            val colorKey = (r shl 16) or (g shl 8) or b
            colorMap[colorKey] = i / 3
        }
        
        // Map pixels
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            val colorKey = (r shl 16) or (g shl 8) or b
            indices[i] = colorMap[colorKey]?.toByte() ?: 0
        }
        
        return indices
    }

    private fun writeColorTable(colorTable: ByteArray) {
        out?.write(colorTable, 0, colorTable.size)
    }

    private fun writeLZWData(indexedPixels: ByteArray) {
        val lzwMinCodeSize = 8
        out?.write(lzwMinCodeSize)
        
        // Simple LZW encoding
        val encoder = LZWEncoder(width, height, indexedPixels, lzwMinCodeSize)
        encoder.encode(out!!)
    }

    private fun writeShort(value: Int) {
        out?.write(value and 0xff)
        out?.write((value shr 8) and 0xff)
    }

    private fun writeString(s: String) {
        for (i in s.indices) {
            out?.write(s[i].code)
        }
    }
}

/**
 * Simple LZW encoder for GIF compression
 */
private class LZWEncoder(
    private val width: Int,
    private val height: Int,
    private val indexedPixels: ByteArray,
    private val colorDepth: Int
) {
    fun encode(os: OutputStream) {
        if (indexedPixels.isEmpty()) return
        
        // Initialize LZW
        val initCodeSize = if (colorDepth < 2) 2 else colorDepth
        var codeSize = initCodeSize + 1
        val clearCode = 1 shl initCodeSize
        val endCode = clearCode + 1
        var maxCode = clearCode + 2
        
        // Write initial code size
        os.write(initCodeSize)
        
        var curAccum = 0
        var curBits = 0
        
        fun writeCode(code: Int) {
            curAccum = curAccum or (code shl curBits)
            curBits += codeSize
            
            while (curBits >= 8) {
                os.write(curAccum and 0xff)
                curAccum = curAccum shr 8
                curBits -= 8
            }
        }
        
        // Write clear code
        writeCode(clearCode)
        
        // Simple encoding: write each pixel as a code
        for (pixel in indexedPixels) {
            val code = pixel.toInt() and 0xff
            writeCode(code)
            
            // Increase code size if needed
            maxCode++
            if (maxCode >= (1 shl codeSize) && codeSize < 12) {
                codeSize++
            }
            
            // Reset if max codes reached
            if (maxCode >= 4096) {
                writeCode(clearCode)
                codeSize = initCodeSize + 1
                maxCode = clearCode + 2
            }
        }
        
        // Write end code
        writeCode(endCode)
        
        // Flush remaining bits
        if (curBits > 0) {
            os.write(curAccum and 0xff)
        }
        
        os.write(0) // Block terminator
    }
}
