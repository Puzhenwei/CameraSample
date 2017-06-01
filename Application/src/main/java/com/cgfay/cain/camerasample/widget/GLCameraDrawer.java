package com.cgfay.cain.camerasample.widget;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Administrator on 2017/5/31.
 */

public class GLCameraDrawer {
    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
            "attribute vec2 inputTextureCoordinate;" +
            "varying vec2 textureCoordinate;" +
            "void main()" +
            "{"+
            "   gl_Position = vPosition;"+
            "   textureCoordinate = inputTextureCoordinate;" +
            "}";

    private final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n"+
            "precision mediump float;" +
            "varying vec2 textureCoordinate;\n" +
            "uniform samplerExternalOES s_texture;\n" +
            "void main() " +
            "{" +
            "   gl_FragColor = texture2D(s_texture, textureCoordinate);\n" +
            "}";

    private FloatBuffer vertexBuffer, textureBuffer;
    private ShortBuffer drawListBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mTextureHandle;

    // order to draw vertices
    private short drawOrder[] = {0, 1, 2, 0, 2, 3};

    // number of coordinate per vertex in this array
    private static final int COORDS_PER_VERTEX = 2;

    // 4 bytes per vertex
    private final int vertexStride = COORDS_PER_VERTEX * 4;

    private static float squareCoords[] = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
             1.0f, -1.0f,
             1.0f,  1.0f,
    };
    private static float textureVertices[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };

    private int mTexture;

    public GLCameraDrawer(int texture) {
        mTexture = texture;
        // 初始化vertex
        ByteBuffer buffer = ByteBuffer.allocateDirect(squareCoords.length * 4);
        buffer.order(ByteOrder.nativeOrder());
        vertexBuffer = buffer.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        //初始化drawlist
        ByteBuffer drawBuffer = ByteBuffer.allocateDirect(drawOrder.length * 2);
        drawBuffer.order(ByteOrder.nativeOrder());
        drawListBuffer = drawBuffer.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        //初始化texture Vertices
        ByteBuffer texBuffer = ByteBuffer.allocateDirect(textureVertices.length * 4);
        texBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = texBuffer.asFloatBuffer();
        textureBuffer.put(textureVertices);
        textureBuffer.position(0);


        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    public void onDraw(float[] mtx) {
        GLES20.glUseProgram(mProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTexture);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        mTextureHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureHandle);
        GLES20.glVertexAttribPointer(mTextureHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, vertexStride, textureBuffer);


        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);


        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureHandle);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private float[] transformTexturecoordinates(float[] coords, float[] matrix) {
        float[] result = new float[coords.length];
        float[] vt = new float[4];

        for (int i = 0; i < coords.length; i += 2) {
            float[] v = {coords[i], coords[i + 1], 0, 1};
            Matrix.multiplyMM(vt, 0, matrix, 0, v, 0);
            result[i] = vt[0];
            result[i + 1] = vt[1];
        }

        return result;
    }

}
