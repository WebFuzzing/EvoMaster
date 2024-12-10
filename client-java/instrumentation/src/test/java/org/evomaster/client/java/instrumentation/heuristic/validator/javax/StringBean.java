package org.evomaster.client.java.instrumentation.heuristic.validator.javax;

import javax.validation.constraints.*;
import java.util.List;
import java.util.Map;

public class StringBean {

    @NotNull
    public String a;

    @Null
    public String b;

    @NotEmpty
    public String c;

    @NotBlank
    public String d;

    @Pattern(regexp = "e+")
    public String e;

    @Size(min = 2, max = 5)
    public String f;

    @Size(min = 2, max = 3)
    public List<String> g;

    @NotEmpty
    public List<String> h;

    @NotEmpty
    public String[] i;

    @Size(min = 1, max = 3)
    public Map<String,String> l;

    @Email
    public String m;

}
