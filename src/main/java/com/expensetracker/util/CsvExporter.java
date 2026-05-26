package com.expensetracker.util;

import com.expensetracker.model.Expense;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Utility for exporting expense lists to CSV files.
 */
public class CsvExporter {
    
    /**
     * Exports a list of expenses to the specified file.
     */
    public static boolean exportToCsv(List<Expense> expenses, File file) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write CSV Header
            writer.println("Expense ID,Date,Category,Description,Amount");
            
            for (Expense exp : expenses) {
                String desc = exp.getDescription();
                if (desc != null) {
                    desc = desc.replace("\"", "\"\"");
                    if (desc.contains(",") || desc.contains("\"") || desc.contains("\n")) {
                        desc = "\"" + desc + "\"";
                    }
                } else {
                    desc = "";
                }
                
                writer.printf("%d,%s,%s,%s,%.2f\n",
                        exp.getId(),
                        exp.getDate().toString(),
                        exp.getCategory(),
                        desc,
                        exp.getAmount()
                );
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
