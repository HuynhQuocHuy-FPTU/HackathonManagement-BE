package com.hackathon.repository;

import com.hackathon.entity.CriteriaDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriteriaDetailRepository extends JpaRepository<CriteriaDetail, Integer> {
    List<CriteriaDetail> findByCriteriaSet_CriteriaSetId(Integer criteriaSetId);
}
