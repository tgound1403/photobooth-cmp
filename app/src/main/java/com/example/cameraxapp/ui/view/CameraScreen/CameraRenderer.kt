package com.example.cameraxapp.ui.view.CameraScreen

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.opengles.GL10
import kotlin.properties.Delegates

class CameraRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private var textureId by Delegates.notNull<Int>()
    private var program: Int = 0
    private var colorFilterLocation: Int = 0

    private val vertexShaderCode = """
        attribute vec4 vPosition;
        attribute vec2 vTexCoord;
        varying vec2 texCoord;
        void main() {
            gl_Position = vPosition;
            texCoord = vTexCoord;
        }
    """

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec2 texCoord;
        uniform sampler2D texture;
        uniform vec4 colorFilter;
        void main() {
            vec4 originalColor = texture2D(texture, texCoord);
            gl_FragColor = vec4(originalColor.rgb * colorFilter.rgb, originalColor.a);
        }
    """

    private var colorFilter = floatArrayOf(1f, 1f, 1f, 1f) // Bộ lọc mặc định (không thay đổi màu)

    override fun onSurfaceCreated(
        glUnused: GL10?,
        config: javax.microedition.khronos.egl.EGLConfig?
    ) {
        // Tạo shader
        program = createProgram(vertexShaderCode, fragmentShaderCode)
        GLES20.glUseProgram(program)

        // Lấy vị trí bộ lọc màu trong shader
        colorFilterLocation = GLES20.glGetUniformLocation(program, "colorFilter")
    }

    override fun onDrawFrame(glUnused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Cập nhật filter màu
        GLES20.glUniform4fv(colorFilterLocation, 1, colorFilter, 0)

        // Render texture
        // TODO: Render camera preview
    }

    override fun onSurfaceChanged(glUnused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    fun setColorFilter(filter: FloatArray) {
        this.colorFilter = filter
    }

    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}
