package io.github.unterstein.services;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/*
 *  Class used to work with Binance
 */
@Component
public class BinanceService {

  private static Logger logger = LoggerFactory.getLogger(BinanceService.class);
  private static BinanceApiRestClient client;

  public BinanceService(@Value("${BINANCE_KEY}") String binanceKey,
                        @Value("${BINANCE_SECRET}") String binanceSecret) {
      final BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(binanceKey, binanceSecret);
      this.client = factory.newRestClient();
  }

  // The bid price represents the maximum price that a buyer is willing to pay for a security.
  // The ask price represents the minimum price that a seller is willing to receive.
  public OrderBook getOrderBook(String symbol) {
      OrderBook orderBook = client.getOrderBook(symbol, 400);
      List<OrderBookEntry> orderAsks = orderBook.getAsks();
      List<String> orderBids = orderBook.getBids().stream().map(s ->s.getPrice()).collect(Collectors.toList());
      return client.getOrderBook(symbol, 400);
  }

  public AssetBalance getBaseBalance(String baseCurrency) {
    return client.getAccount().getAssetBalance(baseCurrency);
  }

  public AssetBalance getTradingBalance(String tradeCurrency) {
    return client.getAccount().getAssetBalance(tradeCurrency);
  }

  public double assetBalanceToDouble(AssetBalance balance) {
    return Double.valueOf(balance.getFree()) + Double.valueOf(balance.getLocked());
  }

  public double getAllTradingBalance(String tradeCurrency) {
    AssetBalance tradingBalance = getTradingBalance(tradeCurrency);
    return assetBalanceToDouble(tradingBalance);
  }

  public boolean tradingBalanceAvailable(AssetBalance tradingBalance) {
    return assetBalanceToDouble(tradingBalance) > 1;
  }

  public List<AssetBalance> getBalances() {
    return client.getAccount().getBalances();
  }

  public List<Order> getOpenOrders(String symbol) {
    OrderRequest request = new OrderRequest(symbol);
    return client.getOpenOrders(request);
  }

  public void cancelAllOrders(String symbol) {
    getOpenOrders(symbol).forEach(order -> client.cancelOrder(new CancelOrderRequest(symbol, order.getOrderId())));
  }

  // * GTC (Good-Til-Canceled) orders are effective until they are executed or canceled.
  // * IOC (Immediate or Cancel) orders fills all or part of an order immediately and cancels the remaining part of the order.
  public NewOrderResponse buy(String symbol,int quantity, double price) {
    String priceString = String.format("%.8f", price).replace(",", ".");
    logger.info(String.format("Buying %d for %s\n", quantity, priceString));
    NewOrder order = new NewOrder(symbol, OrderSide.BUY, OrderType.LIMIT, TimeInForce.GTC, "" + quantity, priceString);
    return client.newOrder(order);
  }

  public void sell(String symbol,int quantity, double price) {
    String priceString = String.format("%.8f", price).replace(",", ".");
    logger.info(String.format("Selling %d for %s\n", quantity, priceString));
    NewOrder order = new NewOrder(symbol, OrderSide.SELL, OrderType.LIMIT, TimeInForce.GTC, "" + quantity, priceString);
    client.newOrder(order);
  }

  public void sellMarket(String symbol,int quantity) {
    if (quantity > 0) {
      logger.info("Selling to MARKET with quantity " + quantity);
      NewOrder order = new NewOrder(symbol, OrderSide.SELL, OrderType.MARKET, null, "" + quantity);
      client.newOrder(order);
    } else {
      logger.info("not executing - 0 quantity sell");
    }
  }

  public Order getOrder(String symbol,long orderId) {
    return client.getOrderStatus(new OrderStatusRequest(symbol, orderId));
  }

  public double lastPrice(String symbol) {
    return Double.valueOf(client.get24HrPriceStatistics(symbol).getLastPrice());
  }

  public void cancelOrder(String symbol,long orderId) {
    logger.info("Cancelling order " + orderId);
    client.cancelOrder(new CancelOrderRequest(symbol, orderId));
  }

  public void panicSell(String symbol, double lastKnownAmount, double lastKnownPrice) {
    logger.error("!!!! PANIC SELL !!!!");
    logger.warn(String.format("Probably selling %.8f for %.8f", lastKnownAmount, lastKnownPrice));
    cancelAllOrders(symbol);
    sellMarket(symbol,Double.valueOf(getTradingBalance("fsf").getFree()).intValue());
  }
}
