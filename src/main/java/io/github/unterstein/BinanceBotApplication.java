package io.github.unterstein;

import io.github.unterstein.services.datascrapers.BinanceDataScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@EnableScheduling
@SpringBootApplication
@RestController("/")
public class BinanceBotApplication {

  private static Logger logger = LoggerFactory.getLogger(BinanceBotApplication.class);

  @Autowired
  private BinanceDataScraper trader;

  @PostConstruct
  public void init() {
    logger.info(String.format("Starting app..."));
  }

  // tick every 3 seconds
  @Scheduled(fixedRate = 3000)
  public void schedule() {
    trader.tick();
  }

  public static void main(String[] args) {
    SpringApplication.run(BinanceBotApplication.class);
  }
}
