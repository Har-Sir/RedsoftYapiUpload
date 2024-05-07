package com.github.aqiu202.ideayapi.util;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Har
 * @version 1.0.0
 * @title PropertiesReader
 * @description 配置读取
 * @create 2024/5/6 17:59
 **/
public class PropertiesReader {

    private static final Logger log = LoggerFactory.getLogger(PropertiesReader.class);
    private static final Map<String, String>  properties = new ConcurrentHashMap<>();


    /**
     * 读取配置文件
     * @param path 路径
     * @return 键值对
     */
    public static Map<String, String> read(String path) {
        Map<String,String> map = new HashMap<>();
        File file = new File(path);
        try {
            if(file.exists() && file.isFile()){

                String content = FileUtils.readFileToString(file, "UTF-8");
                String[] lines = content.split("\n");
                for (String line : lines) {
                    String[] split = line.split("=");
                    if (split != null && split.length == 2) {
                        map.put(split[0], split[1]);
                    }
                }
            }
        } catch (Exception e) {
            log.error("读取配置文件失败", e);
        }
        return map;
    }
}
