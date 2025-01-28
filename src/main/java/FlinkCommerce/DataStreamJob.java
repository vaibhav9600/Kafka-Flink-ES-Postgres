/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package FlinkCommerce;

import Deserializer.JSONValueDeserializationSchema;
import Dto.SalesPerCategory;
import Dto.Transaction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;

import java.sql.Date;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class DataStreamJob {

    private static final String jdbcUrl = "jdbc:postgresql://localhost:5432/postgres";
    private static final String jdbcUsername = "postgres";
    private static final String jdbcPassword = "postgres";

    public static void main(String[] args) throws Exception {
        // Sets up the execution environment, which is the main entry point
        // to building Flink applications.
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        String topic = "financial_transactions";

        KafkaSource<Transaction> source = KafkaSource.<Transaction>builder()
                .setBootstrapServers("localhost:9092")
                .setTopics(topic)
                .setGroupId("flink-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new JSONValueDeserializationSchema())
                .build();

        DataStream<Transaction> transactionStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "kafka-source");
        transactionStream.print();

        JdbcExecutionOptions execOptions = new JdbcExecutionOptions.Builder()
                .withBatchSize(1000)
                .withBatchIntervalMs(2000)
                .withMaxRetries(5)
                .build();

        JdbcConnectionOptions connOptions = new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withDriverName("org.postgresql.Driver") // Make sure to add the PostgresSQL JDBC driver dependency in your build file (e.g., Maven or Gradle)
                .withUrl(jdbcUrl)
                .withPassword(jdbcPassword)
                .withUsername(jdbcUsername)
                .build();

        // create transactions table
        transactionStream.addSink(JdbcSink.sink(
                "CREATE TABLE IF NOT EXISTS transactions (" +
                        "transaction_id VARCHAR(255) PRIMARY KEY," +
                        "product_id VARCHAR(255)," +
                        "product_name VARCHAR(255)," +
                        "product_category VARCHAR(255)," +
                        "product_price DOUBLE PRECISION," +
                        "product_quantity INT," +
                        "product_brand VARCHAR(255)," +
                        "currency VARCHAR(10)," +
                        "customer_id VARCHAR(255)," +
                        "transaction_date TIMESTAMP," +
                        "payment_method VARCHAR(50)," +
                        "total_amount DOUBLE PRECISION)",
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {

                },
                execOptions,
                connOptions
        )).name("create transactions table sink");

        // create sales_per_category_table
        transactionStream.addSink(JdbcSink.sink(
                "CREATE TABLE IF NOT EXISTS sales_per_category(" +
                        "transaction_date DATE," +
                        "category VARCHAR(255)," +
                        "total_sales DOUBLE PRECISION," +
                        "PRIMARY KEY (transaction_date, category)" +
                        ")",
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {
                },
                execOptions,
                connOptions
        )).name("create sales_per_category_table sink");
        // create sales_per_day table
        transactionStream.addSink(JdbcSink.sink(
                "CREATE TABLE IF NOT EXISTS sales_per_day(" +
                        "transaction_date DATE," +
                        "total_sales DOUBLE PRECISION," +
                        "PRIMARY KEY (transaction_date)" +
                        ")",
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {
                },
                execOptions,
                connOptions
        )).name("create sales_per_day_table sink");

        // create sales_per_month table
        transactionStream.addSink(JdbcSink.sink(
                "CREATE TABLE IF NOT EXISTS sales_per_month(" +
                        "year INTEGER," +
                        "month INTEGER," +
                        "transaction_date DATE," +
                        "total_sales DOUBLE PRECISION," +
                        "PRIMARY KEY (year,month)" +
                        ")",
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {
                },
                execOptions,
                connOptions
        )).name("create sales_per_month_table sink");

        transactionStream.addSink(JdbcSink.sink(
                // Query to insert or update on conflict
                "INSERT INTO transactions (" +
                        "transaction_id, product_id, product_name, product_category, " +
                        "product_price, product_quantity, product_brand, currency, " +
                        "customer_id, transaction_date, payment_method, total_amount" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (transaction_id) DO UPDATE SET " +
                        "product_id = EXCLUDED.product_id, " +
                        "product_name = EXCLUDED.product_name, " +
                        "product_category = EXCLUDED.product_category, " +
                        "product_price = EXCLUDED.product_price, " +
                        "product_quantity = EXCLUDED.product_quantity, " +
                        "product_brand = EXCLUDED.product_brand, " +
                        "currency = EXCLUDED.currency, " +
                        "customer_id = EXCLUDED.customer_id, " +
                        "transaction_date = EXCLUDED.transaction_date, " +
                        "payment_method = EXCLUDED.payment_method, " +
                        "total_amount = EXCLUDED.total_amount",
                // JDBC Statement Builder to map `Transaction` fields to query parameters
                (JdbcStatementBuilder<Transaction>) (preparedStatement, transaction) -> {
                    preparedStatement.setString(1, transaction.getTransactionId());
                    preparedStatement.setString(2, transaction.getProductId());
                    preparedStatement.setString(3, transaction.getProductName());
                    preparedStatement.setString(4, transaction.getProductCategory());
                    preparedStatement.setDouble(5, transaction.getProductPrice());
                    preparedStatement.setInt(6, transaction.getProductQuantity());
                    preparedStatement.setString(7, transaction.getProductBrand());
                    preparedStatement.setString(8, transaction.getCurrency());
                    preparedStatement.setString(9, transaction.getCustomerId());
                    preparedStatement.setTimestamp(10, transaction.getTransactionDate());
                    preparedStatement.setString(11, transaction.getPaymentMethod());
                    preparedStatement.setDouble(12, transaction.getTotalAmount());
                },
                execOptions,
                connOptions
        )).name("Insert into transactions table sink");

        transactionStream.map(
                        transaction -> {
                            Date transactionDate = new java.sql.Date(System.currentTimeMillis());
                            String category = transaction.getProductCategory();
                            double totalSales = transaction.getTotalAmount();
                            return new SalesPerCategory(category, transactionDate, totalSales);
                        }
                ).keyBy(SalesPerCategory::getCategory)
                .reduce((salesPerCategory, t1) -> {
                    salesPerCategory.setTotalSales(salesPerCategory.getTotalSales() + t1.getTotalSales());
                    return salesPerCategory;
                }).addSink(JdbcSink.sink(
                        "INSERT INTO sales_per_category(transaction_date,category,total_sales) " +
                                "VALUES (?, ?, ?) " +
                                "ON CONFLICT (transaction_date, category) DO UPDATE SET " +
                                "total_sales = excluded.total_sales" +
                                "Where sales_per_category.category = EXCLUDED.category " +
                                "AND sales_per_category.transaction_date = EXCLUDED.transaction_date",

                        (JdbcStatementBuilder<SalesPerCategory>) (preparedStatement, salesPerCategory) -> {
                            preparedStatement.setDate(1, new Date(System.currentTimeMillis()));
                            preparedStatement.setString(2, salesPerCategory.getCategory());
                            preparedStatement.setDouble(3, salesPerCategory.getTotalSales());
                        },
                        execOptions,
                        connOptions
                )).name("Insert into sales_per_category table sink");
        // Execute program, beginning computation.
        env.execute("Flink Ecommerce Realtime Streaming");
    }
}
