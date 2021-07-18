package io.github.unterstein.services;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/*
 *  Class used to push/pull data to InfluxDB
 */
@Component
public class InfluxDbService {

    private static Logger logger = LoggerFactory.getLogger(InfluxDbService.class);

    public Boolean enabled;

    private static InfluxDB influxDB;

    @Value("${influxdb.url}")
    private String influxUrl;

    @Value("${influxdb.database}")
    private String influxDatabase;

    @Value("${influxdb.user}")
    private String influxUser;

    @Value("${influxdb.pass}")
    private String influxPass;

    @PostConstruct
    public synchronized void initUsingSystemProperties(){
        logger.info("Initializing InfluxDB Service");
        if (this.influxDB == null && this.influxUrl != null && this.influxDatabase != null){
            logger.info(String.format("InfluxDB service wasn't initialized. Creating new influxDB object.[INFLUXDB_URL: %s; INFLUXDB_DATABASE: %s; INFLUXDB_USER: %s]",influxUrl,influxDatabase,influxUser));
            if (influxUser.isEmpty())
                this.influxDB = InfluxDBFactory.connect(influxUrl);
            else
                this.influxDB = InfluxDBFactory.connect(influxUrl,influxUser,influxPass);
            this.influxDB.setDatabase(influxDatabase);
        }
    }

    public void logIndicators(double lastPrice){
        if (influxDB == null){
            logger.error("influxDB not initialized.");
            return;
        }
        influxDB.write(Point.measurement("binance-bot")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("lastPrice3",lastPrice)
                .build());
    }
}
