package com.hackathon.repository;

import com.hackathon.entity.CriteriaDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface CriteriaDetailRepository extends JpaRepository<CriteriaDetail, Integer> {
    List<CriteriaDetail> findByCriteriaSet_CriteriaSetId(Integer criteriaSetId);
}
