-- 阶段 4 中文演示数据。
-- 只新增退款、工单和处理记录，并调整被选中订单的业务状态；不删除任何数据。
-- 同批演示编号已存在时主动终止，防止重复执行。
DO $$
DECLARE
    actor_id BIGINT;
    actor_name VARCHAR(32);
    order_row RECORD;
    ticket_id BIGINT;
    item_index INTEGER := 0;
    demo_status VARCHAR(32);
    demo_category VARCHAR(32);
    demo_priority VARCHAR(20);
    base_time TIMESTAMP WITH TIME ZONE;
    subjects TEXT[] := ARRAY[
        '商品尺寸不合适，希望更换规格',
        '物流信息三天没有更新',
        '商品屏幕到货后发现划痕',
        '包装内缺少原装充电线',
        '商品颜色与预期不符申请换货',
        '订单显示签收但客户未收到',
        '设备开机后频繁自动重启',
        '发票抬头信息需要重新开具',
        '鞋码偏小需要更换大一码',
        '快递运输途中外箱严重破损',
        '耳机右侧播放时没有声音',
        '包装内没有找到使用说明书',
        '重复购买，申请退回未拆封商品',
        '收货地址有误，请协助联系转派',
        '键盘部分按键出现连续失灵',
        '活动赠品没有随订单一起送达'
    ];
    descriptions TEXT[] := ARRAY[
        '客户收到商品后试用发现规格偏小，商品外观和包装完整，希望更换同款大一号规格。',
        '物流轨迹停留在分拨中心超过三天，客户多次查询没有新进展，希望协助联系承运方。',
        '外包装没有明显磕碰，但拆封后发现屏幕右上角有两处划痕，客户已保留开箱照片。',
        '包装清单标注包含充电线，实际开箱后只找到主机和适配器，需要核实并安排补发。',
        '客户下单选择深空灰，收到后认为实物颜色偏浅，希望在不影响二次销售的情况下换货。',
        '系统中的物流状态为本人签收，但客户和家人均未收到包裹，需要核查签收底单。',
        '设备可以正常开机，但运行约十分钟后会自动重启，恢复出厂设置后问题仍然存在。',
        '客户下单时填写的发票抬头有误，希望作废原电子发票并按新的公司信息重新开具。',
        '鞋款外观完好且仅在室内试穿，当前尺码偏小，希望更换大一码的同款商品。',
        '快递送达时外箱一侧已经明显凹陷，客户暂未拆封，希望先确认后续验货和理赔流程。',
        '耳机完成配对后左侧声音正常，右侧始终没有声音，重置并更换设备测试后仍未恢复。',
        '客户首次使用产品，需要纸质说明书完成安装，但包装内没有找到说明书或快速指南。',
        '家人重复下单购买了同一商品，其中一件尚未拆封，希望咨询退货地址和寄回要求。',
        '客户发现订单中的门牌号填写错误，包裹尚未派送，希望协助联系网点修改配送信息。',
        '键盘使用一周后空格键和回车键偶发失灵，客户已更换电脑接口测试，问题依旧存在。',
        '订单参与满额赠品活动，主商品已经正常签收，但包裹中没有活动页面注明的赠品。'
    ];
    refund_reasons TEXT[] := ARRAY[
        '客户临时调整采购计划，商品尚未投入使用，申请整单退款。',
        '收货后发现商品规格不适合当前使用场景，希望取消本次购买。',
        '客户重复提交了同类商品订单，确认保留另一笔订单。',
        '商品交付时间无法满足项目安排，客户申请终止订单。',
        '开箱检查发现商品存在明显外观瑕疵，核实后同意办理退款。',
        '设备多次测试仍无法正常启动，客户提供了故障视频。',
        '运输过程中商品受损，承运方和客户均已提供现场照片。',
        '实际收到的商品型号与订单不一致，客户选择直接退款。',
        '商品已经拆封并出现明显人为损坏，不符合退款受理条件。',
        '订单已超过约定售后时限，现有材料不足以支持退款。',
        '客户暂未提供必要的故障照片和序列号信息，申请被驳回。',
        '核实后商品功能正常，问题由第三方配件不兼容导致。'
    ];
BEGIN
    IF EXISTS (SELECT 1 FROM refund_request WHERE refund_no LIKE 'RFDEMO20260924%')
       OR EXISTS (SELECT 1 FROM after_sales_ticket WHERE ticket_no LIKE 'TKDEMO20260924%') THEN
        RAISE EXCEPTION '阶段 4 演示数据已经存在，已停止重复生成';
    END IF;

    SELECT id, username INTO actor_id, actor_name
    FROM app_user
    WHERE enabled = TRUE
    ORDER BY id
    LIMIT 1;

    IF actor_id IS NULL THEN
        RAISE EXCEPTION '没有可用于演示数据审计字段的启用账号';
    END IF;

    IF (SELECT COUNT(*) FROM orders WHERE status = 'PAID' AND created_at <= CURRENT_TIMESTAMP - INTERVAL '15 days') < 12 THEN
        RAISE EXCEPTION '满足演示时间条件的已支付订单不足 12 笔，无法生成完整退款演示数据';
    END IF;
    IF (SELECT COUNT(*) FROM orders WHERE created_at <= CURRENT_TIMESTAMP - INTERVAL '20 days') < 16 THEN
        RAISE EXCEPTION '满足演示时间条件的订单不足 16 笔，无法生成完整工单演示数据';
    END IF;

    -- 退款数据覆盖待审核、已完成和已驳回三种状态，每种 4 条。
    item_index := 0;
    FOR order_row IN
        SELECT id, order_no, total_amount
        FROM orders
        WHERE status = 'PAID'
          AND created_at <= CURRENT_TIMESTAMP - INTERVAL '15 days'
        ORDER BY id DESC
        LIMIT 12
    LOOP
        item_index := item_index + 1;
        base_time := CURRENT_TIMESTAMP - make_interval(days => item_index + 1);

        IF item_index <= 4 THEN
            demo_status := 'PENDING_REVIEW';
            UPDATE orders
            SET status = 'REFUNDING', updated_at = base_time
            WHERE id = order_row.id AND status = 'PAID';
        ELSIF item_index <= 8 THEN
            demo_status := 'COMPLETED';
            UPDATE orders
            SET status = 'REFUNDED', refunded_at = base_time + INTERVAL '6 hours',
                updated_at = base_time + INTERVAL '6 hours'
            WHERE id = order_row.id AND status = 'PAID';

            -- 已完成退款需要归还创建订单时扣减的全部库存。
            UPDATE product AS product_row
            SET stock = product_row.stock + restored.quantity,
                updated_at = CURRENT_TIMESTAMP
            FROM (
                SELECT product_id, SUM(quantity)::INTEGER AS quantity
                FROM order_item
                WHERE order_id = order_row.id
                GROUP BY product_id
            ) AS restored
            WHERE product_row.id = restored.product_id;
        ELSE
            demo_status := 'REJECTED';
        END IF;

        INSERT INTO refund_request (
            refund_no, order_id, order_no, amount, reason, status,
            applicant_id, applicant_name, reviewer_id, reviewer_name, review_remark,
            applied_at, reviewed_at, created_at, updated_at
        ) VALUES (
            'RFDEMO20260924' || LPAD(item_index::TEXT, 3, '0'),
            order_row.id, order_row.order_no, order_row.total_amount, refund_reasons[item_index], demo_status,
            actor_id, actor_name,
            CASE WHEN demo_status = 'PENDING_REVIEW' THEN NULL ELSE actor_id END,
            CASE WHEN demo_status = 'PENDING_REVIEW' THEN NULL ELSE actor_name END,
            CASE
                WHEN demo_status = 'COMPLETED' THEN '材料完整，商品状态与订单信息核验一致，同意全额退款。'
                WHEN demo_status = 'REJECTED' THEN '当前申请不符合退款条件，已联系客户补充说明。'
                ELSE NULL
            END,
            base_time,
            CASE WHEN demo_status = 'PENDING_REVIEW' THEN NULL ELSE base_time + INTERVAL '6 hours' END,
            base_time,
            CASE WHEN demo_status = 'PENDING_REVIEW' THEN base_time ELSE base_time + INTERVAL '6 hours' END
        );
    END LOOP;

    -- 工单数据覆盖四种分类、四档优先级和完整处理状态。
    item_index := 0;
    FOR order_row IN
        SELECT id, order_no, customer_id, customer_name
        FROM orders
        WHERE created_at <= CURRENT_TIMESTAMP - INTERVAL '20 days'
        ORDER BY id DESC
        LIMIT 16
    LOOP
        item_index := item_index + 1;
        base_time := CURRENT_TIMESTAMP - make_interval(days => 18 - item_index);
        demo_category := (ARRAY['RETURN_EXCHANGE', 'LOGISTICS', 'PRODUCT_QUALITY', 'OTHER'])[((item_index - 1) % 4) + 1];
        demo_priority := (ARRAY['NORMAL', 'HIGH', 'URGENT', 'LOW'])[((item_index - 1) % 4) + 1];
        demo_status := CASE
            WHEN item_index <= 4 THEN 'OPEN'
            WHEN item_index <= 8 THEN 'PROCESSING'
            WHEN item_index <= 12 THEN 'RESOLVED'
            ELSE 'CLOSED'
        END;

        INSERT INTO after_sales_ticket (
            ticket_no, order_id, order_no, customer_id, customer_name,
            category, priority, status, subject, description,
            creator_id, creator_name, resolved_at, closed_at, created_at, updated_at
        ) VALUES (
            'TKDEMO20260924' || LPAD(item_index::TEXT, 3, '0'),
            order_row.id, order_row.order_no, order_row.customer_id, order_row.customer_name,
            demo_category, demo_priority, demo_status, subjects[item_index], descriptions[item_index],
            actor_id, actor_name,
            CASE WHEN demo_status IN ('RESOLVED', 'CLOSED') THEN base_time + INTERVAL '4 hours' ELSE NULL END,
            CASE WHEN demo_status = 'CLOSED' THEN base_time + INTERVAL '6 hours' ELSE NULL END,
            base_time,
            CASE demo_status
                WHEN 'OPEN' THEN base_time + INTERVAL '1 hour'
                WHEN 'PROCESSING' THEN base_time + INTERVAL '3 hours'
                WHEN 'RESOLVED' THEN base_time + INTERVAL '4 hours'
                ELSE base_time + INTERVAL '6 hours'
            END
        ) RETURNING id INTO ticket_id;

        INSERT INTO ticket_message (
            ticket_id, message_type, sender_id, sender_name, content, created_at
        ) VALUES (
            ticket_id, 'REPLY', actor_id, actor_name,
            '已收到客户反馈，订单和售后材料正在核对中。', base_time + INTERVAL '1 hour'
        );

        IF demo_status <> 'OPEN' THEN
            INSERT INTO ticket_message (
                ticket_id, message_type, sender_id, sender_name, content,
                from_status, to_status, created_at
            ) VALUES (
                ticket_id, 'STATUS_CHANGE', actor_id, actor_name,
                '信息已核实，工单转入正式处理。', 'OPEN', 'PROCESSING', base_time + INTERVAL '2 hours'
            );
            INSERT INTO ticket_message (
                ticket_id, message_type, sender_id, sender_name, content, created_at
            ) VALUES (
                ticket_id, 'REPLY', actor_id, actor_name,
                '已联系仓储、物流或供应商核查，处理结果会在本工单中同步。', base_time + INTERVAL '3 hours'
            );
        END IF;

        IF demo_status IN ('RESOLVED', 'CLOSED') THEN
            INSERT INTO ticket_message (
                ticket_id, message_type, sender_id, sender_name, content,
                from_status, to_status, created_at
            ) VALUES (
                ticket_id, 'STATUS_CHANGE', actor_id, actor_name,
                '处理方案已经落实并向客户反馈。', 'PROCESSING', 'RESOLVED', base_time + INTERVAL '4 hours'
            );
        END IF;

        IF demo_status = 'CLOSED' THEN
            INSERT INTO ticket_message (
                ticket_id, message_type, sender_id, sender_name, content, created_at
            ) VALUES (
                ticket_id, 'REPLY', actor_id, actor_name,
                '客户已确认处理结果，本次售后问题可以关闭。', base_time + INTERVAL '5 hours'
            );
            INSERT INTO ticket_message (
                ticket_id, message_type, sender_id, sender_name, content,
                from_status, to_status, created_at
            ) VALUES (
                ticket_id, 'STATUS_CHANGE', actor_id, actor_name,
                '客户确认完成，工单归档。', 'RESOLVED', 'CLOSED', base_time + INTERVAL '6 hours'
            );
        END IF;
    END LOOP;
END $$;
