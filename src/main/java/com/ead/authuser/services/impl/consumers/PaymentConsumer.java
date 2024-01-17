package com.ead.authuser.services.impl.consumers;

import com.ead.authuser.dtos.PaymentEventDto;
import com.ead.authuser.enums.PaymentControl;
import com.ead.authuser.enums.RoleType;
import com.ead.authuser.enums.UserType;
import com.ead.authuser.models.UserModel;
import com.ead.authuser.services.RoleService;
import com.ead.authuser.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class PaymentConsumer {

    final UserService userService;
    final RoleService roleService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${ead.broker.queue.paymentEventQueue.name}", durable = "true"),
            exchange = @Exchange(value = "${ead.broker.exchange.paymentEvent}", type = ExchangeTypes.FANOUT, ignoreDeclarationExceptions = "true")
    ))
    public void listenPaymentEvent(@Payload PaymentEventDto paymentEventDto) {
        UserModel userModel = userService.findById(paymentEventDto.getUserId()).get();
        var roleModel = roleService.findByRoleName(RoleType.ROLE_STUDENT).orElseThrow(() -> new RuntimeException("Error: Role not found."));

        switch (PaymentControl.valueOf(paymentEventDto.getPaymentControl())) {
            case EFFECTED:
                if (userModel.getUserType().equals(UserType.USER)) {
                    userModel.setUserType(UserType.STUDENT);
                    userModel.getRoles().add(roleModel);
                    userService.updateUser(userModel);
                }
                break;
            case REFUSED:
                if (userModel.getUserType().equals(UserType.STUDENT)) {
                    userModel.setUserType(UserType.USER);
                    userModel.getRoles().remove(roleModel);
                    userService.updateUser(userModel);
                }
                break;
            case ERROR:
                log.error("Payment error");
                break;
        }
    }
}

