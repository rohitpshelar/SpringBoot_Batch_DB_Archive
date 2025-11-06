package com.example.archivebatch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.archivebatch.model.MainRecord;

@Repository
public interface MainRecordRepository extends JpaRepository<MainRecord, Long> {
}