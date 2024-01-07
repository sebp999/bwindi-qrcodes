package com.sebswebs.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.sebswebs.myapplication.databinding.ActivityMainBinding
import java.util.concurrent.Executors
import androidx.appcompat.widget.Toolbar


private const val CAMERA_PERMISSION_REQUEST_CODE = 1

@ExperimentalGetImage
class MainActivity : BaseMenus() {

    private lateinit var binding: ActivityMainBinding
    private var alreadySwitched = false
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        Log.e("xxxx", "xxxx")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        if (hasCameraPermission()) bindCameraUseCases()
        else requestPermission()

    }

    // checking to see whether user has already granted permission
    private fun hasCameraPermission() =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission(){
        // opening up dialog to ask for camera permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    private fun switchToReadDatabase() {
        Log.e("xxx", "stub read database")
//        val switchActivityIntent = Intent(this, SyncData::class.java)
//        startActivity(switchActivityIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // user granted permissions - we can set up our scanner
            bindCameraUseCases()
        } else {
            // user did not grant permissions - we can't use the camera
            Toast.makeText(this,
                "Camera permission required",
                Toast.LENGTH_LONG
            ).show()
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // setting up the preview use case
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraView.surfaceProvider)
                }

            // configure our MLKit BarcodeScanning client

            /* passing in our desired barcode formats - MLKit supports additional formats outside of the
            ones listed here, and you may not need to offer support for all of these. You should only
            specify the ones you need */
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_QR_CODE
            ).build()

            // getClient() creates a new instance of the MLKit barcode scanner with the specified options
            val scanner = BarcodeScanning.getClient(options)

            // setting up the analysis use case
            val analysisUseCase = ImageAnalysis.Builder()
                .build()

            // define the actual functionality of our analysis use case
            analysisUseCase.setAnalyzer(
                Executors.newSingleThreadExecutor(),
                { imageProxy ->
                    processImageProxy(scanner, imageProxy)
                }
            )

            // configure to use the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    previewUseCase,
                    analysisUseCase)
            } catch (illegalStateException: IllegalStateException) {
                // If the use case has already been bound to another lifecycle or method is not called on main thread.
                Log.e(TAG, illegalStateException.message.orEmpty())
            } catch (illegalArgumentException: IllegalArgumentException) {
                // If the provided camera selector is unable to resolve a camera to be used for the given use cases.
                Log.e(TAG, illegalArgumentException.message.orEmpty())
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onFoundActivity(value: String) {
        Log.e(TAG, "switching...")
        val switchActivityIntent = Intent(this, ShowResult::class.java)
        switchActivityIntent.putExtra("barcodeValue", value)
        if (!alreadySwitched) {
            startActivity(switchActivityIntent)
            alreadySwitched = true
            Log.e(TAG,"I switched")
        }
        else {
            Log.e(TAG, "I had already switched")
        }

    }

    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        Log.e(TAG, imageProxy.imageInfo.rotationDegrees.toString())
        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )

            Log.e(TAG, inputImage.toString() )
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->

                    val barcode = barcodeList.getOrNull(0)
                    Log.e(TAG, "barcode success" + barcode.toString())
                    // `rawValue` is the decoded value of the barcode
                    barcode?.rawValue?.let { value ->
                        Log.e(TAG, value)
                        binding.bottomText.text = value
                        onFoundActivity(value)
                    }
                }
                .addOnFailureListener {
                    // This failure will happen if the barcode scanning model
                    // fails to download from Google Play Services
                    Log.e(TAG, "barcode success")
                    Log.e(TAG, it.message.orEmpty())
                }.addOnCompleteListener {
                    Log.e(TAG, "barcode complete")
                    // When the image is from CameraX analysis use case, must
                    // call image.close() on received images when finished
                    // using them. Otherwise, new images may not be received
                    // or the camera may stall.

                    imageProxy.image?.close()
                    imageProxy.close()
                }
        }
    }

    companion object {
        val TAG: String = MainActivity::class.java.simpleName

    }
}