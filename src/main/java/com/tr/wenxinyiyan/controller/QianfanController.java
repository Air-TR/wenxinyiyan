package com.tr.wenxinyiyan.controller;

import com.tr.wenxinyiyan.service.QianfanService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;

/**
 * @Author: TR
 */
@Api(tags = "千帆（文心一言）")
@RestController
public class QianfanController {

    @Autowired
    private QianfanService qianfanService;

    @GetMapping("/qianfan/accessToken")
    public String getAccessToken() {
        return qianfanService.getAccessToken();
    }

    @GetMapping("/qianfan/sendMessage")
    public String sendMessage(@RequestParam String content) {
        return qianfanService.sendMessage(content);
    }

    @GetMapping(value = "/qianfan/sendMessage/stream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<String> sendMessageStream(@RequestParam String content) throws IOException {
        return qianfanService.sendMessageStream(content);
    }

    @GetMapping("/qianfan/sendMessage/stream/java-sdk")
    public String sendMessageStreamJavaSDK(@RequestParam String content) {
        return qianfanService.sendMessageStreamJavaSDK(content);
    }

}
