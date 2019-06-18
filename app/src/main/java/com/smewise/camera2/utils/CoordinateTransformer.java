package com.smewise.camera2.utils;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.nfc.Tag;
import android.util.Log;

import com.smewise.camera2.Config;
import com.smewise.camera2.manager.FocusOverlayManager;

/**
 * Transform coordinates to and from preview coordinate space and camera driver
 * coordinate space.
 */
public class CoordinateTransformer {

    private final Matrix mPreviewToCameraTransform;
    private RectF mDriverRectF;
    private final String TAG = Config.getTag(CoordinateTransformer.class);

    /**
     * Convert rectangles to / from camera coordinate and preview coordinate space.
     * @param chr camera characteristics
     * @param previewRect the preview rectangle size and position.
     */
    public CoordinateTransformer(CameraCharacteristics chr, RectF previewRect) {
        if (!hasNonZeroArea(previewRect)) {
            throw new IllegalArgumentException("previewRect");
        }
        Rect rect = chr.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);   // cqd.focus 表示真正接收光线的区域，因此成像的区域是该参数指定的区域,其实也是 SCALER_CROP_REGION 区域;
        Log.d(TAG, "cqd.focus, CoordinateTransformer, SENSOR_INFO_ACTIVE_ARRAY_SIZE, rect{left = " + rect.left +
                ", right = " + rect.right + ", top = " + rect.top + ", bottom = " + rect.bottom);

//        Rect cropRect = chr.get(CaptureRequest.SCALER_CROP_REGION);

        Integer sensorOrientation = chr.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int rotation = sensorOrientation == null ? 90 : sensorOrientation;
        mDriverRectF = new RectF(rect); //
        Integer face = chr.get(CameraCharacteristics.LENS_FACING);  // cqd.focus 获取摄像头 id;
        boolean mirrorX = face != null && face == CameraCharacteristics.LENS_FACING_FRONT;
        mPreviewToCameraTransform = previewToCameraTransform(mirrorX, rotation, previewRect);

        Log.d(TAG, "cqd.focus, CoordinateTransformer, mDriverRectF = {" + rect.left + ", " + rect.right + ", " + rect.top + ", " + rect.bottom + "}");

        float []matrix = new float[16];

        mPreviewToCameraTransform.getValues(matrix);
    }

    /**
     * Transform a rectangle in preview view space into a new rectangle in
     * camera view space.
     * @param source the rectangle in preview view space
     * @return the rectangle in camera view space.
     */
    public RectF toCameraSpace(RectF source) {
        RectF result = new RectF();
        mPreviewToCameraTransform.mapRect(result, source);
        return result;
    }

    private Matrix previewToCameraTransform(boolean mirrorX, int sensorOrientation,
          RectF previewRect) {
        Matrix transform = new Matrix();
        // Need mirror for front camera.
        transform.setScale(mirrorX ? -1 : 1, 1);    // cqd.focus 绽放比例, 第一个参数表示x方向, 第二个参数表示y方向
        // Because preview orientation is different  form sensor orientation,
        // rotate to same orientation, Counterclockwise.
        transform.postRotate(-sensorOrientation);       // cqd.focus 旋转角度
        // Map rotated matrix to preview rect
        transform.mapRect(previewRect); // cqd.focus 对　previewRect　执行旋转
        // Map  preview coordinates to driver coordinates
        Matrix fill = new Matrix();
        fill.setRectToRect(previewRect, mDriverRectF, Matrix.ScaleToFit.FILL);  // cqd.focus 生成将 previewRect 填充至 mDriverRectF 时的变换矩阵;
        // Concat the previous transform on top of the fill behavior.
        transform.setConcat(fill, transform);   // 计算　fill* transform
        // finally get transform matrix

        float []fillMatrixValues = new float[16];   // cqd.focus.question　此处的矩阵到底是什么？为什么跟实际的不一致
        fill.getValues(fillMatrixValues);
        Log.d(TAG, "cqd.focus, previewToCameraTransform, calcute the transform matrix");
        return transform;
    }

    private boolean hasNonZeroArea(RectF rect) {
        return rect.width() != 0 && rect.height() != 0;
    }
}
