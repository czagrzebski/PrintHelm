package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiCreateJobOrderRequest;
import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.model.ApiUpdateJobOrderRequest;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.JobOrderStatus;
import com.czagrzebski.printhelm.web.mapper.JobOrderMapper;
import com.czagrzebski.printhelm.web.repository.JobOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobOrderService {

    private final JobOrderRepository jobOrderRepository;
    private final JobOrderMapper jobOrderMapper;

    public JobOrderService(JobOrderRepository jobOrderRepository, JobOrderMapper jobOrderMapper) {
        this.jobOrderRepository = jobOrderRepository;
        this.jobOrderMapper = jobOrderMapper;
    }

    public List<ApiJobOrderResponse> getAllOrders() {
        return jobOrderMapper.jobOrdersToApiJobOrderResponses(jobOrderRepository.findAll());
    }

    public ApiJobOrderResponse getOrderById(long id) {
        return jobOrderMapper.jobOrderToApiJobOrderResponse(findOrThrow(id));
    }

    public ApiJobOrderResponse createOrder(ApiCreateJobOrderRequest request) {
        JobOrder order = jobOrderMapper.apiCreateJobOrderRequestToJobOrder(request);
        return jobOrderMapper.jobOrderToApiJobOrderResponse(jobOrderRepository.save(order));
    }

    @Transactional
    public ApiJobOrderResponse updateOrder(long id, ApiUpdateJobOrderRequest request) {
        JobOrder order = findOrThrow(id);
        if (request.getCustomerName() != null) order.setCustomerName(request.getCustomerName());
        if (request.getCustomerEmail() != null) order.setCustomerEmail(request.getCustomerEmail());
        if (request.getDescription() != null) order.setDescription(request.getDescription());
        if (request.getStatus() != null) order.setStatus(JobOrderStatus.valueOf(request.getStatus().getValue()));
        if (request.getRequirements() != null) order.setRequirements(request.getRequirements());
        if (request.getRequiresCustomDesign() != null) order.setRequiresCustomDesign(request.getRequiresCustomDesign());
        return jobOrderMapper.jobOrderToApiJobOrderResponse(jobOrderRepository.save(order));
    }

    public void deleteOrder(long id) {
        jobOrderRepository.delete(findOrThrow(id));
    }

    private JobOrder findOrThrow(long id) {
        return jobOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job order not found: " + id));
    }
}
