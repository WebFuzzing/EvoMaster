package com.foo.rpc.examples.spring.authsetup;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthSetupServiceImp implements AuthSetupService.Iface{
    private static boolean login = true;

    private final Map<String, String> auth = new HashMap<String, String>(){{
        put("foo", "zXQV47zsrjfJRnTD");
        put("bar", "5jbNvXvaejDG5MhS");
    }};

    @Override
    public String access() throws TException {
        if (login)
            return "HELLO";
        return "SORRY";
    }

    @Override
    public void login(LoginDto dto) throws TException {
        login = dto!=null && dto.id != null && dto.passcode != null && auth.get(dto.id).equals(dto.passcode);
    }

    @Override
    public void logout() throws TException {
        login = false;
    }
}
