package com.smartmall.tlai.business.refund.application;

public record CreateRefundCommand(long orderId, long applicantId, String applicantName, String reason) {
}
