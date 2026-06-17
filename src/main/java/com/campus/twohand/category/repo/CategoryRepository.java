package com.campus.twohand.category.repo;

import com.campus.twohand.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * 分类数据访问层
 */
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    /**
     * 查询所有分类
     * 按 sort 升序
     * 再按 id 升序
     *
     * 这是 Spring Data JPA 的方法命名规则
     * 它会自动生成 SQL：
     * SELECT * FROM category ORDER BY sort ASC, id ASC
     */
    List<Category> findAllByOrderBySortAscIdAsc();

//    管理员
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Category> findByName(String name);

    Optional<Category> findTopByOrderBySortDescIdDesc();
}

