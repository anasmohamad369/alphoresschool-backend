package com.lumo.backend.timetable.repository;

import com.lumo.backend.timetable.entity.Timetable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findBySchoolClassIdAndSectionId(Long classId, Long sectionId);
    List<Timetable> findBySchoolClassId(Long classId);

    @Query("SELECT t FROM Timetable t LEFT JOIN t.section s WHERE t.schoolClass.id = :classId AND (s.id = :sectionId OR s IS NULL)")
    List<Timetable> findBySchoolClassIdAndSectionIdOrSectionIsNull(@Param("classId") Long classId, @Param("sectionId") Long sectionId);

    List<Timetable> findBySchoolClassIdAndSectionIsNull(Long classId);
}
