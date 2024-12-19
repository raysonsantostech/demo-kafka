package br.com.raysonsatos.producer_a.scheduler;

import br.com.raysonsatos.producer_a.service.ProducerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MessageScheduler {

    private final ProducerService producerService;

    public MessageScheduler(ProducerService producerService) {
        this.producerService = producerService;
    }

    @Scheduled(fixedRate = 5000)
    public void sendMessage() {
        System.out.println("Sending message... ");
        producerService.publishMessage();
    }
}
