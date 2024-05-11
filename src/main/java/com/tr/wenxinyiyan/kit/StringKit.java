package com.tr.wenxinyiyan.kit;

import org.apache.commons.lang3.StringUtils;

/**
 * @Author TR
 */
public class StringKit extends StringUtils {

    private static final char UNDERLINE = '_';

    public static String firstCharUpper(String source) {
        String firstStr = source.substring(0, 1);
        source = source.replace(firstStr, StringKit.toRootUpperCase(firstStr));
        return source;
    }

    public static String firstCharLower(String source) {
        String firstStr = source.substring(0, 1);
        source = StringKit.toRootLowerCase(firstStr).concat(source.substring(1));
        return source;
    }

    /**
     * 驼峰转下划线
     *
     * @param param
     * @return
     */
    public static String camelToUnderline(String param) {
        if (param == null || "".equals(param.trim())) {
            return "";
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append(UNDERLINE);
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 下划线转驼峰
     *
     * @param param
     * @return
     */
    public static String underlineToCamel(String param) {
        if (param == null || "".equals(param.trim())) {
            return "";
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (c == UNDERLINE) {
                if (++i < len) {
                    sb.append(Character.toUpperCase(param.charAt(i)));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
