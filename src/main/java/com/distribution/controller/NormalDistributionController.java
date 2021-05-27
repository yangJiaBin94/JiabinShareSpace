package com.distribution.controller;

import com.distribution.util.ArrayCalculation;
import com.distribution.util.DownloadUtil;
import com.distribution.vo.ExcelContent;
import com.distribution.vo.Param;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yangjiabin
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class NormalDistributionController {

    private static org.apache.commons.math.distribution.NormalDistribution d;

    private static final String PATH = "/data/excel/";
//    private static final String PATH = "/Users/yangjiabin/Documents/file/";

    @RequestMapping(value = "/getResult/v1", method = RequestMethod.POST)
    public void userContactV1(Param param, HttpServletResponse response, HttpServletRequest req) throws Exception {

        log.info("参数：{}", com.alibaba.fastjson.JSON.toJSONString(param));
        String[] dataArr = param.getData().trim().split(" ");
        List<Long> dataList = Arrays.stream(dataArr).map(Long::parseLong).collect(Collectors.toList());
        ArrayCalculation calculation = ArrayCalculation.build(dataList);
        log.info("计算类：{}", com.alibaba.fastjson.JSON.toJSONString(calculation));
        ExcelContent excelContent = extracted(calculation.getAverage().doubleValue(), calculation.getStandardDeviation().doubleValue(), param.getNum());
        log.info("excel组装参数：{}", com.alibaba.fastjson.JSON.toJSONString(excelContent));
        getList(excelContent);
        log.info("excel组装参数：{}", com.alibaba.fastjson.JSON.toJSONString(excelContent));
        excelExport(excelContent, param.getNum(), response, req, rounding(calculation.getAverage().doubleValue()) + "", rounding(calculation.getStandardDeviation().doubleValue()) + "");

    }

    private ExcelContent extracted(double mean, double std, int num) throws MathException {
        ExcelContent excelContent = new ExcelContent();
        d = new NormalDistributionImpl(mean, std);
        List<Double> list = new ArrayList<>();
        if (num % 2 != 1) {
            num = num + 1;
        }

        for (int i = (num / 2 - (num - 1)); i <= (num / 2 + 1); i++) {
            if (i != 0) {
                list.add(rounding(mean + i * std));
            }
        }

        for (int i = 0; i < list.size(); i++) {
            String section = "";
            String rounding = "";
            String rate = "";

            if (i > 0) {
                section = list.get(i - 1) + "～" + list.get(i);
                rounding = ((list.get(i - 1) + 1.0) + "").split("\\.")[0] + "～" + list.get(i).toString().split("\\.")[0];
                rate = getRate(d.cumulativeProbability(list.get(i)) - d.cumulativeProbability(list.get(i - 1)));
            } else if (i == 0) {
                section = (rounding(list.get(i) - std)) + "～" + list.get(i);
                rounding = (rounding(list.get(i) - std) + "").split("\\.")[0] + "～" + list.get(i).toString().split("\\.")[0];
                rate = getRate(d.cumulativeProbability(list.get(i)) - d.cumulativeProbability(list.get(i) - std));
            }
            excelContent.getSection().add(section);
            if (num != 5) {
                excelContent.getRate().add(rate);
            }
            excelContent.getRounding().add(rounding);

        }
        if (num == 5) {
            excelContent.getRate().add("2.28%");
            excelContent.getRate().add("13.59%");
            excelContent.getRate().add("68.26%");
            excelContent.getRate().add("13.59%");
            excelContent.getRate().add("2.28%");
        }
        return excelContent;
    }

    public static String getRate(double num) {
        DecimalFormat df = new DecimalFormat("0.00%");
        BigDecimal b = new BigDecimal(num);
        return df.format(b.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue());

    }

    public static double rounding(double num) {
        BigDecimal b = new BigDecimal(num);
        return b.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();

    }

    private void getList(ExcelContent excelContent) {
        int size = excelContent.getRate().size();

        excelContent.getSum().add(excelContent.getRounding().get(0).split("～")[0] + "-" + excelContent.getRounding().get(size / 2 - 1).split("～")[1]);
        excelContent.getSum().add(excelContent.getRounding().get(size / 2).replace("～", "-"));
        excelContent.getSum().add(excelContent.getRounding().get(size / 2 + 1).split("～")[0] + "-" + excelContent.getRounding().get(size - 1).split("～")[1]);
        excelContent.getSumRate().add((Double.parseDouble(excelContent.getRate().get(0).replace("%", "")) + Double.valueOf(excelContent.getRate().get(size / 2 - 1).replace("%", ""))) + "%");
        excelContent.getSumRate().add(excelContent.getRate().get(size / 2));
        excelContent.getSumRate().add((Double.parseDouble(excelContent.getRate().get(size / 2 + 1).replace("%", "")) + Double.valueOf(excelContent.getRate().get(size - 1).replace("%", ""))) + "%");

    }

    private void excelExport(ExcelContent excelContent, int num, HttpServletResponse response, HttpServletRequest req, String mean, String st) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
        String filePath = PATH + sdf.format(new Date()) + ".xls";
        //创建Excel文件(Workbook)
        Workbook wb = new HSSFWorkbook();
        FileOutputStream fout = new FileOutputStream(filePath);
        Sheet sheet = wb.createSheet("第一个sheet页");
        sheet.setColumnWidth(0, 3766);
        CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setLeftBorderColor(IndexedColors.RED.getIndex());
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setRightBorderColor(IndexedColors.BLUE.getIndex());
        cellStyle.setBorderTop(BorderStyle.DASHED);
        cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setAlignment(HorizontalAlignment.CENTER);

        Row row0 = sheet.createRow(0);
        Cell row0_cell0 = row0.createCell(0);
        Cell row0_cell1 = row0.createCell(1);
        row0_cell0.setCellValue("mean");
        row0_cell1.setCellValue(mean);

        Row row1 = sheet.createRow(1);
        Cell row1_cell0 = row1.createCell(0);
        Cell row1_cell1 = row1.createCell(1);
        row1_cell0.setCellValue("sd");
        row1_cell1.setCellValue(st);

        Row row2 = sheet.createRow(2);
        Cell row2_cell0 = row2.createCell(0);
        Cell row2_cell1 = row2.createCell(1);
        Cell row2_cell2 = row2.createCell(2);
        Cell row2_cell3 = row2.createCell(3);
        Cell row2_cell4 = row2.createCell(4);
        Cell row2_cell5 = row2.createCell(5);
        Cell row2_cell6 = row2.createCell(6);

        row2_cell0.setCellValue("总分");
        row2_cell1.setCellValue("百分比");
        row2_cell2.setCellValue(num + "段区间");
        row2_cell4.setCellValue("取整");
        row2_cell6.setCellValue("百分比");

        row2_cell0.setCellStyle(cellStyle);
        row2_cell1.setCellStyle(cellStyle);
        row2_cell2.setCellStyle(cellStyle);
        row2_cell3.setCellStyle(cellStyle);
        row2_cell4.setCellStyle(cellStyle);
        row2_cell5.setCellStyle(cellStyle);
        row2_cell6.setCellStyle(cellStyle);

        sheet.addMergedRegion(new CellRangeAddress(2, 2, 2, 3));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 4, 5));

        for (int i = 0; i < excelContent.getRounding().size(); i++) {
            Row row = sheet.createRow(i + 3);
            Cell sum_cell = row.createCell(0);
            Cell sum_rate_cell = row.createCell(1);
            Cell section_cell_1 = row.createCell(2);
            Cell section_cell_2 = row.createCell(3);
            Cell rounding_cell_1 = row.createCell(4);
            Cell rounding_cell_2 = row.createCell(5);
            Cell rate_cell = row.createCell(6);

            if (i < 3) {
                sum_cell.setCellValue(excelContent.getSum().get(i));
                sum_rate_cell.setCellValue(excelContent.getSumRate().get(i));
                sum_cell.setCellStyle(cellStyle);
                sum_rate_cell.setCellStyle(cellStyle);
            }
            section_cell_1.setCellValue(excelContent.getSection().get(i).split("～")[0]);
            section_cell_2.setCellValue(excelContent.getSection().get(i).split("～")[1]);
            rounding_cell_1.setCellValue(excelContent.getRounding().get(i).split("～")[0]);
            rounding_cell_2.setCellValue(excelContent.getRounding().get(i).split("～")[1]);
            rate_cell.setCellValue(excelContent.getRate().get(i));
            section_cell_1.setCellStyle(cellStyle);
            section_cell_2.setCellStyle(cellStyle);
            rounding_cell_1.setCellStyle(cellStyle);
            rounding_cell_2.setCellStyle(cellStyle);
            rate_cell.setCellStyle(cellStyle);
        }
        wb.write(fout);
        fout.close();
        log.info("保存excel完成：{}", filePath);
        DownloadUtil.downloadFile(filePath, "正态分布报表.xls", response, req);
    }


}
