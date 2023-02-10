namespace java com.foo.rpc.examples.spring.customization

struct RequestWithCombinedSeedDto {
    1: required string requestId,
    2: required string requestCode,
    3: required double value;
}

struct RequestWithSeedDto {
    1: required double value,
    2: string info
}

struct CycleADto{
    1: required string aID,
    2: CycleBDto obj
}

struct CycleBDto{
    1: required string bID,
    2: CycleADto obj
}

service CustomizationService {

    i32 handleDependent(1:RequestWithSeedDto dto),

    i32 handleCombinedSeed(1:RequestWithCombinedSeedDto dto)

    CycleADto handleCycleDto(1:CycleADto dto)

}