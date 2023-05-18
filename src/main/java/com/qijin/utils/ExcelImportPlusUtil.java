package com.qijin.utils;


import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ImportParams;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据导入解析工具类
 * @param <T>
 * @author zrl
 */
public class ExcelImportPlusUtil<T> {

    /**
     * zip压缩炸弹预警信息
     */
    private static final String ZIP_BOMB_MSG = "Zip bomb detected";

    /**
     * 根据文件解析数据
     * @param excelFile 文件
     * @param clazz 数据类型
     * @param <T>
     * @return
     */
    public static <T> List<T> importExcel(File excelFile, Class<T> clazz){
        return importExcel(excelFile, clazz, null);
    }

    /**
     * 根据文件解析数据
     * @param excelFile 文件
     * @param clazz 数据类型
     * @param params 导入参数设置
     * @param <T>
     * @return
     */
    public static <T> List<T> importExcel(File excelFile, Class<T> clazz, ImportParams params){

        if(null == excelFile){
            throw new RuntimeException("excel文件不能为空！");
        }

        try(FileInputStream fis = new FileInputStream(excelFile)){
            return importExcel(fis, clazz, params);
        }catch (FileNotFoundException e){
            throw new RuntimeException("excel文件不存在！");
        } catch (IOException e) {
            throw new RuntimeException("excel文件解析出错！");
        }
    }

    /**
     * 根据流解析导入数据
     * @param is 流
     * @param clazz 数据类型
     * @param <T>
     * @return
     */
    public static <T> List<T> importExcel(InputStream is, Class<T> clazz){
        return importExcel(is, clazz, null);
    }

    /**
     * 根据流解析导入数据
     * @param is 流
     * @param clazz 数据类型
     * @param params 导入参数设置
     * @param <T>
     * @return
     */
    public static <T> List<T> importExcel(InputStream is, Class<T> clazz, ImportParams params){

        if(null == params){
            params = new ImportParams();
        }

        List<T> list = new ArrayList<>();
        try{
            ExcelExtVerifyHandler<T> verifyHandler = new ExcelExtVerifyHandler<>();

            params.setVerifyHandler(verifyHandler);
            list = ExcelImportUtil.importExcel(is, clazz, params);
        }catch (Exception e){
            if(e.getMessage().contains(ZIP_BOMB_MSG)){
                throw new RuntimeException("文件空行过多！");
            }
        }
        return list;
    }
}
