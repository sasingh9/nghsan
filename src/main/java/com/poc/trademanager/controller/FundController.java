package com.poc.trademanager.controller;

import com.poc.trademanager.entity.Fund;
import com.poc.trademanager.service.FundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funds")
public class FundController {

    @Autowired
    private FundService fundService;

    @GetMapping
    public List<Fund> getAllFunds() {
        return fundService.getAllFunds();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Fund> getFundById(@PathVariable String id) {
        return fundService.getFundById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Fund createFund(@RequestBody Fund fund) {
        return fundService.createFund(fund);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Fund> updateFund(@PathVariable String id, @RequestBody Fund fundDetails) {
        return fundService.updateFund(id, fundDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFund(@PathVariable String id) {
        if (fundService.deleteFund(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
