package com.fashfind.fashfind.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fashfind.fashfind.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
}