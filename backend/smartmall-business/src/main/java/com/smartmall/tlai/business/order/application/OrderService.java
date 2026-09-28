package com.smartmall.tlai.business.order.application;

import com.smartmall.tlai.business.customer.domain.Customer;
import com.smartmall.tlai.business.customer.repository.CustomerRepository;
import com.smartmall.tlai.business.order.domain.Order;
import com.smartmall.tlai.business.order.domain.OrderItem;
import com.smartmall.tlai.business.order.domain.OrderStatus;
import com.smartmall.tlai.business.order.repository.OrderRepository;
import com.smartmall.tlai.business.product.domain.Product;
import com.smartmall.tlai.business.product.repository.ProductRepository;
import com.smartmall.tlai.common.api.PageResponse;
import com.smartmall.tlai.common.error.BusinessException;
import com.smartmall.tlai.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {
    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_ITEM_QUANTITY = 999_999;
    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("9999999999.99");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<Order> list(int page, int size, String keyword, String statusValue) {
        String search = keyword == null ? "" : keyword.trim();
        OrderStatus status = parseStatus(statusValue);
        return new PageResponse<>(orderRepository.findPage(page, size, search, status),
                orderRepository.count(search, status), page, size);
    }

    @Transactional(readOnly = true)
    public Order get(long id) {
        return requireOrder(id);
    }

    @Transactional
    public Order create(CreateOrderCommand command) {
        Customer customer = customerRepository.findById(command.customerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        Map<Long, Integer> requestedProducts = normalizeItems(command.items());
        List<OrderItem> items = new ArrayList<>(requestedProducts.size());
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> requested : requestedProducts.entrySet()) {
            Product product = productRepository.findById(requested.getKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
            if (!product.active()) {
                throw new BusinessException(ErrorCode.PRODUCT_INACTIVE,
                        "商品「" + product.name() + "」已停用");
            }

            int quantity = requested.getValue();
            // 条件扣减由数据库原子执行，任何一项失败都会让当前事务整体回滚。
            if (!productRepository.reserveStock(product.id(), quantity)) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        "商品「" + product.name() + "」库存不足");
            }
            BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(quantity));
            items.add(new OrderItem(null, product.id(), product.sku(), product.name(),
                    product.price(), quantity, subtotal));
            totalAmount = totalAmount.add(subtotal);
            if (subtotal.compareTo(MAX_ORDER_AMOUNT) > 0 || totalAmount.compareTo(MAX_ORDER_AMOUNT) > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "订单金额超出允许范围");
            }
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Order order = new Order(null, createOrderNo(now), customer.id(), customer.name(), totalAmount,
                OrderStatus.PENDING_PAYMENT, items, null, null, null, now, now);
        return orderRepository.insert(order);
    }

    @Transactional
    public Order pay(long id) {
        Order order = requireOrder(id);
        ensurePending(order);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (!orderRepository.markPaid(id, now)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        return requireOrder(id);
    }

    @Transactional
    public Order cancel(long id) {
        Order order = requireOrder(id);
        ensurePending(order);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (!orderRepository.markCancelled(id, now)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        for (OrderItem item : order.items()) {
            productRepository.releaseStock(item.productId(), item.quantity());
        }
        return requireOrder(id);
    }

    private Map<Long, Integer> normalizeItems(List<OrderItemCommand> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单至少需要一件商品");
        }
        Map<Long, Integer> normalized = new LinkedHashMap<>();
        for (OrderItemCommand item : items) {
            if (item == null || item.productId() <= 0 || item.quantity() <= 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "订单商品参数不正确");
            }
            try {
                int mergedQuantity = Math.addExact(normalized.getOrDefault(item.productId(), 0), item.quantity());
                if (mergedQuantity > MAX_ITEM_QUANTITY) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "单件商品数量超出允许范围");
                }
                normalized.put(item.productId(), mergedQuantity);
            } catch (ArithmeticException exception) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "商品数量超出允许范围");
            }
        }
        return normalized;
    }

    private OrderStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单状态筛选值不正确");
        }
    }

    private Order requireOrder(long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void ensurePending(Order order) {
        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
    }

    private String createOrderNo(OffsetDateTime now) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return "SM" + ORDER_TIME_FORMAT.format(now) + random;
    }
}
