package com.foo.rpc.examples.spring.branches;

import com.foo.somedifferentpackage.examples.branches.BranchesImp;
import org.apache.thrift.TException;
import org.evomaster.client.java.instrumentation.example.branches.Branches;
import org.springframework.stereotype.Service;

@Service
public class BranchesServiceImp implements BranchesService.Iface {
    @Override
    public BranchesResponseDto pos(BranchesPostDto dto) throws TException {
        Branches b = new BranchesImp();
        BranchesResponseDto res = new BranchesResponseDto();
        res.value = b.pos(dto.x, dto.y);

        return res;
    }

    @Override
    public BranchesResponseDto neg(BranchesPostDto dto) throws TException {
        Branches b = new BranchesImp();
        BranchesResponseDto res = new BranchesResponseDto();
        res.value = b.neg(dto.x, dto.y);

        return res;
    }

    @Override
    public BranchesResponseDto eq(BranchesPostDto dto) throws TException {
        Branches b = new BranchesImp();
        BranchesResponseDto res = new BranchesResponseDto();
        res.value = b.eq(dto.x, dto.y);

        return res;
    }
}
