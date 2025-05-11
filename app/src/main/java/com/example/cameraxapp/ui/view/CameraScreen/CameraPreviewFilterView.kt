package com.example.cameraxapp.ui.view.CameraScreen

import android.content.Context
import android.opengl.GLSurfaceView

class CameraPreviewFilterView(context: Context) : GLSurfaceView(context) {
    private val renderer: CameraRenderer

    init {
        // Khởi tạo OpenGL ES 2.0
        setEGLContextClientVersion(2)
        renderer = CameraRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY // Chỉ render khi cần
    }

    fun setColorFilter(filter: FloatArray) {
        renderer.setColorFilter(filter)
        requestRender()
    }
}
