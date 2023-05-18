package com.qijin.utils;

import cn.afterturn.easypoi.excel.entity.result.ExcelVerifyHandlerResult;
import cn.afterturn.easypoi.handler.inter.IExcelVerifyHandler;

/**
 * 拓展easy poi 的校验逻辑
 * 主要为过滤空行数据
 * @param <T>
 * @author zrl
 */
public class ExcelExtVerifyHandler<T> implements IExcelVerifyHandler<T> {

    @Override
    public ExcelVerifyHandlerResult verifyHandler(T obj) {
        ExcelVerifyHandlerResult result = new ExcelVerifyHandlerResult(true);
        result.setSuccess(!FieldUtils.checkObjAllFieldsIsNull(obj));
        return result;
    }
}
