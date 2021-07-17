package io.github.unterstein.services.datascrapers;

import com.binance.api.client.domain.account.AssetBalance;
import io.github.unterstein.services.BinanceService;
import io.github.unterstein.services.InfluxDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/*
*  Class to scrap data from Binance
*/
@Component
public class BinanceDataScraper {

  @Autowired
  private InfluxDbService loginService;

  @Autowired
  private BinanceService client;

  private static Logger logger = LoggerFactory.getLogger(BinanceDataScraper.class);

  BinanceDataScraper() {
    logger.info("Starting Binance data scraper");
  }

  public void tick() {
    double lastPrice = 0;
    try {
      lastPrice = client.lastPrice("ETHUSDT");
      logger .debug(String.format("Last price:%s",lastPrice));
      loginService.logIndicators(lastPrice);
      client.getOrderBook("ETHUSDT");
    } catch (Exception e) {
      logger.error("Unable to scrap all the data", e);
    }
  }

  public List<AssetBalance> getBalances() {
    return client.getBalances();
  }

  // tick every 3 seconds
  @Scheduled(fixedRate = 5000)
  public void schedule() {
    this.tick();
  }
}
