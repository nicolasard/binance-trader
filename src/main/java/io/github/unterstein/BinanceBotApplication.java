package io.github.unterstein;

import com.binance.api.client.domain.account.AssetBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@EnableScheduling
@SpringBootApplication
@RestController("/")
public class BinanceBotApplication {

  private static Logger logger = LoggerFactory.getLogger(BinanceBotApplication.class);

  @Autowired
  private BinanceTrader trader;

  @PostConstruct
  public void init() {
    logger.info(String.format("Starting app..."));
  }

  // tick every 3 seconds
  @Scheduled(fixedRate = 3000)
  public void schedule() {
    trader.tick();
  }

  @RequestMapping("/")
  public List<AssetBalance> getBalances() {
    return trader.getBalances().stream().filter(assetBalance -> !assetBalance.getFree().startsWith("0.0000")).collect(Collectors.toList());
  }

  public static void main(String[] args) {
    SpringApplication.run(BinanceBotApplication.class);
  }
}
