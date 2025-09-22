package amgn.amu.config;

import amgn.amu.service.KakaoPayGateway;
import amgn.amu.service.KgpPaymentGateway;
import amgn.amu.service.TossPaymentGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentGatewayConfig {

    @Bean
    public KgpPaymentGateway kgpPaymentGateway() {
        return new KgpPaymentGateway();
    }

    @Bean
    public TossPaymentGateway tossPaymentGateway() {
        return new TossPaymentGateway();
    }

    @Bean
    public KakaoPayGateway kakaoPayGateway() {
        return new KakaoPayGateway();
    }
}
