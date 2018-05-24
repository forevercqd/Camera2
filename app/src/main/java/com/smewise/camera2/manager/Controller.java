package com.smewise.camera2.manager;

import android.app.FragmentManager;

import com.smewise.camera2.ui.AppBaseUI;

public interface Controller {
    int CAMERA_MODULE_STOP = 1;
    int CAMERA_MODULE_PAUSE = 1 << 1;
    int CAMERA_MODULE_RUNNING = 1 << 2;
    int CAMERA_STATE_OPENED = 1 << 3;
    int CAMERA_STATE_UI_READY = 1 << 4;
    int CAMERA_STATE_PREVIEWING = 1 << 5;

    void changeModule(int module);

    CameraToolKit getToolKit();

    FragmentManager getFragmentManager();

    void showSetting(boolean stopModule);

    CameraSettings getSettingManager();

    AppBaseUI getBaseUI();
}