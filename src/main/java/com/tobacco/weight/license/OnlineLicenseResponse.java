package com.tobacco.weight.license;

import org.json.JSONObject;

/**
 * 封装在线许可证校验的返回结果。
 */
public class OnlineLicenseResponse {

    private final LicenseVerificationResult result;
    private final JSONObject licenseData;

    private OnlineLicenseResponse(LicenseVerificationResult result, JSONObject licenseData) {
        this.result = result;
        this.licenseData = licenseData;
    }

    public static OnlineLicenseResponse success(JSONObject licenseData) {
        return new OnlineLicenseResponse(LicenseVerificationResult.SUCCESS, cloneJson(licenseData));
    }

    public static OnlineLicenseResponse failure(LicenseVerificationResult result) {
        return new OnlineLicenseResponse(result, null);
    }

    public LicenseVerificationResult getResult() {
        return result;
    }

    public boolean isSuccess() {
        return result == LicenseVerificationResult.SUCCESS;
    }

    /**
     * 获取许可证数据的副本，避免外部修改原始对象。
     */
    public JSONObject getLicenseData() {
        return cloneJson(licenseData);
    }

    private static JSONObject cloneJson(JSONObject source) {
        return source == null ? null : new JSONObject(source.toString());
    }
}

