package com.tr.wenxinyiyan.service;

import reactor.core.publisher.Flux;

import java.io.IOException;

public interface QianfanService {

    String getAccessToken();

    String sendMessage(String content);

    Flux<String> sendMessageStream(String content) throws IOException;

    String sendMessageStreamJavaSDK(String content);

}
