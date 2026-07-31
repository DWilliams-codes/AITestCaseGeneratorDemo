package com.testforge.common.workitem;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

@Service
public class WorkItemNumberService {
  @PersistenceContext private EntityManager entityManager;

  public long next() {
    Number value =
        (Number)
            entityManager
                .createNativeQuery("select nextval('testforge.work_item_number_seq')")
                .getSingleResult();
    return value.longValue();
  }
}
