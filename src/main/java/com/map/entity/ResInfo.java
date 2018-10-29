package com.map.entity;

import org.json.simple.JSONObject;

public class ResInfo {

    //0-成功，1-失败，2-未登录
    private String status;

    //如success或fail
    private String message;

    //详细信息
    private String detail;

    private JSONObject json;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public JSONObject getJson() {
        return json;
    }

    public void setJson(JSONObject json) {
        this.json = json;
    }
}
