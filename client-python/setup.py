import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="evomaster-client-axelmaddonni",
    version="0.0.1",
    author="Axel Maddonni",
    author_email="axel.maddonni@gmail.com",
    description="EvoMaster client for Python Flask RESTful applications",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/axelmaddonni/EvoMaster",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
        # https://pypi.org/classifiers/
    ],
    python_requires='>=3.8',
)
