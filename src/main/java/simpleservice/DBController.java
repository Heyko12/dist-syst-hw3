package simpleservice;

import java.util.Map;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/db")
public class DBController {

    private final Map<Integer, JdbcTemplate> shardTemplates;
    private final int shardCount;

    @Autowired
    public DBController(Map<Integer, JdbcTemplate> shardTemplates) {
        this.shardTemplates = shardTemplates;
        this.shardCount = shardTemplates.size();
    }

    @PostConstruct
    public void init() {
        shardTemplates.values().forEach(tpl -> tpl.execute("CREATE TABLE IF NOT EXISTS data (key VARCHAR(255) PRIMARY KEY, value VARCHAR(255))"));
    }

    private int calculateShard(String key) {
        return Math.abs(key.hashCode()) % shardCount;
    }

    @PostMapping("/put")
    public ResponseEntity<String> put(@RequestParam String key, @RequestParam String value) {
        int shard = calculateShard(key);
        JdbcTemplate tpl = shardTemplates.get(shard);
        try {
            tpl.update("INSERT INTO data (key, value) VALUES (?, ?) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value", key, value);
            return ResponseEntity.ok("Value saved");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/get")
    public ResponseEntity<String> get(@RequestParam String key) {
        int shard = calculateShard(key);
        JdbcTemplate tpl = shardTemplates.get(shard);
        try {
            String value = tpl.queryForObject("SELECT value FROM data WHERE key = ?", String.class, new Object[]{key});
            return ResponseEntity.ok(value);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }
}