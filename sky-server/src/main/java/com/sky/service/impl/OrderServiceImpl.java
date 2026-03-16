package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {


    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

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
}
