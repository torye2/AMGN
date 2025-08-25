package amgn.amu.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import amgn.amu.dto.OrderCreateRequest;
import amgn.amu.dto.OrderDto;
import amgn.amu.dto.PaymentRequest;
import amgn.amu.dto.TrackingInputRequest;
import amgn.amu.service.OrderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders.html")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderDto create(@RequestParam Long userId, @Valid @RequestBody OrderCreateRequest req) {
        return orderService.create(userId, req);
    }

    @PostMapping("/{id}/pay")
    public OrderDto pay(@RequestParam Long userId, @PathVariable Long id, @RequestBody PaymentRequest req) {
        return orderService.pay(userId, id, req);
    }

    @PostMapping("/{id}/confirm-delivery")
    public OrderDto delivered(@RequestParam Long userId, @PathVariable Long id) {
        return orderService.confirmDelivered(userId, id);
    }

    @PostMapping("/{id}/complete")
    public OrderDto complete(@RequestParam Long userId, @PathVariable Long id) {
        return orderService.complete(userId, id);
    }

    @PostMapping("/{id}/confirm-meetup")
    public OrderDto confirmMeetup(@RequestParam Long userId, @PathVariable Long id) {
        return orderService.confirmMeetup(userId, id);
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@RequestParam Long userId, @PathVariable Long id) {
        return orderService.cancel(userId, id);
    }

    @PostMapping("/{id}/dispute")
    public OrderDto dispute(@RequestParam Long userId, @PathVariable Long id, @RequestParam String reason) {
        return orderService.dispute(userId, id, reason);
    }

    @PostMapping("/{id}/tracking")
    public String inputTracking(@RequestParam Long userId, @PathVariable Long id, @Valid @RequestBody TrackingInputRequest req) {
        return orderService.inputTracking(userId, id, req).toString();
    }

    @GetMapping
    public List<OrderDto> myOrders(@RequestParam Long userId) {
        return orderService.myOrders(userId);
    }
}
