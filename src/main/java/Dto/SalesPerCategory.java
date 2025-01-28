package Dto;

import java.sql.Date;

public class SalesPerCategory {
    private String category;
    private Double totalSales;
    private Date transactionDate;

    public SalesPerCategory() {
    }

    public SalesPerCategory(String category, Date transactionDate, Double totalSales) {
        this.category = category;
        this.transactionDate = transactionDate;
        this.totalSales = totalSales;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(Double totalSales) {
        this.totalSales = totalSales;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }
}
