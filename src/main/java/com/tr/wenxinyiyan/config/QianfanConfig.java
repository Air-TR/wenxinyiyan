package com.tr.wenxinyiyan.config;

import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.core.auth.Auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: TR
 */
@Configuration
public class QianfanConfig {

    @Value("${qianfan.api-key}")
    private String apiKey;
    @Value("${qianfan.secret-key}")
    private String secretKey;

    @Bean
    public Qianfan getQianfan() {
        return new Qianfan(Auth.TYPE_OAUTH, apiKey, secretKey);
    }

}