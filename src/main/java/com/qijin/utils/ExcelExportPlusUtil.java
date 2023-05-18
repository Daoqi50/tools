package com.qijin.utils;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.excel.annotation.ExcelCollection;
import cn.afterturn.easypoi.excel.annotation.ExcelEntity;
import cn.afterturn.easypoi.excel.annotation.ExcelTarget;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qijin.annotation.Dict;
import com.qijin.vo.BaseVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.format.annotation.DateTimeFormat;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author zhuqijin
 * @Date 15:36 2023/3/3
 */
public class ExcelExportPlusUtil {

    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /***
     * 根据字段名串和字段标题串导出Excel
     * @param datalist  数据列表
     * @param fieldNames   字段名串,逗号分隔
     * @param fieldTitles   字段标题串,逗号分隔
     * @param fileName       文件名
     * @param cls            数据的class
     * @param request        请求体
     * @param response       响应体
     * @throws IOException
     * @throws NoSuchFieldException
     */
    public static <T> void export(List<T> datalist, String fieldNames, String fieldTitles, String fileName, Class<T> cls, HttpServletRequest request, HttpServletResponse response) throws IOException, NoSuchFieldException, InstantiationException, IllegalAccessException {

        List<ExcelExportEntity> exportEntityList = new ArrayList<ExcelExportEntity>();

        if (Strings.isBlank(fieldNames) || Strings.isBlank(fieldTitles)) {
            throw new RuntimeException("字段名称和字段标题不能为空");
        }
        //字段串处理
        String[] fieldNameArray = fieldNames.split(",");
        String[] fieldTitleArray = fieldTitles.split(",");
        if (fieldNameArray.length != fieldTitleArray.length) {
            throw new RuntimeException("字段名称和字段标题数量不一致");
        }

        for (int i = 0; i < fieldNameArray.length; i++) {
            String fieldName = fieldNameArray[i];
            String fieldTitle = fieldTitleArray[i];

            Field firstField = findFirstField(cls, fieldName);

            if (firstField.getType().equals(List.class)) {
                final Field listField = firstField;
                ExcelExportEntity ListExportEntity = exportEntityList.stream().filter(x -> x.getKey().toString().equals(listField.getName())).findFirst().orElse(null);
                if (ListExportEntity == null) {
                    ListExportEntity = new ExcelExportEntity("明细", listField.getName());
                    exportEntityList.add(ListExportEntity);
                }
                String childFieldName = fieldName.replace(listField.getName() + ".", "");
                ExcelExportEntity childExportEntity = new ExcelExportEntity(fieldTitle, childFieldName);
                Type type = listField.getGenericType();
                Class detailClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
                Field childField = findLastField(detailClass, childFieldName);
                if (childField.getType().equals(Date.class)) {
                    String pattern = "yyyy-MM-dd HH:mm:ss";
                    DateTimeFormat format = childField.getAnnotation(DateTimeFormat.class);
                    if (null != format && StringUtils.isNotEmpty(format.pattern())) {
                        pattern = format.pattern();
                    }
                    childExportEntity.setFormat(pattern);
                }

                List<ExcelExportEntity> childExportEntityList = ListExportEntity.getList();
                if (childExportEntityList == null) {
                    childExportEntityList = new ArrayList<>();
                    ListExportEntity.setList(childExportEntityList);
                }
                childExportEntityList.add(childExportEntity);
            } else {
                ExcelExportEntity excelExportEntity = new ExcelExportEntity(fieldTitle.trim(), fieldName);
                excelExportEntity.setNeedMerge(true);
                Field field = findLastField(cls, fieldName);
                if (field.getType().equals(Date.class)) {
                    String pattern = "yyyy-MM-dd HH:mm:ss";
                    DateTimeFormat format = field.getAnnotation(DateTimeFormat.class);
                    if (null != format && StringUtils.isNotEmpty(format.pattern())) {
                        pattern = format.pattern();
                    }
                    excelExportEntity.setFormat(pattern);
                }

                exportEntityList.add(excelExportEntity);
            }
        }
        List<Map<String, Object>> mapList = listToMapList(datalist, cls);
        writeResponse(response, exportEntityList, mapList, fileName);
    }


    /***
     * 根据注解Excel或者ApiModelProperty导出Excel文件,ExcelTarget注解存在时应用Excel,否则应用ApiModelProperty
     * @param dataList  数据列表
     * @param cls            数据的class
     * @param request        请求体
     * @param response       响应体
     * @throws IOException
     * @throws NoSuchFieldException
     */
    public static <T> void export(List<T> dataList, Class<T> cls, HttpServletRequest request, HttpServletResponse response) throws IOException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        List<ExcelExportEntity> exportEntityList = new ArrayList<ExcelExportEntity>();
        //判断是否存在  ExcelTarget 注解
        boolean existExcelAnnotation = cls.getAnnotation(ExcelTarget.class) != null;
        //文件名处理
        String fileName = "";
        if (existExcelAnnotation) {  //应用Excel注解
            ExcelTarget excelTarget = (ExcelTarget) cls.getAnnotation(ExcelTarget.class);
            fileName = excelTarget.value();
            addExportEntityByExcel(exportEntityList, cls, null);
            removeSpecifyField(exportEntityList);
        } else { //应用ApiModel
            ApiModel apiModel = (ApiModel) cls.getAnnotation(ApiModel.class);
            fileName = apiModel.description();
            addExportEntityByApi(exportEntityList, cls, null);
            removeSpecifyField(exportEntityList);
            removeNullValueField(exportEntityList, dataList);
        }
        List<Map<String, Object>> mapList = listToMapList(dataList, cls);
        writeResponse(response, exportEntityList, mapList, fileName);
    }


    /***
     * 根据注解Excel或者ApiModelProperty导出Excel文件,ExcelTarget注解存在时应用Excel,否则应用ApiModelProperty
     * @param dataList  数据列表
     * @param cls            数据的class
     * @param request        请求体
     * @param response       响应体
     * @throws IOException
     * @throws NoSuchFieldException
     */
    public static <T> void exportByFileName(List<T> dataList, Class<T> cls, HttpServletRequest request, HttpServletResponse response,String fileName) throws IOException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        List<ExcelExportEntity> exportEntityList = new ArrayList<ExcelExportEntity>();
        //判断是否存在  ExcelTarget 注解
        boolean existExcelAnnotation = cls.getAnnotation(ExcelTarget.class) != null;
        //文件名处理
        if (existExcelAnnotation) {  //应用Excel注解
            addExportEntityByExcel(exportEntityList, cls, null);
            removeSpecifyField(exportEntityList);
        } else { //应用ApiModel
            addExportEntityByApi(exportEntityList, cls, null);
            removeSpecifyField(exportEntityList);
            removeNullValueField(exportEntityList, dataList);
        }
        List<Map<String, Object>> mapList = listToMapList(dataList, cls);
        writeResponse(response, exportEntityList, mapList, fileName);
    }

    public static <T> void export2OutputStream(List<T> dataList, Class<T> cls, OutputStream outputStream, String sheetName) throws IOException, NoSuchFieldException, IllegalAccessException, InstantiationException {
        List<ExcelExportEntity> exportEntityList = new ArrayList<ExcelExportEntity>();
        //判断是否存在  ExcelTarget 注解
        boolean existExcelAnnotation = cls.getAnnotation(ExcelTarget.class) != null;
        //文件名处理
        if (existExcelAnnotation) {  //应用Excel注解
            addExportEntityByExcel(exportEntityList, cls, null);
            removeSpecifyField(exportEntityList);
        } else { //应用ApiModel
            addExportEntityByApi(exportEntityList, cls, null);
            removeSpecifyField(exportEntityList);
            removeNullValueField(exportEntityList, dataList);
        }
        List<Map<String, Object>> mapList = listToMapList(dataList, cls);
        ExportParams exportParams = new ExportParams(null, sheetName, ExcelType.XSSF);
        exportParams.setStyle(ExcelExportXSSFStylerImpl.class);
        Workbook workbook = ExcelExportUtil.exportExcel(exportParams, exportEntityList, (Collection<? extends Map<?, ?>>) dataList);
        workbook.write(outputStream);
    }


    static List<Map<String, Object>> listToMapList(List dataList, Class cls) throws IllegalAccessException, InstantiationException {

        List<Map<String, Object>> list = new ArrayList<>();

        if (CollectionUtils.isEmpty(dataList) || dataList.get(0) == null) {
            return list;
        }

        List<Field> fieldList = getAllUsedField(cls);

        dataList.forEach(item -> {
            Map<String, Object> map = new HashMap<>();
            fieldList.forEach(field -> {
                try {

                    Object value = getValueByField(item, field.getName());

                    ExcelEntity excelEntity = field.getAnnotation(ExcelEntity.class);
                    ExcelCollection excelCollection = field.getAnnotation(ExcelCollection.class);

                    if (excelEntity != null || BaseVO.class.isAssignableFrom(field.getType())) {
//                        if (Strings.isNotBlank(excelEntity.name())) {
//                            String[] fieldNameArray = excelEntity.name().split(",");
//                            for (String fieldName : fieldNameArray) {
//                                Object  fieldValue = findValue(value,fieldName);
//                                map.put(field.getName() + "." + fieldName, fieldValue);
//                            }
//                        } else
//                            {
                        List<Map<String, Object>> mapList = listToMapList(Arrays.asList(value), field.getType());
                        mapList.forEach(x -> {
                            x.entrySet().forEach(y -> {
                                map.put(field.getName() + "." + y.getKey(), y.getValue());
                            });
                        });
//                        }
                    } else if (excelCollection != null || field.getType().equals(List.class)) {
                        Type type = field.getGenericType();
                        Class detailClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];

                        List<Map<String, Object>> mapList = listToMapList((List) value, detailClass);
                        map.put(field.getName(), mapList);

                    } else {
                        if (field.getType().equals(Boolean.class)) {  //处理布尔类型

                            Boolean bo = (Boolean) value;
                            if (bo != null && bo) {
                                value = "是";
                            } else {
                                value = "否";
                            }
                        } else if (field.getType().equals(BigDecimal.class)) {
                            if (null != value) {
                                value = ((BigDecimal) value).stripTrailingZeros().toPlainString();
                            }
                        }
                        map.put(field.getName(), value);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            if (MapUtils.isNotEmpty(map)) {
                list.add(map);
            }
        });
        return list;
    }

    private static Object getValueByField(Object item, String name) throws IllegalAccessException {
//        try {
        return FieldUtils.readField(item, name, true);
//            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(name, item.getClass());
//            return propertyDescriptor.getReadMethod().invoke(item);
//        } catch (IntrospectionException | InvocationTargetException e) {
//            return FieldUtils.readField(item, name, true);
//        }
    }


    static List<Field> getAllUsedField(Class cls) {
        List<Field> fieldList = new ArrayList<>();
        Class tempClass = cls;
        while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
            fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
            tempClass = tempClass.getSuperclass(); //得到父类,然后赋给自己
        }

        return fieldList;
    }

    static Field getField(Class cls, String fieldName) {
        List<Field> fieldList = getAllUsedField(cls);

        Field field = fieldList.stream().filter(x -> x.getName().equals(fieldName)).findFirst().orElse(null);

        if (field == null) {
            throw new RuntimeException("不存在字段:" + fieldName);
        }
        return field;
    }


    static Field findFirstField(Class cls, String fieldName) throws NoSuchFieldException {
        String[] fieldSplitArray = fieldName.split("\\.");
        Field clsField = getField(cls, fieldSplitArray[0]);
        return clsField;
    }

    static Field findLastField(Class cls, String fieldName) throws NoSuchFieldException {

        String[] fieldSplitArray = fieldName.split("\\.");
        Class fieldClass = cls;
        for (int j = 0; j < fieldSplitArray.length - 1; j++) {
            fieldClass = getField(fieldClass, fieldSplitArray[j]).getType();
        }
        Field clsField = getField(fieldClass, fieldSplitArray[fieldSplitArray.length - 1]);
        return clsField;
    }

    static Object findValue(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {

        String[] fieldSplitArray = fieldName.split("\\.");
        Object value = object;
        for (int j = 0; j < fieldSplitArray.length; j++) {
            if (value == null) {
                break;
            }
            value = getValueByField(value, fieldSplitArray[j]);
        }
        return value;
    }


    static void addExportEntityByExcel(List<ExcelExportEntity> exportEntityList, Class cls, String parentFieldName) throws NoSuchFieldException {

        // Field[] fieldList = cls.getDeclaredFields();

        List<Field> fieldList = getAllUsedField(cls);
        for (Field field : fieldList) {
            ExcelEntity excelEntity = field.getAnnotation(ExcelEntity.class);
            ExcelCollection excelCollection = field.getAnnotation(ExcelCollection.class);
            if (excelEntity != null) {
                if (Strings.isNotBlank(excelEntity.name())) {
                    String[] fieldNameArray = excelEntity.name().split(",");
                    for (String fieldName : fieldNameArray) {
                        Field entityField = findLastField(field.getType(), fieldName);
                        Excel excel = entityField.getAnnotation(Excel.class);
                        if (excel != null) {
                            String finalFieldName = field.getName() + "." + fieldName;
                            if (Strings.isNotBlank(parentFieldName)) {
                                finalFieldName = parentFieldName + "." + finalFieldName;
                            }

                            ExcelExportEntity excelExportEntity = new ExcelExportEntity(excel.name().trim(), finalFieldName);
                            excelExportEntity.setOrderNum(Integer.valueOf(excel.orderNum()));
                            excelExportEntity.setWidth(excel.width());
                            excelExportEntity.setNeedMerge(true);
                            if (entityField.getType().equals(Date.class)) {
                                excelExportEntity.setFormat("yyyy-MM-dd HH:mm:ss");
                            }
                            exportEntityList.add(excelExportEntity);
                        }
                    }
                } else {
                    String name = field.getName();
                    if (Strings.isNotBlank(parentFieldName)) {
                        name = parentFieldName + "." + name;
                    }
                    addExportEntityByExcel(exportEntityList, field.getType(), name);
                }
            } else if (excelCollection != null) {
                ExcelExportEntity detailExportEntity = new ExcelExportEntity(excelCollection.name().trim(), field.getName());
                Type type = field.getGenericType();
                Class detailClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];//格式转换
                List<ExcelExportEntity> childList = new ArrayList<>();
                addExportEntityByExcel(childList, detailClass, null);
                detailExportEntity.setList(childList);
                exportEntityList.add(detailExportEntity);
            } else {
                Excel excel = field.getAnnotation(Excel.class);
                if (excel != null) {

                    String fieldName = field.getName();
                    if (Strings.isNotBlank(parentFieldName)) {
                        fieldName = parentFieldName + "." + fieldName;
                    }
                    ExcelExportEntity excelExportEntity = new ExcelExportEntity(excel.name().trim(), fieldName);
                    excelExportEntity.setOrderNum(Integer.valueOf(excel.orderNum()));
                    excelExportEntity.setWidth(excel.width());
                    excelExportEntity.setNeedMerge(true);
                    if (field.getType().equals(Date.class)) {
                        if(StringUtils.isBlank(excel.exportFormat())){
                            excelExportEntity.setFormat("yyyy-MM-dd HH:mm:ss");
                        }
                    }
                    exportEntityList.add(excelExportEntity);
                }
            }
        }
    }

    static void addExportEntityByApi(List<ExcelExportEntity> exportEntityList, Class cls, String parentFieldName) {

        //List<Field> fieldList = Arrays.asList(cls.getDeclaredFields());
        List<Field> fieldList = getAllUsedField(cls);
        //遍历字段
        fieldList.stream().forEach(x -> {
            //字段名处理
            String fieldName = x.getName();
            if (Strings.isNotBlank(parentFieldName)) {
                fieldName = parentFieldName + "." + fieldName;
            }

            if (BaseVO.class.isAssignableFrom(x.getType())) {
                addExportEntityByApi(exportEntityList, x.getType(), fieldName);
            } else if (x.getType().equals(List.class)) {

                Type type = x.getGenericType();
                Class detailClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
                List<ExcelExportEntity> detailExportEntityList = new ArrayList<>();
                addExportEntityByApi(detailExportEntityList, detailClass, parentFieldName);

                String fieldTitle = null;
                ApiModelProperty apiModelProperty = x.getAnnotation(ApiModelProperty.class);
                if (apiModelProperty != null) {
                    fieldTitle = apiModelProperty.value();
                }
                if (fieldTitle != null) {
                    ExcelExportEntity excelExportEntity = new ExcelExportEntity(fieldTitle.trim(), fieldName);
                    excelExportEntity.setNeedMerge(true);

                    excelExportEntity.setList(detailExportEntityList);
                    exportEntityList.add(excelExportEntity);
                }
            } else {
                String fieldTitle = null;
                ApiModelProperty apiModelProperty = x.getAnnotation(ApiModelProperty.class);
                Dict dictionaryField = x.getAnnotation(Dict.class);

                if (apiModelProperty != null && dictionaryField == null) {
                    fieldTitle = apiModelProperty.value();
                }
                if (fieldTitle != null) {

                    String title = StringUtils.substringBefore(fieldTitle, "表：").replace(";", "");
                    title = StringUtils.substringBefore(title, "表:").replace(";", "");
                    title = StringUtils.substringBefore(title, "dictCode：").replace(";", "");
                    title = StringUtils.substringBefore(title, "dictCode:").replace(";", "");

                    ExcelExportEntity excelExportEntity = new ExcelExportEntity(title.trim(), fieldName);
                    excelExportEntity.setNeedMerge(true);
                    if (x.getType().equals(Date.class)) {
                        excelExportEntity.setFormat(getApiDateFormat(x));
                    }else if (x.getType().equals(BigDecimal.class)){
                        excelExportEntity.setNumFormat("##########.##########");
                    }
                    exportEntityList.add(excelExportEntity);
                }
            }
        });
    }

    /**
     * 获取日期格式化
     * @param field
     * @return
     */
    static String getApiDateFormat(Field field){

        DateTimeFormat dateTimeFormat = field.getAnnotation(DateTimeFormat.class);
        if(null != dateTimeFormat){
            String pattern = dateTimeFormat.pattern();
            if(StringUtils.isNotBlank(pattern)){
                return pattern;
            }
        }

        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        if(null != jsonFormat){
            String pattern = jsonFormat.pattern();
            if(StringUtils.isNotBlank(pattern)){
                return pattern;
            }
        }

        return DEFAULT_DATE_TIME_FORMAT;
    }

    //移除指定列
    static void removeSpecifyField(List<ExcelExportEntity> exportEntityList) {
        if (CollectionUtils.isEmpty(exportEntityList)) {
            return;
        }

        List<ExcelExportEntity> removeList = new ArrayList<>();
        for (ExcelExportEntity excelExportEntity : exportEntityList) {

            if (CollectionUtils.isNotEmpty(excelExportEntity.getList())) {
                removeSpecifyField(excelExportEntity.getList());
            }

            if (excelExportEntity.getKey().toString().equals("tenantId") || excelExportEntity.getKey().toString().equals("deletedFlag") ||
                    excelExportEntity.getKey().toString().equals("id") || excelExportEntity.getKey().toString().equals("creatorId") ||
                    excelExportEntity.getKey().toString().equals("updaterId")) {
                removeList.add(excelExportEntity);
            } else if (excelExportEntity.getKey().toString().endsWith("Id")) {
                removeList.add(excelExportEntity);
            }
        }
        exportEntityList.removeAll(removeList);
    }


    static void removeNullValueField(List<ExcelExportEntity> exportEntityList, List dataList) throws NoSuchFieldException, IllegalAccessException {

        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }

        //删除嵌套字段中,列值全部为空的数据
        List<ExcelExportEntity> removeList = new ArrayList<>();
        for (ExcelExportEntity excelExportEntity : exportEntityList) {

            if (CollectionUtils.isNotEmpty(excelExportEntity.getList())) {
                removeNullValueField(excelExportEntity.getList(), (List) findValue(dataList.get(0), excelExportEntity.getKey().toString()));
            }

            if (excelExportEntity.getKey().toString().contains(".") || CollectionUtils.isNotEmpty(excelExportEntity.getList()) ||
                    excelExportEntity.getKey().toString().equals("tenantId") || excelExportEntity.getKey().toString().equals("deletedFlag") ||
                    excelExportEntity.getKey().toString().equals("id") || excelExportEntity.getKey().toString().equals("creatorId") ||
                    excelExportEntity.getKey().toString().equals("createTime") || excelExportEntity.getKey().toString().equals("updaterId") ||
                    excelExportEntity.getKey().toString().equals("updateTime")) {
                boolean isAllNull = dataList.stream().allMatch(x -> {
                    Object value = null;
                    try {
                        value = findValue(x, excelExportEntity.getKey().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (value == null) {
                        return true;
                    } else {
                        return false;
                    }
                });

                if (isAllNull) {
                    removeList.add(excelExportEntity);
                }
            }
        }
        exportEntityList.removeAll(removeList);
    }


    static void writeResponse(HttpServletResponse response, List<ExcelExportEntity> exportEntityList, List dataList, String fileName) throws IOException {

        String sheetName = fileName.replace(".xls", "").replace(".xlsx", "");
        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
            fileName = fileName + ".xlsx";
        }
        ExportParams exportParams = new ExportParams(null, sheetName, ExcelType.XSSF);
        exportParams.setStyle(ExcelExportXSSFStylerImpl.class);
        Workbook workbook = ExcelExportUtil.exportExcel(exportParams, exportEntityList, dataList);
        ServletOutputStream servletOutputStream = response.getOutputStream();
        ExcelTemplateUtils.setDownloadResponse(response, fileName);
        workbook.write(servletOutputStream);
        servletOutputStream.flush();
    }

}
