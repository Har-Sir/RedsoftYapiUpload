package com.github.aqiu202.ideayapi.parser.impl;

import com.github.aqiu202.ideayapi.config.xml.YApiProjectProperty;
import com.github.aqiu202.ideayapi.mode.schema.*;
import com.github.aqiu202.ideayapi.mode.schema.base.ItemJsonSchema;
import com.github.aqiu202.ideayapi.mode.schema.base.SchemaType;
import com.github.aqiu202.ideayapi.model.FieldValueWrapper;
import com.github.aqiu202.ideayapi.model.range.DecimalRange;
import com.github.aqiu202.ideayapi.model.range.IntegerRange;
import com.github.aqiu202.ideayapi.model.range.LongRange;
import com.github.aqiu202.ideayapi.parser.JsonSchemaJsonParser;
import com.github.aqiu202.ideayapi.parser.abs.AbstractJsonParser;
import com.github.aqiu202.ideayapi.util.TypeUtils;
import com.github.aqiu202.ideayapi.util.ValidUtils;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.jgoodies.common.base.Strings;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <b>json-schema解析器默认实现</b>
 *
 * @author aqiu 2020/7/24 9:56 上午
 **/
public class JsonSchemaParserImpl extends AbstractJsonParser implements JsonSchemaJsonParser {

    public JsonSchemaParserImpl(YApiProjectProperty property, Project project) {
        super(property, project);
        this.enableBasicScope = property.isEnableBasicScope();
    }

    public JsonSchemaParserImpl(boolean enableBasicScope, Project project) {
        super(null, project);
        this.enableBasicScope = enableBasicScope;
    }

    private final boolean enableBasicScope;


    @Override
    public ItemJsonSchema parseJsonSchema(String typePkName, List<String> ignores) {
        return (ItemJsonSchema) super.parse(typePkName, ignores);
    }

    @Override
    public ItemJsonSchema parseBasic(String typePkName) {
        ItemJsonSchema result = SchemaHelper
                .parseBasic(TypeUtils.getBasicSchema(typePkName));
        result.setDefault(TypeUtils.getDefaultValueByPackageName(typePkName).toString());
        result.setMock(TypeUtils
                .formatMockType(typePkName.substring(typePkName.lastIndexOf(".") + 1)));
        return result;
    }

    @Override
    public ObjectSchema parseMap(String typePkName, String description) {
        ObjectSchema objectSchema = new ObjectSchema();
        objectSchema.setDescription(description);
        return objectSchema;
    }

    @Override
    public ArraySchema parseCollection(String typePkName, List<String> ignores) {
        ArraySchema result = new ArraySchema();
        if (Strings.isBlank(typePkName)) {
            return result.setItems(new ObjectSchema());
        }
        return result.setItems(this.parseJsonSchema(typePkName, ignores));
    }

    @Override
    public ItemJsonSchema buildPojo(Collection<FieldValueWrapper> wrappers) {
        ObjectSchema objectSchema = new ObjectSchema();
        for (FieldValueWrapper wrapper : wrappers) {
            String fieldName = wrapper.getFieldName();
            ItemJsonSchema value = (ItemJsonSchema) wrapper.getValue();
            // 字段备注
            String desc = wrapper.getDescription();
            if (Strings.isBlank(desc)) {
                desc = "";
            }
            // 类型备注
            String description = value.getDescription();
            if (Strings.isNotBlank(description)) {
                desc += description;
            }
            objectSchema.addProperty(fieldName,
                    ((ItemJsonSchema) wrapper.getValue()).setDescription(desc));
            if (ValidUtils.notNullOrBlank(wrapper.getField())) {
                objectSchema.addRequired(fieldName);
            }
        }
        return objectSchema;
    }

    @Override
    public ItemJsonSchema parseFieldValue(PsiField psiField, List<String> ignores) {
        PsiType type = psiField.getType();
        String typePkName = type.getCanonicalText();
        ItemJsonSchema itemJsonSchema;
        if (TypeUtils.isBasicType(typePkName)) {
            itemJsonSchema = this.parseBasicField(psiField);
        } else {
            itemJsonSchema = this.parseCompoundField(psiField, ignores);
        }
        return itemJsonSchema;
    }

    private ItemJsonSchema parseBasicField(PsiField psiField) {
        PsiType psiType = psiField.getType();
        String typePkName = psiType.getCanonicalText();
        ItemJsonSchema result;
        SchemaType schemaType = TypeUtils.getBasicSchema(typePkName);
        switch (schemaType) {
            case number:
                NumberSchema numberSchema = new NumberSchema();
                DecimalRange decimalRange = ValidUtils.rangeDecimal(psiField);
                if (Objects.nonNull(decimalRange)) {
                    numberSchema.setRange(decimalRange);
                }
                if (ValidUtils.isPositive(psiField)) {
                    numberSchema.setMinimum(new BigDecimal("0"));
                    numberSchema.setExclusiveMinimum(true);
                }
                if (ValidUtils.isPositiveOrZero(psiField)) {
                    numberSchema.setMinimum(new BigDecimal("0"));
                }
                if (ValidUtils.isNegative(psiField)) {
                    numberSchema.setMaximum(new BigDecimal("0"));
                    numberSchema.setExclusiveMaximum(true);
                }
                if (ValidUtils.isNegativeOrZero(psiField)) {
                    numberSchema.setMaximum(new BigDecimal("0"));
                }
                result = numberSchema;
                break;
            case integer:
                IntegerSchema integerSchema = new IntegerSchema();
                if (TypeUtils.hasBaseRange(typePkName)) {
                    if (this.enableBasicScope) {
                        integerSchema.setRange(TypeUtils.getBaseRange(typePkName));
                    }
                }
                LongRange longRange = ValidUtils.range(psiField, this.enableBasicScope);
                if (Objects.nonNull(longRange)) {
                    integerSchema.setRange(longRange);
                }
                if (ValidUtils.isPositive(psiField)) {
                    integerSchema.setMinimum(0L);
                    integerSchema.setExclusiveMinimum(true);
                }
                if (ValidUtils.isPositiveOrZero(psiField)) {
                    integerSchema.setMinimum(0L);
                }
                if (ValidUtils.isNegative(psiField)) {
                    integerSchema.setMinimum(0L);
                    integerSchema.setExclusiveMaximum(true);
                }
                if (ValidUtils.isNegativeOrZero(psiField)) {
                    integerSchema.setMinimum(0L);
                }
                result = integerSchema;
                break;
            case string:
                StringSchema stringSchema = new StringSchema();
                IntegerRange integerRange = ValidUtils
                        .rangeLength(psiField, this.enableBasicScope);
                stringSchema.setMinLength(integerRange.getMin());
                stringSchema.setMaxLength(integerRange.getMax());
                String pattern = ValidUtils.getPattern(psiField);
                if (!Strings.isEmpty(pattern)) {
                    stringSchema.setPattern(pattern);
                }
                result = stringSchema;
                break;
            case bool:
                result = new BooleanSchema();
                break;
            default:
                return new StringSchema();
        }
        result.setDefault(TypeUtils.getDefaultValueByPackageName(typePkName).toString());
        result.setMock(TypeUtils.formatMockType(psiType.getPresentableText()));
        return result;
    }

    private ItemJsonSchema parseCompoundField(PsiField psiField, List<String> ignores) {
        PsiType psiType = psiField.getType();
        String typeName = psiType.getPresentableText();
        boolean wrapArray = typeName.endsWith("[]");
        ItemJsonSchema result = this.parseJsonSchema(psiType.getCanonicalText(), ignores);
        if (result instanceof ArraySchema) {
            ArraySchema a = (ArraySchema) result;
            if (typeName.contains("Set") && !wrapArray) {
                a.setUniqueItems(true);
            }
            if (ValidUtils.notEmpty(psiField)) {
                a.setMinItems(1);
            }
            IntegerRange integerRange = ValidUtils
                    .rangeSize(psiField, this.enableBasicScope);
            a.setMinItems(integerRange.getMin(), this.enableBasicScope);
            a.setMaxItems(integerRange.getMax(), this.enableBasicScope);
        }
        return result;
    }

}