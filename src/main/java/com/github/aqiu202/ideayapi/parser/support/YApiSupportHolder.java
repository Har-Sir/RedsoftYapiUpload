package com.github.aqiu202.ideayapi.parser.support;

import com.github.aqiu202.ideayapi.parser.support.swagger.YApiSwaggerSupport;

/**
 * 当前支持的所有扩展
 */
public interface YApiSupportHolder {

    YApiSupport supports = new YApiSupports(YApiSwaggerSupport.INSTANCE);
}
