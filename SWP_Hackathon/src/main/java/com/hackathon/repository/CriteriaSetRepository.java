package com.hackathon.repository;

import com.hackathon.entity.CriteriaDetail;
import com.hackathon.entity.CriteriaSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
public interface CriteriaSetRepository extends JpaRepository<CriteriaSet, Integer> {
    CriteriaSet findByCriteriaSetId(Integer criteriaSetId);
boolean existsByCriteriaSetNameAndCriteriaSetIdNot(String name, Integer criteriaSetId);
    boolean existsByCriteriaSetName(String  name);
}
