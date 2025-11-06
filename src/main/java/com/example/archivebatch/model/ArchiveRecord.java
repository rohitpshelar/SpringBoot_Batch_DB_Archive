package com.example.archivebatch.model;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "my_table")
@Data
public class ArchiveRecord {
    @Id
    private Long id;
    private String data;
    private LocalDateTime archivedAt;
}