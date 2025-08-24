package com.poc.trademanager.service;

import com.poc.trademanager.entity.Fund;
import com.poc.trademanager.repository.FundRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FundService {

    @Autowired
    private FundRepository fundRepository;

    public List<Fund> getAllFunds() {
        return fundRepository.findAll();
    }

    public Optional<Fund> getFundById(String id) {
        return fundRepository.findById(id);
    }

    public Fund createFund(Fund fund) {
        fund.setLastUpdatedDate(LocalDateTime.now());
        // In a real application, LastUpdatedBy would be set based on the logged-in user
        fund.setLastUpdatedBy("system");
        return fundRepository.save(fund);
    }

    public Optional<Fund> updateFund(String id, Fund fundDetails) {
        return fundRepository.findById(id)
                .map(fund -> {
                    fund.setFundName(fundDetails.getFundName());
                    fund.setFundTicker(fundDetails.getFundTicker());
                    fund.setIsin(fundDetails.getIsin());
                    fund.setFundType(fundDetails.getFundType());
                    fund.setLegalStructure(fundDetails.getLegalStructure());
                    fund.setDomicile(fundDetails.getDomicile());
                    fund.setInceptionDate(fundDetails.getInceptionDate());
                    fund.setFiscalYearEnd(fundDetails.getFiscalYearEnd());
                    fund.setBaseCurrency(fundDetails.getBaseCurrency());
                    fund.setManagementFee(fundDetails.getManagementFee());
                    fund.setPerformanceFee(fundDetails.getPerformanceFee());
                    fund.setFundAdministrator(fundDetails.getFundAdministrator());
                    fund.setCustodian(fundDetails.getCustodian());
                    fund.setPrimeBrokers(fundDetails.getPrimeBrokers());
                    fund.setInvestmentStrategy(fundDetails.getInvestmentStrategy());
                    fund.setValuationFrequency(fundDetails.getValuationFrequency());
                    fund.setSubscriptionCycle(fundDetails.getSubscriptionCycle());
                    fund.setRedemptionCycle(fundDetails.getRedemptionCycle());
                    fund.setNav(fundDetails.getNav());
                    fund.setNavDate(fundDetails.getNavDate());
                    fund.setStatus(fundDetails.getStatus());
                    fund.setLastUpdatedBy("system"); // Or the current user
                    fund.setLastUpdatedDate(LocalDateTime.now());
                    return fundRepository.save(fund);
                });
    }

    public boolean deleteFund(String id) {
        return fundRepository.findById(id)
                .map(fund -> {
                    fundRepository.delete(fund);
                    return true;
                })
                .orElse(false);
    }
}
