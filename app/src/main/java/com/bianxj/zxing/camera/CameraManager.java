package com.bianxj.zxing.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

import com.bianxj.zxing.camera.open.OpenCamera;
import com.bianxj.zxing.camera.open.OpenCameraInterface;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * 作者:边贤君
 * 描述:
 * 创建日期:2017/12/23 10:11
 */

public class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    private Context context;
    private CameraConfigurationManager configManager;
    private AutoFocusManager autoFocusManager;
    private OpenCamera camera;

    private boolean initialized;
    private boolean previewing;
    private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;
    private PreviewCallback callback;
    private MultiFormatReader multiFormatReader;

    public CameraManager(Context context) {
        this.context = context;
        this.configManager = new CameraConfigurationManager(context);
        initFormatReader();
    }

    private void initFormatReader(){
        this.multiFormatReader = new MultiFormatReader();
        Collection<BarcodeFormat> decodeFormats = EnumSet.of(BarcodeFormat.QR_CODE);
        Map<DecodeHintType,Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS,decodeFormats);
        multiFormatReader.setHints(hints);
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    public synchronized void openDriver(SurfaceHolder holder) throws IOException {
        OpenCamera theCamera = camera;
        if (theCamera == null) {
            theCamera = OpenCameraInterface.open(requestedCameraId);
            if (theCamera == null) {
                throw new IOException("Camera.open() failed to return object from driver");
            }
            camera = theCamera;
        }

        if (!initialized) {
            initialized = true;
            configManager.initFromCameraParameters(theCamera);
//            if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
//                setManualFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
//                requestedFramingRectWidth = 0;
//                requestedFramingRectHeight = 0;
//            }
        }

        Camera cameraObject = theCamera.getCamera();
        Camera.Parameters parameters = cameraObject.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
        try {
            configManager.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.i(TAG, "Resetting to saved camera params: " + parametersFlattened);
            // Reset:
            if (parametersFlattened != null) {
                parameters = cameraObject.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    cameraObject.setParameters(parameters);
                    configManager.setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }
        cameraObject.setPreviewDisplay(holder);
    }

    public synchronized boolean isOpen() {
        return camera != null;
    }

    /**
     * Closes the camera driver if still in use.
     */
    public synchronized void closeDriver() {
        if (camera != null) {
            camera.getCamera().release();
            camera = null;
            // Make sure to clear these each time we close the camera, so that any scanning rect
            // requested by intent is forgotten.
//            framingRect = null;
//            framingRectInPreview = null;
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    public synchronized void startPreview() {
        OpenCamera theCamera = camera;
        if (theCamera != null && !previewing) {
            theCamera.getCamera().startPreview();
            previewing = true;
            autoFocusManager = new AutoFocusManager(context, theCamera.getCamera());
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public synchronized void stopPreview() {
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (camera != null && previewing) {
            camera.getCamera().stopPreview();
//            previewCallback.setHandler(null, 0);
            previewing = false;
        }
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     */
    public synchronized void requestPreviewFrame(PreviewCallback callback) {
        OpenCamera theCamera = camera;
        if (theCamera != null && previewing) {
            this.callback = callback;
            theCamera.getCamera().setOneShotPreviewCallback(previewCallback);
        }
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Point cameraResolution = configManager.getCameraResolution();
            callback.onPreviewFrame(data,camera,cameraResolution.x,
                    cameraResolution.y);
        }
    };

    public interface PreviewCallback {
        public void onPreviewFrame(byte[] data, Camera camera, int width, int height);
    }

    public Rect getDecodeArea(Rect framingRect) {
        if (framingRect == null) {
            return null;
        }
        Rect rect = new Rect(framingRect);
        Point cameraResolution = configManager.getCameraResolution();
        Point screenResolution = configManager.getScreenResolution();
        if (cameraResolution == null || screenResolution == null) {
            // Called early, before init even finished
            return null;
        }
        rect.left = rect.left * cameraResolution.y / screenResolution.x;
        rect.right = rect.right * cameraResolution.y / screenResolution.x;
        rect.top = rect.top * cameraResolution.x / screenResolution.y;
        rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
        return rect;
    }

    public Result decodeQR(Rect decodeArea,byte[] data, int width, int height){
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data,width,height,decodeArea.left,decodeArea.top,decodeArea.width(),decodeArea.height(),true);
        Result result = null;
        if ( source != null ){
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                result = multiFormatReader.decodeWithState(bitmap);
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}
