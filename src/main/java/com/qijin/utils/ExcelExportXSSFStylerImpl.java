package com.qijin.utils;

import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import cn.afterturn.easypoi.excel.export.styler.IExcelExportStyler;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

/**
 * @author zhuqijin
 * @Date 10:12 2023/3/3
 */
public class ExcelExportXSSFStylerImpl extends AbstractExcelExportStyler
        implements IExcelExportStyler {

    public ExcelExportXSSFStylerImpl(Workbook workbook) {
        super.createStyles(workbook);
    }

    @Override
    public CellStyle getTitleStyle(short color) {
        XSSFCellStyle titleStyle = (XSSFCellStyle) workbook.createCellStyle();
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setWrapText(true);

        Font titleFont = workbook.createFont();
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleFont.setBold(true);
        titleStyle.setFont(titleFont);

        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFColor xssfColor = new XSSFColor(new java.awt.Color(84, 128, 200), new DefaultIndexedColorMap());
        titleStyle.setFillForegroundColor(xssfColor);


        titleStyle.setBorderRight(BorderStyle.THIN);
        titleStyle.setRightBorderColor(IndexedColors.WHITE.getIndex());
        titleStyle.setBorderLeft(BorderStyle.THIN);
        titleStyle.setLeftBorderColor(IndexedColors.WHITE.getIndex());
        titleStyle.setBorderTop(BorderStyle.THIN);
        titleStyle.setTopBorderColor(IndexedColors.WHITE.getIndex());
        titleStyle.setBorderBottom(BorderStyle.THIN);
        titleStyle.setBottomBorderColor(IndexedColors.WHITE.getIndex());

        return titleStyle;
    }

    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWarp) {
        XSSFCellStyle style = (XSSFCellStyle)workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(STRING_FORMAT);
        if (isWarp) {
            style.setWrapText(true);
        }

        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        return style;
    }

    @Override
    public CellStyle getHeaderStyle(short color) {
        CellStyle titleStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 12);
        titleStyle.setFont(font);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        return titleStyle;
    }

    @Override
    public CellStyle stringNoneStyle(Workbook workbook, boolean isWarp) {
        XSSFCellStyle style = (XSSFCellStyle)workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(STRING_FORMAT);
        if (isWarp) {
            style.setWrapText(true);
        }

        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());

        return style;
    }

}
