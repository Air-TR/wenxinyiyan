package com.tr.wenxinyiyan.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.model.chat.ChatResponse;
import com.baidubce.qianfan.model.chat.Message;
import com.tr.wenxinyiyan.constant.QianfanConst;
import com.tr.wenxinyiyan.constant.RedisKey;
import com.tr.wenxinyiyan.kit.JwtKit;
import com.tr.wenxinyiyan.kit.StringKit;
import com.tr.wenxinyiyan.service.QianfanService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: TR
 */
@Service
public class QianfanServiceImpl implements QianfanService {

    @Value("${qianfan.api-key}")
    private String apiKey;
    @Value("${qianfan.secret-key}")
    private String secretKey;

    @Autowired
    private Qianfan qianfan;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final OkHttpClient okHttpClient = new OkHttpClient();


    /**
     * 文心一言 access_token，默认有效期 30 天，生产环境注意及时刷新
     */
    @Override
    public String getAccessToken() {
        HttpResponse response = HttpRequest.post(String.format("https://aip.baidubce.com/oauth/2.0/token?client_id=%s&client_secret=%s&grant_type=client_credentials", apiKey, secretKey)).execute();
        JSONObject json = JSONObject.parseObject(response.body());
        String accessToken = json.getString(QianfanConst.ACCESS_TOKEN);
        stringRedisTemplate.opsForValue().set(RedisKey.QIAN_FAN_ACCESS_TOKEN, accessToken, 10, TimeUnit.DAYS); // 默认有效期 30 天
        return accessToken;
    }

    /**
     * 单轮对话，HTTP 调用
     */
    @Override
    public String sendMessage(String content) {
        // redis 获取文心一言 access_token
        String accessToken = stringRedisTemplate.opsForValue().get(RedisKey.QIAN_FAN_ACCESS_TOKEN);

        // 没有 access_token，调用 getAccessToken() 获取 access_token 并存入 redis
        if (StringKit.isBlank(accessToken)) {
            accessToken = getAccessToken();
        }

        // 组装消息请求体
        Message userMsg = new Message();
        userMsg.setRole(QianfanConst.USER);
        userMsg.setContent(content);

        // 历史消息
        List<Message> messages = new ArrayList<>();
        String messagesStr = stringRedisTemplate.opsForValue().get(RedisKey.QIAN_FAN_MESSAGES + JwtKit.getUsername());
        if (StringKit.isNotBlank(messagesStr)) {
            messages = JSONArray.parseArray(messagesStr, Message.class);
        }
        messages.add(userMsg);

        // 文心一言请求 json 参数封装
        JSONObject sendJson = new JSONObject();
        sendJson.put("messages", messages);

        // 调用文心一言接口
        HttpResponse response = HttpRequest.post(String.format("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions_pro?access_token=%s", accessToken))
                .header("Content-Type", "application/json")
                .body(sendJson.toJSONString())
                .execute();

        if (!response.isOk()) {
            throw new RuntimeException("Ai 服务错误");
        }

        JSONObject replyJson = JSONObject.parseObject(response.body());

        // Ai 答复加入 messages
        Message assistantMsg = new Message();
        assistantMsg.setRole(QianfanConst.ASSISTANT);
        assistantMsg.setContent(replyJson.getString(QianfanConst.RESULT));
        messages.add(assistantMsg);

        // 保存历史消息到 redis（保留最近 10 条对话记录，保留 1 天）
        saveMessages(messages);

        return replyJson.getString(QianfanConst.RESULT);
    }

    @Override
    public Flux<String> sendMessageStream(String content) throws IOException {

        String username = JwtKit.getUsername();

        // Ai 回答流数据拼接汇总
        StringBuilder stringBuilder = new StringBuilder();

        // 用户消息
        Message userMsg = new Message();
        userMsg.setRole(QianfanConst.USER);
        userMsg.setContent(content);

        // 历史消息
        List<Message> messages = new ArrayList<>();
        String messagesStr = stringRedisTemplate.opsForValue().get(RedisKey.QIAN_FAN_MESSAGES + username);
        if (StringKit.isNotBlank(messagesStr)) {
            messages = JSONArray.parseArray(messagesStr, Message.class);
        }
        messages.add(userMsg);

        // 千帆平台请求参数
        JSONObject sendJson = new JSONObject();
        sendJson.put("messages", messages);
        sendJson.put("stream", true);

        // 封装 request 请求
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, sendJson.toJSONString());
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions_pro?access_token=24.ac6a6bbd883eab48a1a645973fc7ce36.2592000.1717910022.282335-51941640")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();

        // 发起流式响应接口调用
        Response response = okHttpClient.newCall(request).execute();
        ResponseBody responseBody = response.body();

        // 处理响应流
        if (responseBody != null) {
            BufferedReader reader = new BufferedReader(responseBody.charStream());
            List<Message> historyMessages = messages;
            return Flux.<String>generate(sink -> { // 流式响应式接口
                try {
                    String line = reader.readLine();
                    if (null != line) {
                        line = line.replace("data: ", "");
                        JSONObject json = JSON.parseObject(line);
                        if (null != json && StringKit.isNotBlank((String) json.get("result"))) {
                            sink.next(json.getString("result")); // 返回答案
                            stringBuilder.append(json.getString("result"));
                            if ((Boolean) json.get("is_end")) {
                                sink.complete(); // 结束响应
                            }
                        } else if (null != json && (Boolean) json.get("is_end")) {
                            sink.complete(); // 结束响应
                        } else {
                            sink.next("\u200B"); // 返回空白字符
                        }
                    }
                } catch (IOException e) {
                    sink.error(e);
                }
            }).doFinally(signalType -> {
                try {
                    // Ai 回答加入 messages
                    Message assistantMsg = new Message();
                    assistantMsg.setRole(QianfanConst.ASSISTANT);
                    assistantMsg.setContent(stringBuilder.toString());
                    historyMessages.add(assistantMsg);

                    // 保存历史消息到 redis（保留最近 10 条对话记录，保留 1 天）
                    saveMessages(historyMessages, username);

                    reader.close();
                    responseBody.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        return Flux.empty();
    }

    /**
     * 流式输出，Java-SDK 调用
     */
    @Override
    public String sendMessageStreamJavaSDK(String content) {
        List<ChatResponse> results = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();

        // 用户消息
        Message userMsg = new Message();
        userMsg.setRole(QianfanConst.USER);
        userMsg.setContent(content);

        // 历史消息
        List<Message> messages = new ArrayList<>();
        String messagesStr = stringRedisTemplate.opsForValue().get(RedisKey.QIAN_FAN_MESSAGES + JwtKit.getUsername());
        if (StringKit.isNotBlank(messagesStr)) {
            messages = JSONArray.parseArray(messagesStr, Message.class);
        }
        messages.add(userMsg);

        // 调用千帆平台
        qianfan.chatCompletion()
                // 指定模型（指定了高版本模型，会慢，低版本模型快，但是回答不如高版本模型丰富）
                .model("ERNIE-4.0-8K") // 直接注释掉，会使用低版本模型
                .messages(messages)
                // 启用流式返回
                .executeStream()
                .forEachRemaining(chunk -> {
                    results.add(chunk);
                    stringBuilder.append(chunk.getResult());
                });

        // Ai 助手答复消息
        Message assistantMsg = new Message();
        assistantMsg.setRole(QianfanConst.ASSISTANT);
        assistantMsg.setContent(stringBuilder.toString());
        messages.add(assistantMsg);

        // 保存历史消息到 redis（保留最近 10 条对话记录，保留 1 天）
        saveMessages(messages);

        return stringBuilder.toString();
    }

    /**
     * 对话消息存入 Redis（保留最近 10 条，有效期 1 天）
     */
    private void saveMessages(List<Message> messages) {
        saveMessages(messages, JwtKit.getUsername());
    }

    /**
     * 解决在 doFinally 中获取不到 token
     */
    private void saveMessages(List<Message> messages, String username) {
        // 保留最后 10 条对话消息
        if (messages.size() > 10) {
            messages = messages.subList(messages.size() - 10, messages.size());
        }
        // 对话存入 redis（保留最近 10 条，有效期 1 天）
        stringRedisTemplate.opsForValue().set(RedisKey.QIAN_FAN_MESSAGES + username, JSONArray.toJSONString(messages), 1, TimeUnit.DAYS);
    }

}