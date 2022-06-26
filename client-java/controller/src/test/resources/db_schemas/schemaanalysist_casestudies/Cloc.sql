DROP TABLE metadata;

DROP TABLE t;

CREATE TABLE metadata 
(          
   timestamp varchar(50),    
   Project   varchar(50),    
   elapsed_s int
);

CREATE TABLE t        
(
   Project   varchar(50),  
   Language  varchar(50),  
   File      varchar(50),  
   nBlank    integer,  
   nComment  integer,  
   nCode     integer,  
   nScaled   int   
);
