package com.colin.lrml

import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.colin.lrml.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalGetImage class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCamera()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            var imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, DocAnalyzer(binding.rectOverlayFaces))
                }

            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
}

@ExperimentalGetImage
private class DocAnalyzer(private val rectOverlay: RectOverlay) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {

        val realTimeOpts = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .enableTracking()
            .build()

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val detector = FaceDetection.getClient(realTimeOpts)
            val fixedBounds: MutableList<RectF> = mutableListOf()
            val result = detector.process(image)
                .addOnSuccessListener { faces ->
                    val reverseDimens = image.rotationDegrees == 90 || image.rotationDegrees == 270
                    val width = if (reverseDimens) image.height else image.width
                    val height = if (reverseDimens) image.width else image.height
                    val scaleX = rectOverlay.width.toFloat() / width
                    val scaleY = rectOverlay.height.toFloat() / height

                    faces.forEach { face ->
                        val scaledLeft = scaleX * width-scaleX*face.boundingBox.right
                        val scaledTop = scaleY * face.boundingBox.top
                        val scaledRight = scaleX * width-scaleX*face.boundingBox.left
                        val scaledBottom = scaleY * face.boundingBox.bottom
                        val scaledBoundingBox = RectF(scaledLeft, scaledTop, scaledRight, scaledBottom)
                        fixedBounds.add(scaledBoundingBox)
                    }

                    rectOverlay.setFacesBounds(fixedBounds)
                }
                .addOnFailureListener { e ->
                    Log.d("ML", e.message!!)
                }
                .addOnCompleteListener {
                    mediaImage.close()
                    imageProxy.close()
                }
        }
    }
}