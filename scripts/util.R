
# Recursively search for all files given the [filePattern] into the given [directory].
# Return a new single table in which all such data is combined together.
# You might specify some columns with [ignoreColumns] if you need to skip them.
gatherAllTables <- function(directory,ignoreColumns=NULL,filePattern="^statistics(\\w|-|_|\\.)*\\.csv$"){
	allTables = NULL

	for(table in list.files(directory,recursive=TRUE,full.names=TRUE,pattern=filePattern) ){

		cat("Reading: ",table,"\n")

		tryCatch( {dt <- read.csv(table,header=T)} ,
				error = function(e){
					cat("Error in reading table ",table,"\n", paste(e), "\n")
				})

		if(! is.null(ignoreColumns)){
			for(name in ignoreColumns){
				dt[name] = NULL
			}
		}

		if(is.null(allTables)){
			allTables = dt
		} else {
			tryCatch( {allTables = rbind(allTables,dt)} ,
					error = function(e){
						cat("Error in concatenating table ",table,"\n", paste(e), "\n")
					})
		}
	}
	return(allTables)
}

# Recursively gather all data from [directory] given the [filePattern] name.
# Output such data in a zipped file with path [zipFile].
gatherAndSaveData <- function(directory,zipFile,ignoreColumns=NULL,filePattern="^statistics(\\w|-|_|\\.)*\\.csv$"){
	cat("Loading data...",date(),"\n")

	dt = gatherAllTables(directory,ignoreColumns,filePattern)

	cat("Data is loaded. Starting compressing. ",date(),"\n")

	write.table(dt, file = gzfile(zipFile))

	cat("Data is compressed and saved. Starting reading back. ",date(),"\n")

	table <- read.table(gzfile(zipFile),header=T)

	cat("Data read back. Done! ",date(),"\n")

	return(table)
}

### return a boolean vector, where each position in respect to x is true if that element appear in y
areInTheSubset <- function(x,y){

	### first consider vector with all FALSE
	result = x!=x
	for(k in y){
		result = result | x==k
	}
	return(result)
}


### Compute Vargha-Delaney A12 statistics
measureA <- function(a,b){

	if(length(a)==0 & length(b)==0){
		return(0.5)
	} else if(length(a)==0){
		## motivation is that we have no data for "a" but we do for "b".
		## maybe the process generating "a" always fail (eg out of memory)
		return(0)
	} else if(length(b)==0){
		return(1)
	}

	r = rank(c(a,b))
	r1 = sum(r[seq_along(a)])

	m = length(a)
	n = length(b)
	A = (r1/m - (m+1)/2)/n

	return(A)
}