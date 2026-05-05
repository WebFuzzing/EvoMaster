// Dummy file to force binary wheel... but still it has compile properly
#include <Python.h>

static PyMethodDef Methods[] = {
    {NULL, NULL, 0, NULL}
};

static struct PyModuleDef module = {
    PyModuleDef_HEAD_INIT,
    "foo",
    NULL,
    -1,
    Methods
};

PyMODINIT_FUNC PyInit_foo(void) {
    return PyModule_Create(&module);
}