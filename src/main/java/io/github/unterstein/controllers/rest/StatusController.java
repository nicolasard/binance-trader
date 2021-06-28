package io.github.unterstein.controllers.rest;

import com.binance.api.client.domain.account.AssetBalance;
import io.github.unterstein.services.BinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("status")
public class StatusController {

    @Autowired
    private BinanceService service;

    @RequestMapping("/balance")
    public List<AssetBalance> getBalances() {
        return service.getBalances().stream().filter(assetBalance -> !assetBalance.getFree().startsWith("0.0000")).collect(Collectors.toList());
    }
}
