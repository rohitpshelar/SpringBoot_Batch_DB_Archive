package com.example.archivebatch.job;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.example.archivebatch.model.ArchiveRecord;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * Writes ArchiveRecords into archive DB, then deletes corresponding rows from main DB.
 * If delete fails, attempts to delete previously inserted archive rows as a compensating action.
 */
public class ArchiveThenDeleteWriter implements ItemWriter<com.example.archivebatch.model.ArchiveRecord> {

    private final JdbcTemplate archiveJdbc;
    private final JdbcTemplate mainJdbc;
    private final TransactionTemplate archiveTx;
    private final TransactionTemplate mainTx;

    public ArchiveThenDeleteWriter(DataSource archiveDs, DataSource mainDs) {
        this.archiveJdbc = new JdbcTemplate(archiveDs);
        this.mainJdbc = new JdbcTemplate(mainDs);

        DataSourceTransactionManager atm = new DataSourceTransactionManager(archiveDs);
        DataSourceTransactionManager mtm = new DataSourceTransactionManager(mainDs);
        this.archiveTx = new TransactionTemplate(atm);
        this.mainTx = new TransactionTemplate(mtm);
    }

    @Override
    public void write(Chunk<? extends com.example.archivebatch.model.ArchiveRecord> items) throws Exception {
        if (items == null || items.isEmpty()) return;

        final List<Long> ids = items.getItems().stream().map(com.example.archivebatch.model.ArchiveRecord::getId).collect(Collectors.toList());

        // 1) Insert into archive DB in a transaction
        archiveTx.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(org.springframework.transaction.TransactionStatus status) {
                String sql = "INSERT INTO my_table (id, data, archived_at) VALUES (?, ?, ?)";
                for (com.example.archivebatch.model.ArchiveRecord r : items) {
                    archiveJdbc.update(sql, r.getId(), r.getData(), Timestamp.valueOf(r.getArchivedAt()));
                }
            }
        });

        // 2) Delete from main DB in a transaction
        try {
            mainTx.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(org.springframework.transaction.TransactionStatus status) {
                    String inSql = ids.stream().map(i -> "?").collect(Collectors.joining(","));
                    String sql = "DELETE FROM my_table WHERE id IN (" + inSql + ")";
                    Object[] params = ids.toArray();
                    mainJdbc.update(sql, params);
                }
            });
        } catch (Exception ex) {
            // Compensating action: delete inserted archive rows
            try {
                archiveTx.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(org.springframework.transaction.TransactionStatus status) {
                        String inSql = ids.stream().map(i -> "?").collect(Collectors.joining(","));
                        String sql = "DELETE FROM my_table WHERE id IN (" + inSql + ")";
                        Object[] params = ids.toArray();
                        archiveJdbc.update(sql, params);
                    }
                });
            } catch (Exception ex2) {
                // If compensation fails, rethrow original exception with both exceptions as suppressed.
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
    }


}