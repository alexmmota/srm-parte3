package br.com.srm.service;

import br.com.srm.client.EstoqueClient;
import br.com.srm.exception.BusinessServiceException;
import br.com.srm.model.Order;
import br.com.srm.model.OrderItem;
import br.com.srm.client.dto.Product;
import br.com.srm.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EstoqueClient estoqueClient;

    public Order create(Order order) {
        logger.info("m=create, order={}", order);
        validateItensExists(order);
        order.setCreateDate(new Date());
        order.setStatus(Order.Status.CREATED);
        return orderRepository.save(order);
    }

    public Order findById(String id) {
        return getOrderById(id);
    }

    public List<Order> findByClient(String cpf) {
        return orderRepository.findByClient_CpfOrderByCreateDateAsc(cpf);
    }

    public Order finish(String id) {
        logger.info("m=finish, id={}", id);
        Order order = getOrderById(id);
        validateItensAmount(order);
        order.setFinishDate(new Date());
        order.setStatus(Order.Status.FINISHED);
        updateProductAmount(order);
        return orderRepository.save(order);
    }

    public Order cancel(String id) {
        logger.info("m=cancel, id={}", id);
        Order order = getOrderById(id);
        order.setFinishDate(new Date());
        order.setStatus(Order.Status.CANCELED);
        return orderRepository.save(order);
    }

    private void validateItensAmount(Order order) {
        for (OrderItem item : order.getItens()) {
            Product product = estoqueClient.findByIsbn(1l, item.getProduct());
            if (product.getAmount() < item.getAmount())
                throw new BusinessServiceException("Quantidade de produto insuficiente no estoque");
        }
    }

    private void validateItensExists(Order order) {
        for (OrderItem item : order.getItens()) {
            Product product = estoqueClient.findByIsbn(1l, item.getProduct());
            if (product == null)
                throw new BusinessServiceException("Produto nao encontrado");
        }
    }

    private void updateProductAmount(Order order) {
        for (OrderItem item : order.getItens()) {
            estoqueClient.subtractAmount(1l, item.getProduct(), item.getAmount());
        }
    }

    private Order getOrderById(String id) {
        Optional<Order> optionalOrder = orderRepository.findById(id);
        if (!optionalOrder.isPresent())
            throw new BusinessServiceException("Pedido não encontrado");
        return optionalOrder.get();
    }

}
