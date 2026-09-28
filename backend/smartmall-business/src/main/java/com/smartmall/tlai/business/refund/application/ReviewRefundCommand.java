package com.smartmall.tlai.business.refund.application;

public record ReviewRefundCommand(long reviewerId, String reviewerName, String remark) {
}
