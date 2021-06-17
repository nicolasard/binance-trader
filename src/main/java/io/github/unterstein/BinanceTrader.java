package io.github.unterstein;

import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.market.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BinanceTrader {

  @Autowired
  private LoginService loginService;

  private static Logger logger = LoggerFactory.getLogger(BinanceTrader.class);

  private TradingClient client;
  private final double tradeDifference;
  private final double tradeProfit;
  private final int tradeAmount;

  private Double currentlyBoughtPrice;
  private Long orderId;
  private int panicBuyCounter;
  private int panicSellCounter;
  private double trackingLastPrice;

  BinanceTrader(@Value("${TRADE_DIFFERENCE:0.00000001}") double tradeDifference,
                @Value("${TRADE_PROFIT:1.3}") double tradeProfit,
                @Value("${TRADE_AMOUNT:150}") int tradeAmount,
                @Value("${BASE_CURRENCY:XVG}") String baseCurrency,
                @Value("${TRADE_CURRENCY:XVG}") String tradeCurrency,
                @Value("${API_KEY}") String key,
                @Value("${API_SECRET}") String secret) {
    client = new TradingClient(baseCurrency, tradeCurrency, key, secret);
    trackingLastPrice = client.lastPrice();
    this.tradeAmount = tradeAmount;
    this.tradeProfit = tradeProfit;
    this.tradeDifference = tradeDifference;
    clear();
  }

  void tick() {
    double lastPrice = 0;
    try {
      OrderBook orderBook = client.getOrderBook();
      lastPrice = client.lastPrice();
      AssetBalance tradingBalance = client.getTradingBalance();
      double lastKnownTradingBalance = client.getAllTradingBalance();
      double lastBid = Double.valueOf(orderBook.getBids().get(0).getPrice());
      double lastAsk = Double.valueOf(orderBook.getAsks().get(0).getPrice());
      double buyPrice = lastBid + tradeDifference;
      double sellPrice = lastAsk - tradeDifference;
      double profitablePrice = buyPrice + (buyPrice * tradeProfit / 100);
      logger.info(String.format("buyPrice:%.8f sellPrice:%.8f bid:%.8f ask:%.8f price:%.8f profit:%.8f diff:%.8f", buyPrice, sellPrice, lastBid, lastAsk, lastPrice, profitablePrice, (lastAsk - profitablePrice)));
      if(loginService.enabled) {
        loginService.logIndicators(lastPrice);
      }
      if (orderId == null) {
        logger.info("nothing bought, let`s check");
        // find a burst to buy
        // but make sure price is ascending!
        if (lastAsk >= profitablePrice) {
          if (lastPrice > trackingLastPrice) {
            logger.info("Buy burst detected");
            currentlyBoughtPrice = profitablePrice;
            orderId = client.buy(tradeAmount, buyPrice).getOrderId();
            panicBuyCounter = 0;
            panicSellCounter = 0;
          } else {
            logger.warn("woooops, price is falling?!? don`t do something!");
            panicSellForCondition(lastPrice, lastKnownTradingBalance, client.tradingBalanceAvailable(tradingBalance));
          }
        } else {
          logger.info(String.format("No profit detected, difference %.8f", lastAsk - profitablePrice));
          currentlyBoughtPrice = null;
          panicSellForCondition(lastPrice, lastKnownTradingBalance, client.tradingBalanceAvailable(tradingBalance));
        }
      } else {
        Order order = client.getOrder(orderId);
        OrderStatus status = order.getStatus();
        if (status != OrderStatus.CANCELED) {
          // not new and not canceled, check for profit
          logger.info("Tradingbalance: " + tradingBalance);
          if ("0".equals("" + tradingBalance.getLocked().charAt(0)) &&
              lastAsk >= currentlyBoughtPrice) {
            if (status == OrderStatus.NEW) {
              // nothing happened here, maybe cancel as well?
              panicBuyCounter++;
              logger.info(String.format("order still new, time %d\n", panicBuyCounter));
              if (panicBuyCounter > 4) {
                client.cancelOrder(orderId);
                clear();
              }
            } else {
              if ("0".equals("" + tradingBalance.getFree().charAt(0))) {
                logger.warn("no balance in trading money, clearing out");
                clear();
              } else if (status == OrderStatus.PARTIALLY_FILLED || status == OrderStatus.FILLED) {
                logger.info("Order filled with status " + status);
                if (lastAsk >= profitablePrice) {
                  logger.info("still gaining profitable profits HODL!!");
                } else {
                  logger.info("Not gaining enough profit anymore, let`s sell");
                  logger.info(String.format("Bought %d for %.8f and sell it for %.8f, this is %.8f coins profit", tradeAmount, currentlyBoughtPrice, sellPrice, (1.0 * currentlyBoughtPrice - sellPrice) * tradeAmount));
                  client.sell(tradeAmount, sellPrice);
                }
              } else {
                // WTF?!
                logger.error("DETECTED WTF!!!!!");
                logger.error("Order: " + order + " , Order-Status: " + status);
                client.panicSell(lastKnownTradingBalance, lastPrice);
                clear();
              }
            }
          } else {
            panicSellCounter++;
            logger.info(String.format("sell request not successful, increasing time %d\n", panicSellCounter));
            panicSellForCondition(lastPrice, lastKnownTradingBalance, panicSellCounter > 3);
          }
        } else {
          logger.warn("Order was canceled, cleaning up.");
          clear(); // Order was canceled, so clear and go on
        }
      }
    } catch (Exception e) {
      logger.error("Unable to perform ticker", e);
    }
    trackingLastPrice = lastPrice;
  }

  private void panicSellForCondition(double lastPrice, double lastKnownTradingBalance, boolean condition) {
    if (condition) {
      logger.info("panicSellForCondition");
      client.panicSell(lastKnownTradingBalance, lastPrice);
      clear();
    }
  }

  private void clear() {
    panicBuyCounter = 0;
    panicSellCounter = 0;
    orderId = null;
    currentlyBoughtPrice = null;
  }

  List<AssetBalance> getBalances() {
    return client.getBalances();
  }
}
