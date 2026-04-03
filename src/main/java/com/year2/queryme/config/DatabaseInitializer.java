package com.year2.queryme.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- STARTING ROBUST DATABASE REPAIR ---");
        
        repairTable("users");
        repairTable("teachers");
        repairTable("courses");
        repairTable("students");
        repairTable("admins");
        repairTable("guests");
        repairTable("class_groups");
        
        System.out.println("--- DATABASE REPAIR COMPLETED ---");
    }

    private void repairTable(String tableName) {
        try {
            System.out.println(">>> 🛠️ Repairing table: " + tableName);
            
            // 1. Deep Clean: Remove null bytes, spaces, and non-numeric junk
            // This is the most common reason for 'Data truncated' errors in MySQL/MariaDB
            jdbcTemplate.execute("UPDATE " + tableName + " SET id = TRIM(REPLACE(id, '\\0', '')) WHERE id IS NOT NULL");
            
            // 2. Try to cast explicitly to numeric internal state
            jdbcTemplate.execute("UPDATE " + tableName + " SET id = CAST(id AS UNSIGNED) WHERE id IS NOT NULL AND id != ''");
            
            // 3. Perform the actual schema alteration to BIGINT AUTO_INCREMENT
            String sql = "ALTER TABLE " + tableName + " MODIFY id BIGINT AUTO_INCREMENT";
            jdbcTemplate.execute(sql);
            System.out.println("✅ " + tableName + " table is now HEALTHY and AUTO_INCREMENT is ON.");
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("already exists") || errorMsg.contains("Duplicate entry")) {
                System.out.println("⚠️ " + tableName + ": Found duplicate IDs during cleaning. You may need to manually delete duplicate rows.");
            } else if (errorMsg.contains("invalid use of group function")) {
               // ignore
            } else {
                System.out.println("ℹ️ " + tableName + " status: " + errorMsg);
            }
        }
    }
}
