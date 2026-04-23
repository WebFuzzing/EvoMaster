namespace java com.foo.rpc.examples.spring.branches

struct BranchesResponseDto {
    1: i32 value
}

struct BranchesPostDto {
    1: i32 x,
    2: i32 y
}

service BranchesService {

    BranchesResponseDto pos(1:BranchesPostDto dto),

    BranchesResponseDto neg(1:BranchesPostDto dto),

    BranchesResponseDto eq(1:BranchesPostDto dto)
}