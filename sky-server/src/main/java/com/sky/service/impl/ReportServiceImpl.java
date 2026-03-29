package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 统计指定时间区间的内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        // 当前集合用于存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        LocalDate current = begin;
        while(!current.equals(end)){
            // 日期计算，计算指定日期的后一天对应的日期
            current = current.plusDays(1);
            dateList.add(current);
        }

        // 存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : dateList){
            // 计算每天的营业额数据，查询的是每天状态为“已完成”的订单金额总和
            // 这里需要将LocalDate转换成LocalDateTime，转换成当天的开始时间和结束时间,对应着数据库中订单的完成时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // select sum(amount) from orders where status = 5 and order_time >= beginTime and complete_time <= endTime
            Map map = new HashMap<>();
            map.put("status", Orders.COMPLETED);
            map.put("begin", beginTime);
            map.put("end", endTime);
            // 这里拿到每一天的营业额放到turnoverList集合中，方便后面构建TurnoverReportVO对象
            Double turnover = orderMapper.sumByMap(map);
            // 判断当天营业额是否为null，如果为null则设置为0.0
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }


        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();

    }

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        LocalDate current = begin;
        dateList.add(current);
        while(!current.equals(end)){
            // 日期计算，计算指定日期的后一天对应的日期
            current = current.plusDays(1);
            dateList.add(current);
        }


        // 存放每天的新用户数量 select count(id) from user where create_time >= beginTime and create_time <= endTime
        List<Integer> newUserCountList = new ArrayList<>();
        // 存放每天的总用户数量 select count(id) from user where create_time <= ?
        List<Integer> totalUserCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("end", endTime);

            // 统计总用户数量
            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);
            // 统计新用户数量
            Integer newUser = userMapper.countByMap(map);

            totalUserCountList.add(totalUser);
            newUserCountList.add(newUser);
        }


        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserCountList, ","))
                .totalUserList(StringUtils.join(totalUserCountList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     * 有效订单指的是状态为“已完成”的订单
     * 基于可视化报表的折线图展示订单数据，x轴为日期，y轴为订单数量
     * 根据时间选择区间，展示每天的订单总数和有效订单数
     * 展示所选时间区间内的有效订单数、订单总数、订单完成率（有效订单数/订单总数*100%）
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!begin.equals(end)){
            // 日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 遍历dateList集合，统计每天的订单总数和有效订单数
        List<Integer> totalOrderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 查询每天的订单数 select count(id) from orders where order_time >= beginTime and order_time <= endTime
            Integer orderCount = getOrderCount(beginTime, endTime,null);
            // 查询有效订单数 select count(id) from orders where status = 5 and order_time >= beginTime and order_time <= endTime
            Integer validOrderCount = getOrderCount(beginTime, endTime,Orders.COMPLETED);

            totalOrderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);

        }

        // 计算时间区间内的订单总数量
        Integer totalOrderCount = totalOrderCountList.stream().reduce(Integer::sum).get();
        // 计算时间区间内的有效订单总数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        // 计算订单完成率，订单完成率 = 有效订单数 / 订单总数 * 100%
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate=  validOrderCount.doubleValue() / totalOrderCount;
        }



        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(totalOrderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间区间内的热销榜单top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");

        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1. 查询数据库，获取营业数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        LocalDateTime begin = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(dateEnd, LocalTime.MAX);

        // 查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(begin, end);

        // 2. 通过POI将营业数据写入Excel文件中

        // 将Excel模板文件加载到内存中，获取输入流对象
        ClassLoader loader = this.getClass().getClassLoader();
        InputStream in = loader.getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            // 基于模板文件创建一个新的Excel文件对象
            XSSFWorkbook excel = new XSSFWorkbook(in);

            // 填充数据——时间
            XSSFSheet sheet = excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue("时间： " + dateBegin + " ~ " + dateEnd);

            // 获得第四行
            sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers());

            // 获得第五行
            sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice());

            // 填充明细数据
            for(int i = 0 ; i < 30 ; i++){
                LocalDate date = dateBegin.plusDays(i);
                // 查询某一天的营业数据，查询条件是当天的开始时间和结束时间
                BusinessDataVO data = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                // Excel表中从第八行开始填充
                XSSFRow row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(data.getTurnover());
                row.getCell(3).setCellValue(data.getValidOrderCount());
                row.getCell(4).setCellValue(data.getOrderCompletionRate());
                row.getCell(5).setCellValue(data.getUnitPrice());
                row.getCell(6).setCellValue(data.getNewUsers());
            }


            // 3. 将Excel文件通过输出流写入到浏览器中，提供给用户下载
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            // 关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 根据条件统计订单数量
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status){
        Map map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);

        return orderMapper.countOrderByMap(map);
    }
}
