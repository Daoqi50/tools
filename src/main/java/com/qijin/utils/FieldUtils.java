package com.qijin.utils;

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * 对象字段工具类
 */
public class FieldUtils {

    /**
     * 校验字段是否全部为空
     * @param object
     * @return
     */
    public static boolean checkObjAllFieldsIsNull(Object object) {
        if (null == object) {
            return true;
        }

        try {
            for (Field f : object.getClass().getDeclaredFields()) {
                f.setAccessible(true);

                if (f.get(object) != null && StringUtils.isNotBlank(f.get(object).toString())) {
                    return false;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }


    /**
     * 修改字段的必须值
     * @param clazz
     * @param fieldName
     * @param required
     */
    public static void setFieldRequiredProperty(Class<?> clazz, String fieldName, boolean required){
        try {
            Field field = clazz.getDeclaredField(fieldName);
            if(null == field){
                return;
            }
            field.setAccessible(true);
            ApiModelProperty apiModelProperty = field.getAnnotation(ApiModelProperty.class);
            if(null == apiModelProperty){
                return;
            }
            System.out.println("修改前的值：" + apiModelProperty.required());
            InvocationHandler handler = Proxy.getInvocationHandler(apiModelProperty);
            Field requiredField = handler.getClass().getDeclaredField("memberValues");
            // 因为这个字段事 private final 修饰，所以要打开权限
            requiredField.setAccessible(true);
            // 获取 memberValues
            Map values = (Map) requiredField.get(handler);
            values.put("required", required);

            System.out.println("修改后的值：" + apiModelProperty.required());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
