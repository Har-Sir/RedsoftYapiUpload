package com.github.aqiu202.ideayapi.parser.impl;

import com.github.aqiu202.ideayapi.config.xml.YApiProjectProperty;
import com.github.aqiu202.ideayapi.mode.json5.Json;
import com.github.aqiu202.ideayapi.mode.json5.JsonArray;
import com.github.aqiu202.ideayapi.mode.json5.JsonItem;
import com.github.aqiu202.ideayapi.mode.json5.JsonObject;
import com.github.aqiu202.ideayapi.model.FieldValueWrapper;
import com.github.aqiu202.ideayapi.parser.Json5JsonParser;
import com.github.aqiu202.ideayapi.parser.Jsonable;
import com.github.aqiu202.ideayapi.parser.ObjectRawParser;
import com.github.aqiu202.ideayapi.parser.abs.AbstractJsonParser;
import com.github.aqiu202.ideayapi.util.TypeUtils;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiType;
import com.jgoodies.common.base.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <b>json5解析器默认实现</b>
 *
 * @author aqiu 2020/7/24 9:56 上午
 **/
public class Json5ParserImpl extends AbstractJsonParser implements Json5JsonParser,
        ObjectRawParser {

    private final boolean needDesc;

    public Json5ParserImpl(YApiProjectProperty property, Project project) {
        this(property, project, true);
    }

    public Json5ParserImpl(YApiProjectProperty property, Project project, boolean needDesc) {
        super(property, project);
        this.needDesc = needDesc;
    }

    public Json5ParserImpl(Project project) {
        this(project, true);
    }

    public Json5ParserImpl(Project project, boolean needDesc) {
        super(project);
        this.needDesc = needDesc;
    }

    @Override
    public Json<?> parseJson5(String typePkName, List<String> ignores) {
        return (Json<?>) super.parse(typePkName, ignores);
    }

    @Override
    public Jsonable parseBasic(String typePkName) {
        return new Json<>(TypeUtils.getDefaultValueByPackageName(typePkName));
    }

    @Override
    public JsonObject parseMap(String typePkName, String description) {
        return new JsonObject(new JsonItem<>("key", new Json<>("value"), description));
    }

    @Override
    public JsonArray<?> parseCollection(String typePkName, List<String> ignores) {
        if (Strings.isBlank(typePkName)) {
            return new JsonArray<>();
        }
        return new JsonArray<>(this.parseJson5(typePkName, ignores));
    }

    @Override
    public String getRawResponse(PsiType psiType) {
        return this.parse(psiType.getCanonicalText(), new ArrayList<>()).toJson();
    }

    @Override
    protected boolean needDescription() {
        return this.needDesc;
    }

    @Override
    public Jsonable buildPojo(Collection<FieldValueWrapper> wrappers) {
        JsonObject jsonObject = new JsonObject();
        for (FieldValueWrapper wrapper : wrappers) {
            final Jsonable value = wrapper.getValue();
            if (value instanceof Json) {
                // 字段备注
                String desc = wrapper.getDescription();
                if (Strings.isBlank(desc)) {
                    desc = "";
                }
                if (value instanceof JsonObject) {
                    Collection<JsonItem<?>> items;
                    if (!(items = ((JsonObject) value).getValue()).isEmpty()) {
                        JsonItem<?> item = items.iterator().next();
                        // 类型备注
                        String description = item.getDescription();
                        if (Strings.isNotBlank(description)) {
                            desc += description;
                        }
                    }
                }
                jsonObject.addItem(new JsonItem<>(wrapper.getFieldName(), (Json<?>) value, desc));
            }
        }
        return jsonObject;
    }
}