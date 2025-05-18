package simpleservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ApplicationStarter {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationStarter.class, args);
    }

    @Bean
    public ApplicationRunner prepopulate(Map<Integer, JdbcTemplate> shardTemplates) {
        return args -> {
            int totalRows = 5_000_000;
            int shardCount = shardTemplates.size();
            Map<Integer, StringBuilder> buffers = new HashMap<>();
            for (int shardId = 0; shardId < shardCount; shardId++) {
                buffers.put(shardId, new StringBuilder());
            }

            for (int i = 0; i < totalRows; i++) {
                String key = "key_" + i;
                String value = "val_" + i;
                int shardId = Math.abs(key.hashCode()) % shardCount;
                buffers.get(shardId).append(key).append(',').append(value).append('\n');
            }

            for (Map.Entry<Integer, JdbcTemplate> entry : shardTemplates.entrySet()) {
                int shardId = entry.getKey();
                JdbcTemplate tpl = entry.getValue();
                DataSource ds = tpl.getDataSource();
                String csvData = buffers.get(shardId).toString();

                System.out.printf("Populating shard %d with rows via COPY...%n", shardId, totalRows);
                try (var conn = ds.getConnection()) {
                    PGConnection pgConn = conn.unwrap(PGConnection.class);
                    CopyManager copyManager = pgConn.getCopyAPI();
                    Reader reader = new StringReader(csvData);
                    long inserted = copyManager.copyIn("COPY data (key, value) FROM STDIN WITH (FORMAT csv)", reader);
                    System.out.printf("Shard %d: inserted %d rows%n", shardId, inserted);
                }
            }

            System.out.println("Pre-population completed on all shards.");
        };
    }
}
