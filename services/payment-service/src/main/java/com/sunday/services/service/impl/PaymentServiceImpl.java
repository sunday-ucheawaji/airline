package com.sunday.services.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.enums.PaymentStatus;
import com.sunday.common_lib.exception.PaymentException;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.request.PaymentVerifyRequest;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.common_lib.payload.response.PaymentLinkResponse;
import com.sunday.services.client.UserClient;
import com.sunday.services.event.PaymentEventProducer;
import com.sunday.services.mapper.PaymentMapper;
import com.sunday.services.model.Payment;
import com.sunday.services.repository.PaymentRepository;
import com.sunday.services.service.PaymentService;
import com.sunday.services.service.gateway.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final RazorpayService razorpayService;
    private final UserClient userClient;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) throws PaymentException {
        try {
            log.info("Initiating payment for user: {} with gateway: {}",
                    request.getUserId(), request.getGateway());

            // Check if payment already exists for this booking
            paymentRepository.findByBookingId(request.getBookingId())
                    .ifPresent(existingPayment -> {
                        if (existingPayment.getStatus() == PaymentStatus.SUCCESS) {
                            throw new RuntimeException("Payment already completed for this booking");
                        }
                    });

            // Create payment entity
            Payment payment = Payment.builder()
                    .userId(request.getUserId())
                    .bookingId(request.getBookingId())
                    .amount(request.getAmount())
                    .provider(request.getGateway())
                    .status(PaymentStatus.PENDING)
                    .transactionId(generateTransactionId())
                    .build();

            payment = paymentRepository.save(payment);

            // Create response based on gateway
            PaymentInitiateResponse response = PaymentInitiateResponse.builder()
                    .paymentId(payment.getId())
                    .gateway(request.getGateway())
                    .transactionId(payment.getTransactionId())
                    .amount(request.getAmount())
                    .description(request.getDescription())
                    .success(true)
                    .message("Payment initiated successfully")
                    .build();

            if (request.getGateway() == PaymentGateway.RAZORPAY) {


                UserDTO user=userClient.getUserById(payment.getUserId());

                PaymentLinkResponse paymentLinkResponse=razorpayService.createPaymentLink(
                        user, payment
                );
                response.setCheckoutUrl(paymentLinkResponse.getPayment_link_url());
                response.setRazorpayOrderId(paymentLinkResponse.getPayment_link_id());


            } else if (request.getGateway() == PaymentGateway.STRIPE) {
                String checkoutUrl = "https://checkout.stripe.com/pay/" + payment.getTransactionId();
                response.setCheckoutUrl(checkoutUrl);
                // TODO: Integrate with Stripe gateway service
            }

            log.info("Payment initiated successfully with ID: {}", payment.getId());
            return response;

        } catch (Exception e) {
            log.error("Error initiating payment: {}", e.getMessage(), e);
            throw new PaymentException("Failed to initiate payment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentDTO verifyPayment(String payload, String signature) throws PaymentException
    {

        PaymentVerifyRequest request = handleRazorpayWebhook(payload, signature);

        System.out.println("verify payment request: " + request);

        // gateway payment
        JSONObject paymentDetails = razorpayService
                .fetchPaymentDetails(request.getRazorpayPaymentId());

        System.out.println("gatway payment details: " + paymentDetails);


        String status = paymentDetails.optString("status");
        long amount = paymentDetails.optLong("amount");
        long amountInRupees = amount / 100;


        // Extract 'notes' object
        JSONObject notes = paymentDetails.getJSONObject("notes");

        Long paymentId = Long.parseLong(notes.optString("payment_id"));


        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found with ID: " + paymentId));


        boolean isValid = "captured".equalsIgnoreCase(status);

        if (payment.getProvider() == PaymentGateway.RAZORPAY) {

            if (isValid) {
                payment.setProviderPaymentId(request.getRazorpayPaymentId());

            }
        } else if (payment.getProvider() == PaymentGateway.STRIPE) {
//            isValid = stripeService.verifyPayment(request.getStripePaymentIntentId());
//
//            if (isValid) {
//                payment.setProviderPaymentId(request.getStripePaymentIntentId());
//            }
        }

        if (isValid) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            // Save payment first
            payment = paymentRepository.save(payment);
            System.out.println("send payment event and payment status is : "+status);
            paymentEventProducer.sendPaymentCompleted(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment verification failed");
            log.error("Payment verification failed: {}", payment.getId());
            payment = paymentRepository.save(payment);

            paymentEventProducer.sendPaymentFailed(payment);
        }

        return PaymentMapper.toDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(PaymentMapper::toDTO);
    }



    @Override
    @Transactional(readOnly = true)
    public Map<Long, PaymentDTO> getPaymentsByBookingIds(List<Long> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) return Map.of();
        return paymentRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(Payment::getBookingId, PaymentMapper::toDTO));
    }

    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }


    private PaymentVerifyRequest handleRazorpayWebhook(
            String payload,
            String signature) throws PaymentException {

        try {

            JsonNode root = objectMapper.readTree(payload);

            String event = root.path("event").asText();

            JsonNode paymentEntity = root
                    .path("payload")
                    .path("payment")
                    .path("entity");

            String razorpayPaymentId =
                    paymentEntity.path("id").asText();

            String razorpayOrderId =
                    paymentEntity.path("order_id").asText();

            String status =
                    paymentEntity.path("status").asText();

            String internalPaymentId =
                    paymentEntity
                            .path("notes")
                            .path("payment_id")
                            .asText();

            PaymentVerifyRequest request = new PaymentVerifyRequest();

            request.setRazorpayPaymentId(razorpayPaymentId);
            request.setRazorpayOrderId(razorpayOrderId);
            request.setRazorpaySignature(signature);

            log.info(
                    "Razorpay webhook: event={}, paymentId={}, orderId={}, status={}, internalPaymentId={}",
                    event,
                    razorpayPaymentId,
                    razorpayOrderId,
                    status,
                    internalPaymentId
            );

           return request;

        } catch (Exception e) {
            throw new PaymentException(
                    "Failed to process Razorpay webhook: " + e.getMessage()
            );
        }
    }
}
