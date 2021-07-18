package io.github.unterstein.services;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.market.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/*
 *  Class used to work with Binance
 */
@Service
public class BinanceService {

  private static Logger logger = LoggerFactory.getLogger(BinanceService.class);

  private static BinanceApiRestClient client;

  private final String BINANCE_SECRET_CONFIG = "binance.secret";

  private final String BINANCE_KEY_CONFIG = "binance.key";

  @Value("${binance.key}")
  private String binanceKey;

  @Value("${binance.secret}")
  private String binanceSecret;

  @PostConstruct
  public synchronized void initUsingSystemProperties(){
          logger.info("Initializing Binance Service");
          if (this.binanceKey == null || this.binanceSecret == null){
              this.binanceKey=System.getProperty(BINANCE_KEY_CONFIG);
              this.binanceSecret=System.getProperty(BINANCE_SECRET_CONFIG);
          }

          if (this.client==null){
              final BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(binanceKey, binanceSecret);
              this.client = factory.newRestClient();
          }
  }

  // The bid price represents the maximum price that a buyer is willing to pay for a security.
  // The ask price represents the minimum price that a seller is willing to receive.
  public OrderBook getOrderBook(String symbol) {
      return client.getOrderBook(symbol, 400);
  }

  public List<AssetBalance> getBalances() {
    return client.getAccount().getBalances();
  }

  public double lastPrice(String symbol) {
    return Double.valueOf(client.get24HrPriceStatistics(symbol).getLastPrice());
  }

}
