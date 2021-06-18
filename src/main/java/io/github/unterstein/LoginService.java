package io.github.unterstein;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/*
 *  Class used to push/pull data to InfluxDB
 */
@Component
public class LoginService {

    public static Boolean enabled;

    private static InfluxDB influxDB;

    public LoginService(@Value("${INFLUXDB_URL}") String influxUrl,
                        @Value("${INFLUXDB_DATABASE}") String influxDatabase,
                        @Value("${INFLUXDB_USER}") String influxUser,
                        @Value("${INFLUXDB_PASS}") String influxPass,
                        @Value("${INFLUXDB_ENABLED}") Boolean isEnalbed){
        this.enabled = isEnalbed;
        if (this.influxDB == null){
            if (influxUser.isEmpty())
                this.influxDB = InfluxDBFactory.connect(influxUrl);
            else
                this.influxDB = InfluxDBFactory.connect(influxUrl,influxUser,influxPass);
            this.influxDB.setDatabase(influxDatabase);
        }
    }

    void logIndicators(double lastPrice){
        influxDB.write(Point.measurement("binance-bot")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("lastPrice",lastPrice)
                .build());
    }
}
