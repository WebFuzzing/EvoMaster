namespace java com.foo.rpc.examples.spring.db.directint

service DbDirectIntService{

    void post(),

    i32 get(1: i32 x, 2: i32 y)
}
