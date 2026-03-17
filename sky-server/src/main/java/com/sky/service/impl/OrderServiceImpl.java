package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {


    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     * 这个方法当中涉及到多张表的操作，必须要保证事务的一致性，
     * 所以我们需要在这个方法上添加@Transactional注解，开启事务
     * 否则可能会出现订单表中有数据，但是订单明细表中没有数据，
     * 或者订单表和订单明细表中都有数据，但是购物车表中的数据没有被清空，这些都是不一致的情况
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional //
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 处理订单相关的业务异常（地址不存在、购物车为空等）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppCartlist = shoppingCartMapper.list(shoppingCart);
        // 如果购物车为空，则抛出业务异常， 用户不能下单
        if(shoppCartlist == null || shoppCartlist.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 订单表Order和订单明细表OrderDetail是一对多的关系，所以我们需要先向订单表Order插入一条数据，获取到订单id之后，再向订单明细表OrderDetail插入多条数据
        // 1.向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);  // 现在处于待付款状态
        orders.setNumber(String.valueOf(System.currentTimeMillis()));  // 订单号，使用当前时间戳来生成
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);
        // 设置用户地址
        orders.setAddress(addressBook.getDetail());

        orderMapper.insert(orders);

        // 2.向订单明细表插入多条数据
        // 将所有的订单明细全部封装到一个List集合当中，方便后面一次性插入到订单明细表当中
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart shoppingCartItem : shoppCartlist) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCartItem, orderDetail);
            // 设置订单id，在前面执行完orderMapper.insert(orders)之后，orders对象的id属性就会被自动填充上数据库生成的订单id
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        // 3.清空当前用户的购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 4.封装订单提交结果VO对象并返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();


        return orderSubmitVO;
    }

//    /**
//     * 订单支付
//     *
//     * @param ordersPaymentDTO
//     * @return
//     */
//    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
//
//        return vo;
//    }

    /**
     * 订单支付 (模拟支付跳过微信真实校验)
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("【模拟微信支付】正在处理订单：{}", ordersPaymentDTO.getOrderNumber());

        // 1. 我们不再调用 weChatPayUtil 去向微信服务器请求预支付交易单了
        // 因为没有真实的商户号，调用必定报错。

        // 2. 核心魔法：直接在这里“手动”触发支付成功的回调逻辑！
        // 这样数据库里的订单状态就会立刻变成“已支付”和“待接单”
        this.paySuccess(ordersPaymentDTO.getOrderNumber());
        log.info("【模拟微信支付】订单状态已成功修改为已支付！");

        // 3. 随便造一个空的或假的 VO 对象返回给前端
        // 因为前端已经被你改成了直接跳转成功页，根本不看这些具体的签名参数了，
        // 只要外层的 Result.success() 能让前端拿到 code === 1 就行。
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setNonceStr("mock_nonce_str");
        vo.setPaySign("mock_pay_sign");
        vo.setPackageStr("prepay_id=mock_prepay_id");
        vo.setSignType("RSA");
        vo.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

}
