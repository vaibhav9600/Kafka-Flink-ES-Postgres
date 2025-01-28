package Dto;

import java.sql.Date;


public class SalesPerDay {
    private Date TransactionDate;
    private Double TotalSales;

    public SalesPerDay() {
    }

    public SalesPerDay(Date transactionDate, Double totalSales) {
        this.TransactionDate = transactionDate;
        this.TotalSales = totalSales;
    }

    public Date getTransactionDate() {
        return TransactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.TransactionDate = transactionDate;
    }

    public Double getTotalSales() {
        return TotalSales;
    }

    public void setTotalSales(Double totalSales) {
        this.TotalSales = totalSales;
    }
}
