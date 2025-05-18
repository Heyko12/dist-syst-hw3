package simpleservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping("/db")
public class DBController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS data (key VARCHAR(255) PRIMARY KEY, value VARCHAR(255))");
    }

    @PostMapping("/put")
    public ResponseEntity<String> put(@RequestParam String key, @RequestParam String value) {
        try {
            jdbcTemplate.update("INSERT INTO data (key, value) VALUES (?, ?) ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value", key, value);
            return ResponseEntity.ok("Value saved");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/get")
    public ResponseEntity<String> get(@RequestParam String key) {
        try {
            String value = jdbcTemplate.queryForObject("SELECT value FROM data WHERE key = ?", String.class, new Object[]{key});
            return ResponseEntity.ok(value);
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
