package com.hackathon.repository;

import com.hackathon.entity.CriteriaDetail;
import com.hackathon.entity.CriteriaSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CriteriaSetRepository extends JpaRepository<CriteriaSet, Integer> {
    CriteriaSet findByCriteriaSetId(Integer criteriaSetId);

    boolean existsByCriteriaSetName(String  name);
}
