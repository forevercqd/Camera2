package com.smewise.camera2.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;

import com.smewise.camera2.Config;
import com.smewise.camera2.R;
import com.smewise.camera2.utils.CameraUtil;

import java.util.ArrayList;


/**
 * Created by wenzhe on 12/16/16.
 */

public class CameraSettings {
    private final String TAG = Config.getTag(CameraSettings.class);

    public static final String KEY_PICTURE_SIZE = "pref_picture_size";
    public static final String KEY_PREVIEW_SIZE = "pref_preview_size";
    public static final String KEY_CAMERA_ID = "pref_camera_id";
    public static final String KEY_MAIN_CAMERA_ID = "pref_main_camera_id";
    public static final String KEY_AUX_CAMERA_ID = "pref_aux_camera_id";
    public static final String KEY_PICTURE_FORMAT = "pref_picture_format";
    public static final String KEY_RESTART_PREVIEW = "pref_restart_preview";
    public static final String KEY_SWITCH_CAMERA = "pref_switch_camera";
    public static final String KEY_FLASH_MODE = "pref_flash_mode";
    public static final String KEY_ENABLE_DUAL_CAMERA = "pref_enable_dual_camera";
    public static final String KEY_SUPPORT_INFO = "pref_support_info";
    public static final String KEY_VIDEO_ID = "pref_video_camera_id";
    public static final String KEY_VIDEO_SIZE = "pref_video_size";
    //for flash mode
    public static final String FLASH_VALUE_ON = "on";
    public static final String FLASH_VALUE_OFF = "off";
    public static final String FLASH_VALUE_AUTO = "auto";
    public static final String FLASH_VALUE_TORCH = "torch";

    private static final ArrayList<String> SPEC_KEY = new ArrayList<>(3);

    static {
        SPEC_KEY.add(KEY_PICTURE_SIZE);
        SPEC_KEY.add(KEY_PREVIEW_SIZE);
        SPEC_KEY.add(KEY_PICTURE_FORMAT);
        SPEC_KEY.add(KEY_VIDEO_SIZE);
    }

    private SharedPreferences mSharedPreference;
    private Context mContext;
    private Point mRealDisplaySize = new Point();   // 通过 windowManager.getDefaultDisplay().getRealSize 获取的窗口的尺寸，最终显示时关键是　surface　与　铺在 surface 上的图片的分辨率，　如果surface与图片的分辨率不一致时则会变形;

    public CameraSettings(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.camera_setting, false);
        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(context);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context
                .WINDOW_SERVICE);
        Log.d(TAG, "cqd, CameraSettings, mRealDisplaySize = " + mRealDisplaySize.x + " x " + mRealDisplaySize.y);
        windowManager.getDefaultDisplay().getRealSize(mRealDisplaySize);
        Log.d(TAG, "cqd, CameraSettings, mRealDisplaySize = " + mRealDisplaySize.x + " x " + mRealDisplaySize.y);

        getSupportInfo(context);
        mContext = context;
    }

    /**
     * get related shared preference by camera id
     * @param cameraId valid camera id
     * @return related SharedPreference from the camera id
     */
    public SharedPreferences getSharedPrefById(String cameraId) {
        return mContext.getSharedPreferences(getSharedPrefName(cameraId), Context.MODE_PRIVATE);
    }

    public String getValueFromPref(String cameraId, String key, String defaultValue) {
        SharedPreferences preferences;
        if (!SPEC_KEY.contains(key)) {
            preferences = mSharedPreference;
        } else {
            preferences = getSharedPrefById(cameraId);
        }
        return preferences.getString(key, defaultValue);
    }

    public boolean setPrefValueById(String cameraId, String key, String value) {
        SharedPreferences preferences;
        if (!SPEC_KEY.contains(key)) {
            preferences = mSharedPreference;
        } else {
            preferences = getSharedPrefById(cameraId);
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        return editor.commit();
    }


    public boolean setGlobalPref(String key, String value) {
        SharedPreferences.Editor editor = mSharedPreference.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public String getGlobalPref(String key, String defaultValue) {
        return mSharedPreference.getString(key, defaultValue);
    }

    public String getGlobalPref(String key) {
        String defaultValue;
        switch (key) {
            case KEY_FLASH_MODE:
                defaultValue = mContext.getResources().getString(R.string.flash_off);
                break;
            case KEY_CAMERA_ID:
                defaultValue = mContext.getResources().getString(R.string.default_camera_id);
                break;
            default:
                defaultValue = "no value";
                break;
        }
        return mSharedPreference.getString(key, defaultValue);
    }

    private String getSharedPrefName(String cameraId) {
        return mContext.getPackageName() + "_camera_" + cameraId;
    }

    public int getPicFormat(String id, String key) {
        return Integer.parseInt(getValueFromPref(id, key, Config.IMAGE_FORMAT));
    }

    public String getPicFormatStr(String id, String key) {
        return getValueFromPref(id, key, Config.IMAGE_FORMAT);
    }

    public boolean needStartPreview() {
        return mSharedPreference.getBoolean(KEY_RESTART_PREVIEW, true);
    }

    public boolean isDualCameraEnable() {
        return mSharedPreference.getBoolean(KEY_ENABLE_DUAL_CAMERA, true);
    }

    public Size getPictureSize(String id, String key, StreamConfigurationMap map, int format) {
        String picStr = getValueFromPref(id, key, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(picStr)) {
            // preference not set, use default value
            return CameraUtil.getDefaultPictureSize(map, format);
        } else {
            String[] size = picStr.split(CameraUtil.SPLIT_TAG);
            return new Size(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
    }

    public String getPictureSizeStr(String id, String key, StreamConfigurationMap map, int format) {
        String picStr = getValueFromPref(id, key, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(picStr)) {
            // preference not set, use default value
            Size size = CameraUtil.getDefaultPictureSize(map, format);
            return size.getWidth() + CameraUtil.SPLIT_TAG + size.getHeight();
        } else {
            return picStr;
        }
    }

    public Size getPreviewSize(String id, String key, StreamConfigurationMap map) {
        String preStr = getValueFromPref(id, key, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(preStr)) {
            // preference not set, use default value
            return CameraUtil.getDefaultPreviewSize(map, mRealDisplaySize);
        } else {
            String[] size = preStr.split(CameraUtil.SPLIT_TAG);
            return new Size(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
    }

    public Size getPreviewSizeByRatio(StreamConfigurationMap map, double ratio) {
        return CameraUtil.getPreviewSizeByRatio(map, mRealDisplaySize, ratio);
    }

    public String getPreviewSizeStr(String id, String key, StreamConfigurationMap map) {
        String preStr = getValueFromPref(id, key, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(preStr)) {
            // preference not set, use default value
            Size size = CameraUtil.getDefaultPreviewSize(map, mRealDisplaySize);
            return size.getWidth() + CameraUtil.SPLIT_TAG + size.getHeight();
        } else {
            return preStr;
        }
    }

    public Size getVideoSize(String id, String key, StreamConfigurationMap map) {
        String videoStr = getValueFromPref(id, key, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(videoStr)) {
            // preference not set, use default value
            return CameraUtil.getDefaultVideoSize(map, mRealDisplaySize);
        } else {
            String[] size = videoStr.split(CameraUtil.SPLIT_TAG);
            return new Size(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
        }
    }

    public String getVideoSizeStr(String id, String key, StreamConfigurationMap map) {
        String videoStr = getValueFromPref(id, key, Config.NULL_VALUE);
        if (Config.NULL_VALUE.equals(videoStr)) {
            // preference not set, use default value
            Size size = CameraUtil.getDefaultVideoSize(map, mRealDisplaySize);
            return size.getWidth() + CameraUtil.SPLIT_TAG + size.getHeight();
        } else {
            return videoStr;
        }
    }

    public String getSupportInfo(Context context) {
        StringBuilder builder = new StringBuilder();
        DeviceManager deviceManager = new DeviceManager(context);   // cqd.note.1 获取　DeviceManger
        String[] idList = deviceManager.getCameraIdList();
        String splitLine = "- - - - - - - - - -";
        builder.append(splitLine).append("\n");

        Log.d(TAG, "CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY = " + CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
        Log.d(TAG, "CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED = " + CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        Log.d(TAG, "CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL = " + CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        Log.d(TAG, "CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 = " + CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);
//        Log.d(TAG, "CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL = " + CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL);


        for (String cameraId : idList) {    // cqd.note.2 遍历各摄像头
            builder.append("Camera ID: ").append(cameraId).append("\n");
            // hardware support level
            CameraCharacteristics c = deviceManager.getCharacteristics(cameraId);   // cqd.note.3 获取该摄像头的　character　;
            Integer level = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);     // cqd.note.4　获取 INFO_SUPPORTED_HARDWARE_LEVEL 信息;
            Log.d(TAG, "cqd, getSupportInfo,  cameraId = " + cameraId + ", INFO_SUPPORTED_HARDWARE_LEVEL, level = " + level);
            builder.append("Hardware Support Level:").append("\n");
            builder.append(CameraUtil.hardwareLevel2Sting(level)).append("\n");
            builder.append("(LEGACY < LIMITED < FULL < LEVEL_3)").append("\n");
            // Capabilities
            builder.append("Camera Capabilities:").append("\n");

            Integer face = c.get(CameraCharacteristics.LENS_FACING);
            if (CameraCharacteristics.LENS_FACING_FRONT == face){
                Log.d(TAG, "cqd, getSupportInfo, cameraID = " + cameraId + ", LENS_FACING_FRONT");
            }else if (CameraCharacteristics.LENS_FACING_BACK == face){
                Log.d(TAG, "cqd, getSupportInfo, cameraID = " + cameraId + ", LENS_FACING_BACK");
            }else if (CameraCharacteristics.LENS_FACING_EXTERNAL == face){
                Log.d(TAG, "cqd, getSupportInfo, cameraID = " + cameraId + ", LENS_FACING_EXTERNAL");
            }
            int[] caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);   // cqd.note.5　获取 REQUEST_AVAILABLE_CAPABILITIES 信息;
            for (int cap : caps) {
                Log.d(TAG, "cqd, getSupportInfo,  cameraId = " + cameraId + ", REQUEST_AVAILABLE_CAPABILITIES, cap = " + cap);
                builder.append(CameraUtil.capabilities2String(cap)).append(" ");
            }
            builder.append("\n");
            builder.append(splitLine).append("\n");
        }
        return builder.toString();
    }

}
