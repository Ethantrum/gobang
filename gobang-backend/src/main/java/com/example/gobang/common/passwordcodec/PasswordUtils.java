package com.example.gobang.common.passwordcodec;

import cn.hutool.crypto.SecureUtil;

public class PasswordUtils {
    public static String encode(String password) {
        return SecureUtil.md5(password);
    }
}