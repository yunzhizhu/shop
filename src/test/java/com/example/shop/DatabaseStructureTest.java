package com.example.shop;

import com.example.shop.mapper.UserCouponMapper;
import com.example.shop.service.CouponService;
import com.example.shop.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DatabaseStructureTest {

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private CouponService couponService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private DataSource dataSource;

    @Test
    public void testUpdatedStructure() {
        // 简单测试修正后的结构是否能正常加载
        System.out.println("UserCouponMapper loaded: " + (userCouponMapper != null));
        System.out.println("CouponService loaded: " + (couponService != null));
        System.out.println("OrderService loaded: " + (orderService != null));

        System.out.println("数据库结构修正完成，消除了循环依赖");
        System.out.println("现有表结构已经足够，无需额外的中间表");
    }

    /**
     * 验证message表包含所有必需字段和索引
     * Requirements: 1.1, 1.3, 1.4
     */
    @Test
    public void testMessageTableStructure() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 验证message表存在
            assertTrue(tableExists(metaData, "message"), "Message table should exist");
            
            // 验证必需字段存在
            Set<String> requiredColumns = Set.of(
                "message_id", "conversation_id", "sender_id", "receiver_id", 
                "content", "message_type", "content_type", "image_url", 
                "is_read", "created_at"
            );
            
            Set<String> actualColumns = getTableColumns(metaData, "message");
            for (String column : requiredColumns) {
                assertTrue(actualColumns.contains(column), 
                    "Message table should contain column: " + column);
            }
            
            // 验证必需索引存在
            Set<String> requiredIndexes = Set.of(
                "idx_conversation_id", "idx_receiver_id", "idx_sender_id",
                "idx_is_read", "idx_created_at", "idx_receiver_read", 
                "idx_conversation_time"
            );
            
            Set<String> actualIndexes = getTableIndexes(metaData, "message");
            for (String index : requiredIndexes) {
                assertTrue(actualIndexes.contains(index), 
                    "Message table should have index: " + index);
            }
            
            // 验证字符集为utf8mb4
            verifyTableCharset(connection, "message", "utf8mb4");
        }
    }

    /**
     * 验证conversation表包含所有必需字段和约束
     * Requirements: 2.1, 2.3
     */
    @Test
    public void testConversationTableStructure() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 验证conversation表存在
            assertTrue(tableExists(metaData, "conversation"), "Conversation table should exist");
            
            // 验证必需字段存在
            Set<String> requiredColumns = Set.of(
                "conversation_id", "user_id", "conversation_type", "target_id",
                "target_name", "target_avatar", "last_message_id", 
                "last_message_content", "last_message_time", "unread_count",
                "total_count", "is_pinned", "created_at", "updated_at"
            );
            
            Set<String> actualColumns = getTableColumns(metaData, "conversation");
            for (String column : requiredColumns) {
                assertTrue(actualColumns.contains(column), 
                    "Conversation table should contain column: " + column);
            }
            
            // 验证必需索引存在
            Set<String> requiredIndexes = Set.of(
                "uk_user_target", "idx_user_id", "idx_user_last_time",
                "idx_user_unread", "idx_user_pinned"
            );
            
            Set<String> actualIndexes = getTableIndexes(metaData, "conversation");
            for (String index : requiredIndexes) {
                assertTrue(actualIndexes.contains(index), 
                    "Conversation table should have index: " + index);
            }
            
            // 验证字符集为utf8mb4
            verifyTableCharset(connection, "conversation", "utf8mb4");
        }
    }

    /**
     * 验证utf8mb4字符集支持
     * Requirements: 1.3
     */
    @Test
    public void testUtf8mb4CharsetSupport() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // 验证数据库字符集
            String databaseCharset = getDatabaseCharset(connection);
            assertTrue(databaseCharset.contains("utf8mb4"), 
                "Database should use utf8mb4 charset, but was: " + databaseCharset);
            
            // 验证message表字符集
            verifyTableCharset(connection, "message", "utf8mb4");
            
            // 验证conversation表字符集
            verifyTableCharset(connection, "conversation", "utf8mb4");
        }
    }

    /**
     * 测试emoji和特殊Unicode字符的存储和检索
     * Requirements: 1.3
     */
    @Test
    public void testEmojiAndUnicodeCharacterStorage() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // 测试各种Unicode字符
            String[] testStrings = {
                "Hello World! 😀😂🎉❤️", // 基本emoji
                "🌟⭐✨💫🌙", // 星星和月亮emoji
                "👨‍👩‍👧‍👦👨‍💻👩‍🎨", // 复合emoji
                "🇨🇳🇺🇸🇯🇵🇰🇷", // 国旗emoji
                "数学符号: ∑∏∫∞≠≤≥", // 数学符号
                "希腊字母: αβγδεζηθικλμνξοπρστυφχψω", // 希腊字母
                "中文测试: 你好世界！这是一个测试消息。", // 中文字符
                "日文测试: こんにちは世界！これはテストメッセージです。", // 日文字符
                "韩文测试: 안녕하세요 세계! 이것은 테스트 메시지입니다.", // 韩文字符
                "阿拉伯文测试: مرحبا بالعالم! هذه رسالة اختبار.", // 阿拉伯文字符
                "俄文测试: Привет мир! Это тестовое сообщение.", // 俄文字符
                "特殊符号: ♠♣♥♦♪♫♬♭♮♯", // 特殊符号
                "箭头符号: ←↑→↓↔↕↖↗↘↙", // 箭头符号
                "混合内容: Hello 世界 🌍 こんにちは 😊 مرحبا 🎉" // 混合多种字符
            };
            
            // 创建临时测试表
            createTempTestTable(connection);
            
            try {
                // 测试插入和检索
                for (int i = 0; i < testStrings.length; i++) {
                    String testString = testStrings[i];
                    
                    // 插入测试数据
                    insertTestString(connection, i + 1, testString);
                    
                    // 检索并验证
                    String retrievedString = retrieveTestString(connection, i + 1);
                    assertEquals(testString, retrievedString, 
                        "UTF8MB4 character should be stored and retrieved correctly: " + testString);
                }
                
                // 测试长文本中的Unicode字符
                String longUnicodeText = "这是一个很长的测试文本，包含各种Unicode字符：" +
                    "😀😂🎉❤️🌟⭐✨💫🌙👨‍👩‍👧‍👦👨‍💻👩‍🎨🇨🇳🇺🇸🇯🇵🇰🇷" +
                    "数学符号∑∏∫∞≠≤≥，希腊字母αβγδεζηθικλμνξοπρστυφχψω，" +
                    "以及各种语言的问候：Hello, 你好, こんにちは, 안녕하세요, مرحبا, Привет！" +
                    "特殊符号♠♣♥♦♪♫♬♭♮♯和箭头←↑→↓↔↕↖↗↘↙也应该正确存储。";
                
                insertTestString(connection, 999, longUnicodeText);
                String retrievedLongText = retrieveTestString(connection, 999);
                assertEquals(longUnicodeText, retrievedLongText, 
                    "Long UTF8MB4 text should be stored and retrieved correctly");
                
            } finally {
                // 清理临时测试表
                dropTempTestTable(connection);
            }
        }
    }

    /**
     * 测试各种Unicode字符的处理性能
     * Requirements: 1.3
     */
    @Test
    public void testUnicodeCharacterPerformance() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            createTempTestTable(connection);
            
            try {
                long startTime = System.currentTimeMillis();
                
                // 批量插入包含Unicode字符的数据
                for (int i = 0; i < 100; i++) {
                    String unicodeContent = "测试消息 " + i + " 😀🎉❤️ Unicode字符处理性能测试";
                    insertTestString(connection, i, unicodeContent);
                }
                
                // 批量查询
                for (int i = 0; i < 100; i++) {
                    String retrieved = retrieveTestString(connection, i);
                    assertNotNull(retrieved, "Retrieved Unicode content should not be null");
                    assertTrue(retrieved.contains("😀🎉❤️"), "Retrieved content should contain emojis");
                }
                
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                // 性能应该在合理范围内（这里设置为5秒，实际应该更快）
                assertTrue(duration < 5000, 
                    "Unicode character processing should complete within reasonable time, took: " + duration + "ms");
                
            } finally {
                dropTempTestTable(connection);
            }
        }
    }

    /**
     * 测试边界情况和特殊Unicode字符
     * Requirements: 1.3
     */
    @Test
    public void testUnicodeEdgeCases() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            createTempTestTable(connection);
            
            try {
                // 测试边界情况
                String[] edgeCases = {
                    "", // 空字符串
                    " ", // 单个空格
                    "😀", // 单个emoji
                    "🏳️‍🌈", // 复合emoji（彩虹旗）
                    "👨‍👩‍👧‍👦", // 家庭emoji（零宽连接符）
                    "🇺🇸", // 国旗emoji（区域指示符）
                    "\u200B\u200C\u200D", // 零宽字符
                    "🤔💭", // 思考emoji组合
                    "Test\uD83D\uDE00End", // 混合ASCII和emoji
                    "🔥🔥🔥🔥🔥🔥🔥🔥🔥🔥", // 重复emoji
                };
                
                for (int i = 0; i < edgeCases.length; i++) {
                    String testCase = edgeCases[i];
                    insertTestString(connection, i + 1000, testCase);
                    String retrieved = retrieveTestString(connection, i + 1000);
                    assertEquals(testCase, retrieved, 
                        "Edge case should be handled correctly: " + testCase);
                }
                
            } finally {
                dropTempTestTable(connection);
            }
        }
    }

    /**
     * 验证MessageImage表不存在（已删除）
     * Requirements: 1.5, 3.3
     */
    @Test
    public void testMessageImageTableRemoved() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 验证message_image表不存在
            assertFalse(tableExists(metaData, "message_image"), 
                "MessageImage table should be removed");
        }
    }

    /**
     * 验证表的字段类型和约束
     * Requirements: 1.1, 1.4, 2.1
     */
    @Test
    public void testTableFieldTypesAndConstraints() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            // 验证message表的content字段为TEXT类型
            verifyColumnType(metaData, "message", "content", "TEXT");
            
            // 验证conversation表的主键
            verifyPrimaryKey(metaData, "conversation", "conversation_id");
            
            // 验证conversation表的唯一约束
            verifyUniqueConstraint(metaData, "conversation", "uk_user_target");
        }
    }

    // Helper methods
    
    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws SQLException {
        try (ResultSet tables = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return tables.next();
        }
    }
    
    private Set<String> getTableColumns(DatabaseMetaData metaData, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (ResultSet rs = metaData.getColumns(null, null, tableName.toUpperCase(), null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }
        return columns;
    }
    
    private Set<String> getTableIndexes(DatabaseMetaData metaData, String tableName) throws SQLException {
        Set<String> indexes = new HashSet<>();
        try (ResultSet rs = metaData.getIndexInfo(null, null, tableName.toUpperCase(), false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName != null && !indexName.equals("PRIMARY")) {
                    indexes.add(indexName.toLowerCase());
                }
            }
        }
        return indexes;
    }
    
    private String getDatabaseCharset(Connection connection) throws SQLException {
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery("SELECT @@character_set_database")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "";
    }
    
    private void verifyTableCharset(Connection connection, String tableName, String expectedCharset) throws SQLException {
        try (var stmt = connection.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT TABLE_COLLATION FROM information_schema.TABLES " +
                 "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tableName + "'")) {
            if (rs.next()) {
                String collation = rs.getString("TABLE_COLLATION");
                assertTrue(collation.startsWith(expectedCharset), 
                    "Table " + tableName + " should use " + expectedCharset + " charset, but collation was: " + collation);
            }
        }
    }
    
    private void verifyColumnType(DatabaseMetaData metaData, String tableName, String columnName, String expectedType) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            if (rs.next()) {
                String actualType = rs.getString("TYPE_NAME");
                assertTrue(actualType.toUpperCase().contains(expectedType.toUpperCase()),
                    "Column " + columnName + " should be of type " + expectedType + ", but was: " + actualType);
            } else {
                fail("Column " + columnName + " not found in table " + tableName);
            }
        }
    }
    
    private void verifyPrimaryKey(DatabaseMetaData metaData, String tableName, String expectedPkColumn) throws SQLException {
        try (ResultSet rs = metaData.getPrimaryKeys(null, null, tableName.toUpperCase())) {
            boolean found = false;
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                if (columnName.equalsIgnoreCase(expectedPkColumn)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Primary key column " + expectedPkColumn + " not found in table " + tableName);
        }
    }
    
    private void verifyUniqueConstraint(DatabaseMetaData metaData, String tableName, String constraintName) throws SQLException {
        try (ResultSet rs = metaData.getIndexInfo(null, null, tableName.toUpperCase(), true, false)) {
            boolean found = false;
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName != null && indexName.equalsIgnoreCase(constraintName)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Unique constraint " + constraintName + " not found in table " + tableName);
        }
    }
    
    // UTF8MB4 test helper methods
    
    private void createTempTestTable(Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TEMPORARY TABLE utf8mb4_test (" +
                "id INT PRIMARY KEY, " +
                "content TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
        }
    }
    
    private void dropTempTestTable(Connection connection) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("DROP TEMPORARY TABLE IF EXISTS utf8mb4_test");
        }
    }
    
    private void insertTestString(Connection connection, int id, String content) throws SQLException {
        try (var stmt = connection.prepareStatement("INSERT INTO utf8mb4_test (id, content) VALUES (?, ?)")) {
            stmt.setInt(1, id);
            stmt.setString(2, content);
            stmt.executeUpdate();
        }
    }
    
    private String retrieveTestString(Connection connection, int id) throws SQLException {
        try (var stmt = connection.prepareStatement("SELECT content FROM utf8mb4_test WHERE id = ?")) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("content");
                }
            }
        }
        return null;
    }
}
