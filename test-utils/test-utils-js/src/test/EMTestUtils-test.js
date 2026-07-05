 const EMTestUtils = require("../main/resources/EMTestUtils");


 test('should create string with prefix, postfix, and length constraints', () => {
     const prefix = "foo";
     const postfix = "bar";
     const min = 5;
     const max = 10;

     const first = EMTestUtils.createString(min, max, prefix, postfix);
     expect(first.startsWith(prefix)).toBe(true);
     expect(first.endsWith(postfix)).toBe(true);
     expect(first.length).toBeGreaterThanOrEqual(min);
     expect(first.length).toBeLessThanOrEqual(max);

     const second = EMTestUtils.createString(min, max, prefix, postfix);
     expect(second.startsWith(prefix)).toBe(true);
     expect(second.endsWith(postfix)).toBe(true);
     expect(second.length).toBeGreaterThanOrEqual(min);
     expect(second.length).toBeLessThanOrEqual(max);

     expect(first).not.toEqual(second);
 })


test("testResolveLocation_direct", () => {

    const template = "http://localhost:12345/a/{id}";
    const location = "/a/5";

    const res = EMTestUtils.resolveLocation(location, template);
    expect(res).toBe("http://localhost:12345/a/5");
});

test("testResolveLocation_indirect", () => {

    const template = "http://localhost:12345/a/{id}/x";
    const location = "/a/5";

    const res = EMTestUtils.resolveLocation(location, template);
    expect(res).toBe("http://localhost:12345/a/5/x");
});

test("testResolveLocation_fullURI_different_indirect", () => {
    const template = "http://localhost:12345/a/{id}/x";
    const location = "https://127.0.0.1:80/a/5";

    const res = EMTestUtils.resolveLocation(location, template);
    expect(res).toBe("https://127.0.0.1:80/a/5/x");
});


test("testGivenAnInvalidLocationHeaderWhenResolveLocationThenTheExpectedTemplateIsReturned", () => {
    const template = "http://localhost:12345/a/x";
    const location = "/a/\"52\"";

    const res = EMTestUtils.resolveLocation(location, template);

    //TODO yet another difference between Java and JS handling of URIs... should check specs to see which one is correct
    expect(res).toBe("http://localhost:12345/a/%2252%22");
});

test("testIsValidURI", () => {

    expect(EMTestUtils.isValidURIorEmpty(null)).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("    ")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("a")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("/a")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("/a/b")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("/a/b/c?k=4&z=foo")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("http://foo.org/a")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("https://127.0.0.1:443")).toBe(true);

    //this should fail, as "{}" are invalid chars
    //TODO here Java and JS libraries do differ... should check specs to see which one is correct
    expect(EMTestUtils.isValidURIorEmpty("/{a}")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("--://///{a}")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("http://foo.org/#")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("http://foo.org/#a")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("http://foo.org/|foo")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("http://example.com/?key=value&")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("http://example.com/#fragment?query=value")).toBe(true);
    expect(EMTestUtils.isValidURIorEmpty("http://example.com:port")).toBe(false);

});

test("testResolveLocation_null", () => {
    const template = "http://localhost:12345/a/x";
    const location= null;

    const res = EMTestUtils.resolveLocation(location, template);
    expect(res).toBe(template);
});

