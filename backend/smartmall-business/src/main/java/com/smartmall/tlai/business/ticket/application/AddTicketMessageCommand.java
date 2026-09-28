package com.smartmall.tlai.business.ticket.application;

public record AddTicketMessageCommand(long senderId, String senderName, String content) {
}
