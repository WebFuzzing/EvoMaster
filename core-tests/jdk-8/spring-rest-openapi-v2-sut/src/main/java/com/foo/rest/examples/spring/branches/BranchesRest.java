package com.foo.rest.examples.spring.branches;

import com.foo.somedifferentpackage.examples.branches.BranchesImp;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.MediaType;

@RestController
@RequestMapping(path = "/api/branches")
public class BranchesRest {


    @ApiOperation("Evaluate 'pos'")
    @RequestMapping(
            value = "/pos",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public BranchesResponseDto pos(@ApiParam("x and y inputs")
                                       @RequestBody BranchesPostDto dto){

        Branches b = new BranchesImp();
        BranchesResponseDto res = new BranchesResponseDto();
        res.value = b.pos(dto.x, dto.y);

        return res;
    }


    @ApiOperation("Evaluate 'neg'")
    @RequestMapping(
            value = "/neg",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public BranchesResponseDto neg(@ApiParam("x and y inputs")
                                       @RequestBody BranchesPostDto dto){

        Branches b = new BranchesImp();
        BranchesResponseDto res = new BranchesResponseDto();
        res.value = b.neg(dto.x, dto.y);

        return res;
    }

    @ApiOperation("Evaluate 'eq'")
    @RequestMapping(
            value = "/eq",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public BranchesResponseDto eq(@ApiParam("x and y inputs")
                                      @RequestBody BranchesPostDto dto){

        Branches b = new BranchesImp();
        BranchesResponseDto res = new BranchesResponseDto();
        res.value = b.eq(dto.x, dto.y);

        return res;
    }



}
