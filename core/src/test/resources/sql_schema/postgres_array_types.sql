CREATE TABLE ArrayTypes (
                            nonArrayColumn integer NOT NULL,
                            arrayColumn integer[] NOT NULL,
                            matrixColumn integer[][] NOT NULL,
                            spaceColumn integer[][][] NOT NULL,
                            manyDimensionsColumn integer[][][][] NOT NULL,
                            exactSizeArrayColumn integer[3] NOT NULL,
                            exactSizeMatrixColumn integer[3][3] NOT NULL
);

CREATE TABLE NullableArrayTable (
    nullableArrayColumn integer[]
);

CREATE TABLE StringArrayTable (
    stringArrayColumn varchar[] NOT NULL
);