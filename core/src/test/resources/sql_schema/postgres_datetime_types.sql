CREATE TABLE DatetimeTypes (
           timestampColumn timestamp (8) without time zone NOT NULL,
           timestampWithTimeZoneColumn timestamp(8) with time zone NOT NULL,
           dateColumn date NOT NULL,
           timeColumn time(8) without time zone NOT NULL,
           timeWithTimeZoneColumn time(8) with time zone NOT NULL,
           intervalColumn interval(16) NOT NULL
);

