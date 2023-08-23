package com.hqnguyen.camera2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    lateinit var textureView: TextureView
    private lateinit var cameraManager: CameraManager
    lateinit var cameCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var captureRequest: CaptureRequest
    lateinit var capRequest: CaptureRequest.Builder
    lateinit var imageReader: ImageReader
    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()

        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("captureThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                startCamera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

            }
        }

        imageReader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({
            val image = it.acquireLatestImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "img.jpeg")
            Log.d("TAG", "onCreate: ${getExternalFilesDir(Environment.DIRECTORY_PICTURES)}")
            val opStream = FileOutputStream(file)

            opStream.write(bytes)
            opStream.close()

            image.close()

            Toast.makeText(
                this,
                "Image Capture",
                Toast.LENGTH_LONG
            ).show()
        }, handler)

        findViewById<MaterialButton>(R.id.btn).apply {
            setOnClickListener {
                capRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                capRequest.addTarget(imageReader.surface)

                cameCaptureSession.capture(capRequest.build(), null, null)
            }
        }
    }

    private fun checkPermission() {
        val permissions = arrayListOf<String>()
        if (checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) permissions.add(Manifest.permission.CAMERA)
        if (checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permissions.size == 0) {
//            startCamera()
        } else {
            requestPermissions(this, permissions.toTypedArray(), REQUEST_CAMERA_PERMISSION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCamera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList.first(),
            object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    cameraDevice = p0

                    capRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    val surfaceTexture = textureView.surfaceTexture
                    surfaceTexture?.setDefaultBufferSize(1280, 720)
                    Log.d("TAG", "startCamera: ${textureView.width} -- ${textureView.height}")


                    val surface = Surface(surfaceTexture)
                    capRequest.addTarget(surface)
                            capRequest.addTarget(imageReader.surface)

                    cameraDevice.createCaptureSession(
                        mutableListOf(imageReader.surface, surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(p0: CameraCaptureSession) {
                                cameCaptureSession = p0

                                cameCaptureSession.setRepeatingRequest(
                                    capRequest.build(), null, null
                                )
                            }

                            override fun onConfigureFailed(p0: CameraCaptureSession) {

                            }
                        }, handler
                    )
                }

                override fun onDisconnected(p0: CameraDevice) {
                    TODO("Not yet implemented")
                }

                override fun onError(p0: CameraDevice, p1: Int) {
                    TODO("Not yet implemented")
                }
            },
            handler
        )
    }

    private fun calculateCropRegion(sensorSize: Rect, desiredAspectRatio: Float): Rect {
        val sensorAspectRatio = sensorSize.width().toFloat() / sensorSize.height()
        val cropRect: Rect

        if (sensorAspectRatio > desiredAspectRatio) {
            val cropHeight = (sensorSize.width() / desiredAspectRatio).toInt()
            val cropOffset = (sensorSize.height() - cropHeight) / 2
            cropRect = Rect(0, cropOffset, sensorSize.width(), cropOffset + cropHeight)
        } else {
            val cropWidth = (sensorSize.height() * desiredAspectRatio).toInt()
            val cropOffset = (sensorSize.width() - cropWidth) / 2
            cropRect = Rect(cropOffset, 0, cropOffset + cropWidth, sensorSize.height())
        }

        return cropRect
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED)
                checkPermission()
        }
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 123
    }
}