package com.android.settings.display;

import android.R.integer;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.util.Log;

import com.android.settings.utils.ReflectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Drm Display Setting.
 */

public class DrmDisplaySetting {
    private final static String TAG = "DrmDisplaySetting";

    public final static int DISPLAY_TYPE_HDMI = 1;//mid hdmi is aux
    public final static int DISPLAY_TYPE_DP = 1;

    private static void logd(String text) {
        Log.d(TAG, TAG + " - " + text);
    }

    public static List<DisplayInfo> getDisplayInfoList() {
        List<DisplayInfo> displayInfos = new ArrayList<DisplayInfo>();
        Object rkDisplayOutputManager = null;

        try {
            rkDisplayOutputManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
            logd("getDisplayInfoList->rkDisplayOutputManager->name:" + rkDisplayOutputManager.getClass().getName());
        } catch (Exception e) {
        }
        logd(" getDisplayInfoList 1");
        int[] mainTypes = (int[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getIfaceList", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_HDMI});
        logd(" getDisplayInfoList 2");
        int[] externalTypes = (int[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getIfaceList", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_HDMI});
        logd(" getDisplayInfoList 3");

        if (mainTypes != null && mainTypes.length > 0) {
            int currMainType = (Integer) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getCurrentInterface", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_HDMI});
            DisplayInfo displayInfo = new DisplayInfo();
            displayInfo.setDisplayId(DISPLAY_TYPE_HDMI);
            logd(" getDisplayInfoList 4");
            displayInfo.setDescription((String) ReflectUtils.invokeMethod(rkDisplayOutputManager, "typetoface", new Class[]{int.class}, new Object[]{currMainType}));
            logd(" getDisplayInfoList 5");
            displayInfo.setType(currMainType);
            String[] orginModes = (String[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getModeList", new Class[]{int.class, int.class}, new Object[]{DISPLAY_TYPE_HDMI, currMainType});
            orginModes = filterOrginModes(orginModes);
            displayInfo.setOrginModes(orginModes);
            displayInfo.setModes(getFilterModeList(orginModes));
            logd(" getDisplayInfoList 6");
            displayInfos.add(displayInfo);
        }
        if (externalTypes != null && externalTypes.length > 0) {
            int currExternalType = (Integer) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getCurrentInterface", new Class[]{int.class}, new Object[]{1});
            DisplayInfo displayInfo = new DisplayInfo();
            displayInfo.setType(currExternalType);
            String[] orginModes = (String[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getModeList", new Class[]{int.class, int.class}, new Object[]{1, externalTypes});
            orginModes = filterOrginModes(orginModes);
            displayInfo.setOrginModes(orginModes);
            displayInfo.setModes(getFilterModeList(orginModes));
            displayInfo.setDescription((String) ReflectUtils.invokeMethod(rkDisplayOutputManager, "typetoface", new Class[]{int.class}, new Integer[]{currExternalType}));
            displayInfo.setDisplayId(1);
            displayInfos.add(displayInfo);
        }
        return displayInfos;
    }

    public static List<String> getDisplayModes(DisplayInfo di) {
        List<String> res = new ArrayList<String>();
        if (di.getDisplayId() == DISPLAY_TYPE_HDMI) {
            di = getHdmiDisplayInfo();
        } else if (di.getDisplayId() == DISPLAY_TYPE_DP) {
            di = getDpDisplayInfo();
        }
        if (di != null) {
            String[] modes = di.getOrginModes();
            if (modes != null && modes.length != 0) {
                res = Arrays.asList(modes);
            }
        }
        return res;
    }

    public static String getCurDisplayMode(DisplayInfo di) {
        if (di.getDisplayId() == DISPLAY_TYPE_HDMI) {
            logd("DrmDisplaySetting getCurDisplayMode DISPLAY_TYPE_HDMI" + System.currentTimeMillis());
            return getCurHdmiMode();
        } else if (di.getDisplayId() == DISPLAY_TYPE_DP) {
            logd("DrmDisplaySetting getCurDisplayMode DISPLAY_TYPE_DP " + System.currentTimeMillis());
            return getCurDpMode();
        }
        return null;
    }

    public static String getCurHdmiMode() {
        return getHdmiMode();
    }

    public static String getCurDpMode() {
        return getDpMode();
    }

    public static void setDisplayModeTemp(DisplayInfo di, int index) {
        String mode = getDisplayModes(di).get(index);
        setDisplayModeTemp(di, mode);
    }

    public static void setDisplayModeTemp(DisplayInfo di, String mode) {
        if (di.getDisplayId() == DISPLAY_TYPE_HDMI) {
            setHdmiModeTemp(mode);
        } else if (di.getDisplayId() == DISPLAY_TYPE_DP) {
            setDpModeTemp(mode);
        }
    }

    public static void saveConfig() {
        Object rkDisplayOutputManager = null;
        try {
            rkDisplayOutputManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
        } catch (Exception e) {
            // no handle
        }
        if (rkDisplayOutputManager != null) {
            int result = (Integer) ReflectUtils.invokeMethodNoParameter(rkDisplayOutputManager, "saveConfig");
        }
    }

    public static void updateDisplayInfos() {
        Object rkDisplayOutputManager = null;
        try {
            rkDisplayOutputManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
        } catch (Exception e) {
            // no handle
        }
        if (rkDisplayOutputManager != null) {
            logd("updateDisplayInfos");
            int result = (Integer) ReflectUtils.invokeMethodNoParameter(rkDisplayOutputManager, "updateDisplayInfos");
        }
    }

    public static void confirmSaveDisplayMode(DisplayInfo di, boolean isSave) {
        if (di == null) {
            return;
        }
        if (di.getDisplayId() == DISPLAY_TYPE_HDMI) {
            confirmSaveHdmiMode(isSave);
        } else if (di.getDisplayId() == DISPLAY_TYPE_DP) {
            confirmSaveDpMode(isSave);
        }
        saveConfig();
    }

    private static String tmpSetHdmiMode = null;
    private static String curSetHdmiMode = "Auto";

    public static DisplayInfo getHdmiDisplayInfo() {
        Object rkDisplayOutputManager = null;
        try {
            rkDisplayOutputManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
            logd("getDisplayInfoList->rkDisplayOutputManager->name:" + rkDisplayOutputManager.getClass().getName());
        } catch (Exception e) {
        }
        if (rkDisplayOutputManager == null)
            return null;
        logd(" getHdmiDisplayInfo 1");
        int[] mainTypes = (int[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getIfaceList", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_HDMI});
        logd(" getHdmiDisplayInfo 2");
        if (mainTypes != null && mainTypes.length > 0) {
            int currMainType = (Integer) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getCurrentInterface", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_HDMI});
            DisplayInfo displayInfo = new DisplayInfo();
            displayInfo.setDisplayId(DISPLAY_TYPE_HDMI);
            logd(" getHdmiDisplayInfo 3");
            displayInfo.setDescription((String) ReflectUtils.invokeMethod(rkDisplayOutputManager, "typetoface", new Class[]{int.class}, new Object[]{currMainType}));
            logd(" getHdmiDisplayInfo 4");
            displayInfo.setType(currMainType);
            String[] orginModes = (String[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getModeList", new Class[]{int.class, int.class}, new Object[]{DISPLAY_TYPE_HDMI, currMainType});
            orginModes = filterOrginModes(orginModes);
            displayInfo.setOrginModes(orginModes);
            displayInfo.setModes(getFilterModeList(orginModes));
            logd(" getHdmiDisplayInfo 5");
            return displayInfo;
        }
        return null;
    }

    private static String getHdmiMode() {
        Object rkDisplayOutputManager = null;
        try {
            rkDisplayOutputManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
            logd("getDisplayInfoList->rkDisplayOutputManager->name:" + rkDisplayOutputManager.getClass().getName());
        } catch (Exception e) {
            // no handle
        }
        if (rkDisplayOutputManager == null)
            return null;
        logd(" getHdmiMode 1");
        int[] mainTypes = (int[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getIfaceList", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_HDMI});
        logd(" getHdmiMode 2");
        if (mainTypes != null && mainTypes.length > 0) {
            int currMainType = (Integer) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getCurrentInterface", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_HDMI});
            return (String) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getCurrentMode", new Class[]{int.class, int.class}, new Object[]{DISPLAY_TYPE_HDMI, currMainType});
        }
        return null;
    }

    private static void setHdmiModeTemp(String mode) {
        setHdmiMode(mode);
        tmpSetHdmiMode = mode;
    }

    private static void confirmSaveHdmiMode(boolean isSave) {
        if (tmpSetHdmiMode == null) {
            return;
        }
        if (isSave) {
            curSetHdmiMode = tmpSetHdmiMode;
        } else {
            setHdmiMode(curSetHdmiMode);
            tmpSetHdmiMode = null;
        }
    }

    private static void setHdmiMode(String mode) {
        //SystemProperties.set(PROP_RESOLUTION_HDMI, mode);
        Object rkDisplayOutputManager = null;
        try {
            rkDisplayOutputManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
            logd("getDisplayInfoList->rkDisplayOutputManager->name:" + rkDisplayOutputManager.getClass().getName());
        } catch (Exception e) {
        }
        if (rkDisplayOutputManager == null)
            return;
        logd(" setHdmiMode 1");
        int[] mainTypes = (int[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getIfaceList", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_HDMI});
        logd(" setHdmiMode 2");
        if (mainTypes != null && mainTypes.length > 0) {
            logd(" setHdmiMode mode = " + mode);
            int currMainType = (Integer) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getCurrentInterface", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_HDMI});
            ReflectUtils.invokeMethod(rkDisplayOutputManager, "setMode", new Class[]{int.class, int.class, String.class}, new Object[]{DISPLAY_TYPE_HDMI, currMainType, mode});
        }
        logd(" setHdmiMode 3");
    }

    private static String tmpSetDpMode = null;
    private static String curSetDpMode = "1920x1080p60";

    public static DisplayInfo getDpDisplayInfo() {
        Object rkDisplayOutputManager = null;
        try {
            rkDisplayOutputManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
            logd("getDisplayInfoList->rkDisplayOutputManager->name:" + rkDisplayOutputManager.getClass().getName());
        } catch (Exception e) {
        }
        logd(" getDpDisplayInfo 1");
        int[] externalTypes = (int[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getIfaceList", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_DP});
        logd(" getDpDisplayInfo 2");
        if (externalTypes != null && externalTypes.length > 0) {
            int currMainType = (Integer) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getCurrentInterface", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_DP});
            DisplayInfo displayInfo = new DisplayInfo();
            displayInfo.setDisplayId(DISPLAY_TYPE_DP);
            logd(" getDpDisplayInfo 3");
            displayInfo.setDescription((String) ReflectUtils.invokeMethod(rkDisplayOutputManager, "typetoface", new Class[]{int.class}, new Object[]{currMainType}));
            logd(" getDpDisplayInfo 4");
            displayInfo.setType(currMainType);
            String[] orginModes = (String[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getModeList", new Class[]{int.class, int.class}, new Object[]{DISPLAY_TYPE_DP, currMainType});
            orginModes = filterOrginModes(orginModes);
            displayInfo.setOrginModes(orginModes);
            displayInfo.setModes(getFilterModeList(orginModes));
            logd(" getDpDisplayInfo 5");
            return displayInfo;
        }
        return null;
    }

    private static String getDpMode() {
        Object rkDisplayOutputManager = null;
        try {
            rkDisplayOutputManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
            logd("getDisplayInfoList->rkDisplayOutputManager->name:" + rkDisplayOutputManager.getClass().getName());
        } catch (Exception e) {
            // no handle
        }
        if (rkDisplayOutputManager == null)
            return null;
        logd(" getDpMode 1");
        int[] mainTypes = (int[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getIfaceList", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_DP});
        logd(" getDpMode 2");
        if (mainTypes != null && mainTypes.length > 0) {
            int currMainType = (Integer) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getCurrentInterface", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_DP});
            return (String) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getCurrentMode", new Class[]{int.class, int.class}, new Object[]{DISPLAY_TYPE_DP, currMainType});
        }
        return null;
    }

    private static void setDpModeTemp(String reso) {
        setDpMode(reso);
        tmpSetDpMode = reso;
    }

    private static void confirmSaveDpMode(boolean isSave) {
        if (tmpSetDpMode == null) {
            return;
        }
        if (isSave) {
            curSetDpMode = tmpSetDpMode;
        } else {
            setDpMode(curSetDpMode);
            tmpSetDpMode = null;
        }
    }

    private static void setDpMode(String reso) {
        Object rkDisplayOutputManager = null;
        try {
            rkDisplayOutputManager = Class.forName("android.os.RkDisplayOutputManager").newInstance();
            logd("getDisplayInfoList->rkDisplayOutputManager->name:" + rkDisplayOutputManager.getClass().getName());
        } catch (Exception e) {
        }
        if (rkDisplayOutputManager == null)
            return;
        logd(" setDpMode 1");
        int[] mainTypes = (int[]) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getIfaceList", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_DP});
        logd(" setDpMode 2");
        if (mainTypes != null && mainTypes.length > 0) {
            int currMainType = (Integer) ReflectUtils.invokeMethod(rkDisplayOutputManager, "getCurrentInterface", new Class[]{int.class}, new Object[]{DISPLAY_TYPE_DP});
            ReflectUtils.invokeMethod(rkDisplayOutputManager, "setMode", new Class[]{int.class, int.class, String.class}, new Object[]{DISPLAY_TYPE_DP, currMainType, reso});
        }
    }

    private static String[] filterOrginModes(String[] modes) {
        if (modes == null)
            return null;
        List<String> filterModeList = new ArrayList<String>();
        List<String> resModeList = new ArrayList<String>();
        for (int i = 0; i < modes.length; ++i) {
            logd("filterOrginModes->mode:" + modes[i]);
            String itemMode = modes[i];
            int endIndex = itemMode.indexOf("-");
            if (endIndex > 0)
                itemMode = itemMode.substring(0, endIndex);
            if (!resModeList.contains(itemMode)) {
                resModeList.add(itemMode);
                if (!filterModeList.contains(modes[i]))
                    filterModeList.add(modes[i]);
            }
        }
        return filterModeList.toArray(new String[0]);
    }

    private static String[] getFilterModeList(String[] modes) {
        if (modes == null)
            return null;
        String[] filterModes = new String[modes.length];
        for (int i = 0; i < modes.length; ++i) {
            String itemMode = modes[i];
            int endIndex = itemMode.indexOf("-");
            if (endIndex > 0)
                itemMode = itemMode.substring(0, endIndex);
            filterModes[i] = itemMode;
        }
        return filterModes;
    }
}
