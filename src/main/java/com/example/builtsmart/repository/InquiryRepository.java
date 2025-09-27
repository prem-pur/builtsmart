package com.example.builtsmart.repository;

import com.example.builtsmart.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByClientEmailOrderByCreatedAtDesc(String clientEmail);
}
