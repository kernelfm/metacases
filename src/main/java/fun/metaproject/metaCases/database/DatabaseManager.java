package fun.metaproject.metaCases.database;
import java.io.File;
import java.sql.*;
import java.util.*;
import fun.metaproject.metaCases.MetaCasesFree;
import fun.metaproject.metaCases.case_system.HistoryEntry;
public class DatabaseManager {
    private final MetaCasesFree plugin;
    private Connection conn;
    public DatabaseManager(MetaCasesFree plugin) {
        this.plugin = plugin;
        connect();
        createTables();
    }
    private void connect() {
        File db = new File(this.plugin.getDataFolder(), "database.db");
        db.getParentFile().mkdirs();
        try {
            Class.forName("org.sqlite.JDBC");
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void createTables() {
        try {
            Statement s = this.conn.createStatement();
            try {
                s.execute("CREATE TABLE IF NOT EXISTS player_keys (uuid VARCHAR(36) NOT NULL, case_name VARCHAR(64) NOT NULL, amount INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (uuid, case_name));");
                s.execute("CREATE TABLE IF NOT EXISTS opening_history (id INTEGER PRIMARY KEY AUTOINCREMENT, case_name VARCHAR(64) NOT NULL, username VARCHAR(16) NOT NULL, reward_display TEXT NOT NULL, reward_material VARCHAR(64) NOT NULL, timestamp INTEGER NOT NULL);");
                s.execute("CREATE TABLE IF NOT EXISTS case_blocks (location VARCHAR(128) NOT NULL PRIMARY KEY, case_name VARCHAR(64) NOT NULL);");
                if (s != null) s.close();
            } catch (Throwable throwable) {
                if (s != null) try {
                    s.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void close() {
        try {
            if (this.conn != null && !this.conn.isClosed()) this.conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public Map<UUID, Map<String, Integer>> loadAllKeys() {
        Map<UUID, Map<String, Integer>> keys = new HashMap<>();
        try {
            Statement s = this.conn.createStatement();
            try {
                ResultSet rs = s.executeQuery("SELECT * FROM player_keys");
                try {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String cn = rs.getString("case_name").toLowerCase();
                        (keys.computeIfAbsent(uuid, k -> new HashMap<>())).put(cn, Integer.valueOf(rs.getInt("amount")));
                    }
                    if (rs != null) rs.close();
                } catch (Throwable throwable) {
                    if (rs != null) try {
                        rs.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                    throw throwable;
                }
                if (s != null) s.close();
            } catch (Throwable throwable) {
                if (s != null) try {
                    s.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return keys;
    }
    public void saveKey(UUID uuid, String caseName, int amount) {
        try {
            PreparedStatement ps = this.conn.prepareStatement("INSERT OR REPLACE INTO player_keys (uuid, case_name, amount) VALUES (?, ?, ?)");
            try {
                ps.setString(1, uuid.toString());
                ps.setString(2, caseName.toLowerCase());
                ps.setInt(3, amount);
                ps.executeUpdate();
                if (ps != null) ps.close();
            } catch (Throwable throwable) {
                if (ps != null) try {
                    ps.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public Map<String, String> loadAllBlocks() {
        Map<String, String> blocks = new HashMap<>();
        try {
            Statement s = this.conn.createStatement();
            try {
                ResultSet rs = s.executeQuery("SELECT * FROM case_blocks");
                try {
                    while (rs.next())
                        blocks.put(rs.getString("location"), rs.getString("case_name").toLowerCase());
                    if (rs != null) rs.close();
                } catch (Throwable throwable) {
                    if (rs != null) try {
                        rs.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                    throw throwable;
                }
                if (s != null) s.close();
            } catch (Throwable throwable) {
                if (s != null) try {
                    s.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return blocks;
    }
    public void saveBlock(String loc, String caseName) {
        try {
            PreparedStatement ps = this.conn.prepareStatement("INSERT OR REPLACE INTO case_blocks (location, case_name) VALUES (?, ?)");
            try {
                ps.setString(1, loc);
                ps.setString(2, caseName.toLowerCase());
                ps.executeUpdate();
                if (ps != null) ps.close();
            } catch (Throwable throwable) {
                if (ps != null) try {
                    ps.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void deleteBlock(String loc) {
        try {
            PreparedStatement ps = this.conn.prepareStatement("DELETE FROM case_blocks WHERE location = ?");
            try {
                ps.setString(1, loc);
                ps.executeUpdate();
                if (ps != null) ps.close();
            } catch (Throwable throwable) {
                if (ps != null) try {
                    ps.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public List<HistoryEntry> loadAllHistory() {
        List<HistoryEntry> list = new ArrayList<>();
        try {
            Statement s = this.conn.createStatement();
            try {
                ResultSet rs = s.executeQuery("SELECT * FROM opening_history ORDER BY timestamp DESC");
                try {
                    while (rs.next())
                        list.add(new HistoryEntry(rs.getString("case_name"), rs.getString("username"), rs.getString("reward_display"), rs.getString("reward_material"), rs.getTimestamp("timestamp")));
                    if (rs != null) rs.close();
                } catch (Throwable throwable) {
                    if (rs != null) try {
                        rs.close();
                    } catch (Throwable throwable1) {
                        throwable.addSuppressed(throwable1);
                    }
                    throw throwable;
                }
                if (s != null) s.close();
            } catch (Throwable throwable) {
                if (s != null) try {
                    s.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    public void addHistory(String caseName, String username, String rewardDisplay, String rewardMaterial, long ts) {
        try {
            PreparedStatement ps = this.conn.prepareStatement("INSERT INTO opening_history (case_name, username, reward_display, reward_material, timestamp) VALUES (?, ?, ?, ?, ?)");
            try {
                ps.setString(1, caseName);
                ps.setString(2, username);
                ps.setString(3, rewardDisplay);
                ps.setString(4, rewardMaterial);
                ps.setLong(5, ts);
                ps.executeUpdate();
                if (ps != null) ps.close();
            } catch (Throwable throwable) {
                if (ps != null) try {
                    ps.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void trimHistory() {
        try {
            PreparedStatement ps = this.conn.prepareStatement("DELETE FROM opening_history WHERE id NOT IN (SELECT id FROM opening_history ORDER BY timestamp DESC LIMIT 9)");
            try {
                ps.executeUpdate();
                if (ps != null) ps.close();
            } catch (Throwable throwable) {
                if (ps != null) try {
                    ps.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
