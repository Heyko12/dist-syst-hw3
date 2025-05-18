package simpleservice;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ShardConfig {

    @Autowired
    private Environment env;

    @Bean
    public Map<Integer, JdbcTemplate> shardTemplates() {
        int shardCount = Integer.parseInt(env.getProperty("APP_SHARD_COUNT", "8"));
        Map<Integer, JdbcTemplate> map = new HashMap<>();
        for (int i = 1; i <= shardCount; i++) {
            String urlPropertyName = "SHARD" + i + "_URL";
            DataSource ds = DataSourceBuilder.create()
                .url(env.getProperty(urlPropertyName))
                .username(env.getProperty("SPRING_DATASOURCE_USERNAME"))
                .password(env.getProperty("SPRING_DATASOURCE_PASSWORD"))
                .driverClassName(env.getProperty("SPRING_DATASOURCE_DRIVER_CLASS_NAME"))
                .build();
            map.put(i - 1, new JdbcTemplate(ds));
        }
        return map;
    }
}