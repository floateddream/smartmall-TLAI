-- 第四阶段扩展订单状态。历史迁移 V1-V3 保持不变，已有数据库由本迁移增量升级。
ALTER TABLE orders DROP CONSTRAINT orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check
    CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'REFUNDING', 'REFUNDED'));
ALTER TABLE orders ADD COLUMN refunded_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE refund_request (
    id BIGSERIAL PRIMARY KEY,
    refund_no VARCHAR(32) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL REFERENCES orders (id) ON DELETE RESTRICT,
    order_no VARCHAR(32) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING_REVIEW', 'COMPLETED', 'REJECTED')),
    applicant_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    applicant_name VARCHAR(32) NOT NULL,
    reviewer_id BIGINT REFERENCES app_user (id) ON DELETE RESTRICT,
    reviewer_name VARCHAR(32),
    review_remark VARCHAR(500),
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refund_request_order_id ON refund_request (order_id);
CREATE INDEX idx_refund_request_status ON refund_request (status);
CREATE INDEX idx_refund_request_applied_at ON refund_request (applied_at DESC);
-- 同一个订单同一时间只能有一笔待审核退款；驳回后仍允许重新申请。
CREATE UNIQUE INDEX uk_refund_request_pending_order
    ON refund_request (order_id) WHERE status = 'PENDING_REVIEW';

CREATE TABLE after_sales_ticket (
    id BIGSERIAL PRIMARY KEY,
    ticket_no VARCHAR(32) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL REFERENCES orders (id) ON DELETE RESTRICT,
    order_no VARCHAR(32) NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customer (id) ON DELETE RESTRICT,
    customer_name VARCHAR(100) NOT NULL,
    category VARCHAR(32) NOT NULL CHECK (category IN ('RETURN_EXCHANGE', 'LOGISTICS', 'PRODUCT_QUALITY', 'OTHER')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'PROCESSING', 'RESOLVED', 'CLOSED')),
    subject VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    creator_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    creator_name VARCHAR(32) NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_after_sales_ticket_order_id ON after_sales_ticket (order_id);
CREATE INDEX idx_after_sales_ticket_status ON after_sales_ticket (status);
CREATE INDEX idx_after_sales_ticket_category ON after_sales_ticket (category);
CREATE INDEX idx_after_sales_ticket_updated_at ON after_sales_ticket (updated_at DESC);

CREATE TABLE ticket_message (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES after_sales_ticket (id) ON DELETE CASCADE,
    message_type VARCHAR(24) NOT NULL CHECK (message_type IN ('REPLY', 'STATUS_CHANGE')),
    sender_id BIGINT NOT NULL REFERENCES app_user (id) ON DELETE RESTRICT,
    sender_name VARCHAR(32) NOT NULL,
    content VARCHAR(1000),
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ticket_message_status_pair CHECK (
        (message_type = 'REPLY' AND from_status IS NULL AND to_status IS NULL AND content IS NOT NULL)
        OR
        (message_type = 'STATUS_CHANGE' AND from_status IS NOT NULL AND to_status IS NOT NULL)
    )
);

CREATE INDEX idx_ticket_message_ticket_id ON ticket_message (ticket_id, id);
