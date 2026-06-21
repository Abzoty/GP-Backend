package com.gp.GP_backend.domain.onlinecourse.repository;

import com.gp.GP_backend.domain.onlinecourse.entity.OnlineCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OnlineCourseRepository extends JpaRepository<OnlineCourse, String> {
    List<OnlineCourse> findByCourseCodeIgnoreCaseOrderByScoreDesc(String courseCode);
    List<OnlineCourse> findAllByOrderByScoreDesc();
    
    List<OnlineCourse> findByCourseCodeInOrderByScoreDesc(List<String> courseCodes);
}