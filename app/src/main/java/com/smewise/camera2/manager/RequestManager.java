package com.smewise.camera2.manager;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.util.Log;

import com.smewise.camera2.Config;

public class RequestManager {

    private final String TAG = Config.getTag(RequestManager.class);

    private CameraCharacteristics mCharacteristics;
    private MeteringRectangle[] mFocusArea;
    private MeteringRectangle[] mMeteringArea;
    // for reset AE/AF metering area
    private MeteringRectangle[] mResetRect = new MeteringRectangle[] {
            new MeteringRectangle(0, 0, 0, 0, 0)
    };

    public void setCharacteristics(CameraCharacteristics characteristics) {
        mCharacteristics = characteristics; // cqd.note 保存当前在运行的 Camera　的　Character　参数;
    }

    public CaptureRequest getPreviewRequest(CaptureRequest.Builder builder) {  // cqd.focus 设置自动模式，其中自动对焦设置成　CONTROL_AF_MODE_CONTINUOUS_VIDEO, 曝光模式设置成　CONTROL_AE_ANTIBANDING_MODE_AUTO, 同时清除之前所有的对焦请求;
        int afMode = getValidAFMode(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO); // cqd.note 使用 CONTROL_AF_MODE_CONTINUOUS_PICTURE 时预览会出现花面有闪烁现象
        int antiBMode = getValidAntiBandingMode(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, antiBMode);
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE); // cqd.focus　解除自动对焦模式;

        Log.d(TAG, "cqd.focus, getPreviewRequest, builder set CONTROL_AE_ANTIBANDING_MODE_AUTO,  CONTROL_AF_TRIGGER_IDLE, CONTROL_AF_MODE = CONTROL_AF_MODE_CONTINUOUS_VIDEO");
        return builder.build();
    }

    public CaptureRequest getTouch2FocusRequest(CaptureRequest.Builder builder,
            MeteringRectangle focus, MeteringRectangle metering) {
        int afMode = getValidAFMode(CaptureRequest.CONTROL_AF_MODE_AUTO);
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        if (mFocusArea == null) {
            mFocusArea = new MeteringRectangle[] {focus};
        } else {
            mFocusArea[0] = focus;
        }
        if (mMeteringArea == null) {
            mMeteringArea = new MeteringRectangle[] {metering};
        } else {
            mMeteringArea[0] = metering;
        }
        if (isMeteringSupport(true)) {  //　cqd.focus 返回相机支持的 CONTROL_MAX_REGIONS_AF 对焦的区域的数量
            Log.d(TAG, "cqd, getTouch2FocusRequest, set CONTROL_AF_REGIONS");
            builder.set(CaptureRequest.CONTROL_AF_REGIONS, mFocusArea);
        }
        if (isMeteringSupport(false)) { // cqd.focus 返回相机支持的 CONTROL_MAX_REGIONS_AE 对焦的区域的数量
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, mMeteringArea);
        }

        Log.d(TAG, "cqd.focus, getTouch2FocusRequest, set CONTROL_AE_REGIONS, CONTROL_AF_MODE_AUTO, CONTROL_MODE_AUTO, CONTROL_AE_REGIONS = {"  +
                mMeteringArea[0].getRect().left + ", " + mMeteringArea[0].getRect().top + ", " + mMeteringArea[0].getRect().right + ", " + mMeteringArea[0].getRect().bottom + "}");

        Log.d(TAG, "cqd.focus, getTouch2FocusRequest, set CONTROL_AE_REGIONS, CONTROL_AF_MODE_AUTO, CONTROL_MODE_AUTO, CONTROL_AF_REGIONS = {"  +
                mFocusArea[0].getRect().left + ", " + mFocusArea[0].getRect().top + ", " + mFocusArea[0].getRect().right + ", " + mFocusArea[0].getRect().bottom + "}");

        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);  // cqd.focus 清除对焦请求;
        return builder.build();
    }

    public CaptureRequest getFocusModeRequest(CaptureRequest.Builder builder, int focusMode) {  // cqd.focus 应用对焦模式
        int afMode = getValidAFMode(focusMode);
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO); // cqd.note 哪里恢复手动对焦后的结果
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, mResetRect);
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, mResetRect);

        Log.d(TAG, "cqd.focus, getFocusModeRequest, set CONTROL_AF_REGIONS, CONTROL_AE_REGIONS, CONTROL_AF_MODE = " + afMode + ", CONTROL_AF_REGIONS = {" +
                mResetRect[0].getRect().left + ", " + mResetRect[0].getRect().top + ", " + mResetRect[0].getRect().right + ", " + mResetRect[0].getRect().bottom + "}");

        // cancel af trigger
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        return builder.build();
    }

    public CaptureRequest getStillPictureRequest(CaptureRequest.Builder builder, int rotation) {
        builder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
        return builder.build();
    }

    public CaptureRequest getFocusDistanceRequest(CaptureRequest.Builder builder, float distance) {
        int afMode = getValidAFMode(CaptureRequest.CONTROL_AF_MODE_OFF);
            // preview
        builder.set(CaptureRequest.CONTROL_AF_MODE, afMode);

        Log.d(TAG, "cqd, getFocusDistanceRequest, afMode = " + afMode);
        float miniDistance = getMinimumDistance();
        if (miniDistance > 0) {
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, miniDistance * distance);
        }
        return builder.build();
    }

    public CaptureRequest getFlashRequest(CaptureRequest.Builder builder, String value) {
        if (!isFlashSupport()) {
            Log.w(TAG, " not support flash");
            return builder.build();
        }
        switch (value) {
            case CameraSettings.FLASH_VALUE_ON:
                builder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
                break;
            case CameraSettings.FLASH_VALUE_OFF:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                break;
            case CameraSettings.FLASH_VALUE_AUTO:
                builder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
                break;
            case CameraSettings.FLASH_VALUE_TORCH:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                break;
            default:
                Log.e(TAG, "error value for flash mode");
                break;
        }

        Log.d(TAG, "cqd, getFlashRequest, set FlashBuildRequest");
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        return builder.build();
    }

    public void applyFlashRequest(CaptureRequest.Builder builder, String value) {
        if (!isFlashSupport()) {
            Log.w(TAG, " not support flash");
            return;
        }
        switch (value) {
            case CameraSettings.FLASH_VALUE_ON:
                builder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
                break;
            case CameraSettings.FLASH_VALUE_OFF:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                break;
            case CameraSettings.FLASH_VALUE_AUTO:
                builder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
                break;
            case CameraSettings.FLASH_VALUE_TORCH:
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                break;
            default:
                Log.e(TAG, "error value for flash mode");
                break;
        }
    }

    /* ------------------------- private function------------------------- */
    private int getValidAFMode(int targetMode) {
        int[] allAFMode = mCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        Log.d(TAG, "cqd, getValidAntiBandingMode CONTROL_AF_AVAILABLE_MODES");
        for (int mode : allAFMode) {
            if (mode == targetMode) {
                return targetMode;
            }
        }
        Log.i(TAG, "not support af mode:" + targetMode + " use mode:" + allAFMode[0]);
        return allAFMode[0];
    }

    private int getValidAntiBandingMode(int targetMode) {
        int[] allABMode = mCharacteristics.get(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);

        Log.d(TAG, "cqd, getValidAntiBandingMode CONTROL_AE_AVAILABLE_ANTIBANDING_MODES");
        for (int mode : allABMode) {
            if (mode == targetMode) {
                return targetMode;
            }
        }
        Log.i(TAG, "not support anti banding mode:" + targetMode
                + " use mode:" + allABMode[0]);
        return allABMode[0];
    }

    private boolean isMeteringSupport(boolean focusArea) {
        int regionNum;
        if (focusArea) {
           regionNum = mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        } else {
            regionNum = mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        }
        return regionNum > 0;
    }

    private float getMinimumDistance() {
        Float distance = mCharacteristics.get(
                CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        if (distance == null) {
            return 0;
        }
        return distance;
    }

    private boolean isFlashSupport() {
        Boolean support = mCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        return support != null && support;
    }

    boolean canTriggerAf() {
        int[] allAFMode = mCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        return  allAFMode != null && allAFMode.length > 1;
    }

}
