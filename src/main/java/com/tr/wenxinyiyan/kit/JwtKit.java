package com.tr.wenxinyiyan.kit;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWTUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * @Author: TR
 */
public class JwtKit {

    /**
     * 返回 token，不带授权类型（如 bearer）
     */
    public static String getToken() {
        return getAuthorization().split(" ")[1];
    }

    /**
     * 返回 authorization，带授权类型（如 bearer）
     */
    public static String getAuthorization() {
        return getAuthorization(getRequest());
    }

    public static String getAuthorization(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }

    public static JSONObject getClaimsJson() {
        return getClaimsJson(getToken());
    }

    public static JSONObject getClaimsJson(String token) {
        return JWTUtil.parseToken(token).getPayload().getClaimsJson();
    }

    public static String getUsername() {
        if (RequestContextHolder.getRequestAttributes() == null || getAuthorization(getRequest()) == null) {
            return null;
        }
        return getClaimsJson().getStr("username");
    }

    public static String getRealname() {
        if (RequestContextHolder.getRequestAttributes() == null || getAuthorization(getRequest()) == null) {
            return null;
        }
        return getClaimsJson().getStr("realname");
    }

    public static String getSchoolCode() {
        if (RequestContextHolder.getRequestAttributes() == null || getAuthorization(getRequest()) == null) {
            return null;
        }
        return getClaimsJson().getStr("schoolCode");
    }

    public static String getSchoolName() {
        if (RequestContextHolder.getRequestAttributes() == null || getAuthorization(getRequest()) == null) {
            return null;
        }
        return getClaimsJson().getStr("schoolName");
    }

    public static String getUsername(String token) {
        return getClaimsJson(token).getStr("username");
    }

    public static Long getUserId() {
        if (RequestContextHolder.getRequestAttributes() == null || getAuthorization(getRequest()) == null) {
            return null;
        }
        return getClaimsJson().getLong("userId");
    }

    public static String getBusinessId() {
        return getClaimsJson().getStr("businessId");
    }

    public static String getCreateBy() {
        return getClaimsJson().getStr("createBy");
    }

    public static JSONArray getAuthorities() {
        return getClaimsJson().getJSONArray("authorities");
    }

    public static Boolean hasRole(String rolename) {
        JSONArray authorities = JwtKit.getAuthorities();
        for (Object authority : authorities) {
            if (authority instanceof JSONObject && rolename.equals(((JSONObject) authority).getStr("authority"))) {
                return true;
            }
        }
        return false;
    }

    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
    }

}
