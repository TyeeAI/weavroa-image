package com.eniac.weavroa.util.string;

public class JsonUtil {


    public static String cleanResult(String result) {
        if (result == null) {
            return null;
        }

        // 去掉可能的 JSON 引号包装
        result = result.trim();

        // 如果是 "\"xxx\"" 这种情况
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length() - 1);
        }

        return result;
    }

    /**
     * 清理AI返回的JSON字符串（去掉 ```json 和 ```）
     */
    public static String cleanJson(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String result = content.trim();

        // 去掉 ```json 开头
        if (result.startsWith("```json")) {
            result = result.substring(7);
        }

        // 去掉 ``` 开头（兼容 ```json 或 ```）
        if (result.startsWith("```")) {
            result = result.substring(3);
        }

        // 去掉结尾 ```
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3);
        }

        return result.trim();
    }
}