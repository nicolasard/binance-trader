package io.github.unterstein.services.datascrapers;

import com.binance.api.client.domain.account.AssetBalance;
import io.github.unterstein.services.BinanceService;
import io.github.unterstein.services.InfluxDbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  BinanceDataScraper(@Value("${BASE_CURRENCY:XVG}") String baseCurrency,
                     @Value("${TRADE_CURRENCY:XVG}") String tradeCurrency) {
    logger.info("Starting Binance data scraper");
  }

  public void tick() {
    double lastPrice = 0;
    try {
      lastPrice = client.lastPrice("ETHUSDT");
      logger .debug(String.format("Last price:%s",lastPrice));
      if(loginService.enabled) {
        loginService.logIndicators(lastPrice);
      }

      client.getOrderBook("ETHUSDT");

    } catch (Exception e) {
      logger.error("Unable to scrap all the data", e);
    }
  }

  public List<AssetBalance> getBalances() {
    return client.getBalances();
  }
}
