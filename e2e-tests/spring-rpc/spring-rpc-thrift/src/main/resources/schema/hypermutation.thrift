namespace java com.foo.rpc.examples.spring.hypermutation

struct HighWeightDto{

    1: required i32 f1,
    2: required string f2,
    3: required string f3,
    4: required double f4,
    5: required double f5,
    6: required i64 f6

}

service HypermutationService{

    string differentWeight(1: required i32 x, 2: required string y, 3: HighWeightDto z),

    string lowWeightHighCoverage(1: required i32 x, 2: required string y, 3: HighWeightDto z)
}