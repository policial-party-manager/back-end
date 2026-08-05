package sicau.policialPartyManager.service;

import sicau.policialPartyManager.entity.Branch;

import java.util.List;

public interface BranchService {

    List<Branch> listAll();

    Branch getById(Long id);

    void save(Branch branch);

    void update(Branch branch);

    void delete(Long id);
}
