package io.github.unterstein.services;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/*
 *  Class used to push/pull data to InfluxDB
 */
@Component
public class InfluxDbService {

    private static Logger logger = LoggerFactory.getLogger(InfluxDbService.class);

    public Boolean enabled;

    private InfluxDB influxDB;

    public InfluxDbService(@Value("${INFLUXDB_URL}") String influxUrl,
                           @Value("${INFLUXDB_DATABASE}") String influxDatabase,
                           @Value("${INFLUXDB_USER}") String influxUser,
                           @Value("${INFLUXDB_PASS}") String influxPass,
                           @Value("${INFLUXDB_ENABLED}") Boolean isEnalbed){
        this.enabled = isEnalbed;
        if (this.influxDB == null){
            logger.info(String.format("InfluxDB service wasn't initialized. Creating new influxDB object.[INFLUXDB_URL: %s; INFLUXDB_DATABASE: %s; INFLUXDB_USER: %s]",influxUrl,influxDatabase,influxUser));
            if (influxUser.isEmpty())
                this.influxDB = InfluxDBFactory.connect(influxUrl);
            else
                this.influxDB = InfluxDBFactory.connect(influxUrl,influxUser,influxPass);
            this.influxDB.setDatabase(influxDatabase);
        }
    }

    public void logIndicators(double lastPrice){
        influxDB.write(Point.measurement("binance-bot")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("lastPrice3",lastPrice)
                .build());
    }
}
