
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

##################################################################################################


# Folder where files like tables and graphs should be saved to
GENERATED_FILES = paste("../generated_files", sep = "")

# Where all the CSV files of the experiments are stored
DATA_DIR = paste("../results", sep = "")

# Zip file of the aggregated data
ZIP_FILE = paste(DATA_DIR, "/../", "compressedData.zip", sep = "")

# Snapshot data, at each x% search intervals, eg, default 5%
SNAP_ZIP_FILE = paste(DATA_DIR, "/../","snapshotCompressedData.zip", sep = "")


##################################################################################################
# collect all data and save to zip
init <- function(){
  k = gatherAndSaveData(DATA_DIR, ZIP_FILE)
  z = gatherAndSaveData(DATA_DIR, SNAP_ZIP_FILE, c(), "^snapshot(\\w|-|_)*\\.csv$")
}


##################################################################################################
# print some stats on the data, for quick sanity check
checkData <- function() {

  dt <- read.table(gzfile(ZIP_FILE), header = T)
  projects = sort(unique(dt$id))
  labels = sort(unique(dt$labelForExperiments))
  budgets =  sort(unique(dt$maxTime))

  cat("Total projects: ", length(projects),"\n")
  cat("Total configurations: ", length(labels),"\n")
  cat("Total runs: ", length(dt$algorithm), "\n")
  cat("Budgets: ", budgets, "\n")

  cat("\n\n-------------------------------------\n")
  for (bud in budgets) {
    cat("Budget: ", bud, "\n\n")
    budMask = dt$maxTime == bud

    for (proj in projects) {
        cat("\n\n-------------------------------------\n")
        cat("Project", proj, "\n")
        projMask = (dt$id == proj) & budMask

        for(conf in labels){
          confMask = dt$labelForExperiments == conf

          cat("CONFIGURATION:",conf,"\n")
          checkProject(dt,projMask & confMask)
        }
    }

    cat("\n\n-------------------------------------\n")
  }

}


##################################################################################################
checkProject <- function(dt,mask){

  seconds = dt$elapsedSeconds[mask]
  lines = dt$coveredLines[mask]
  faults = dt$potentialFaults[mask]
  categoryCodes = dt$potentialFaultCategories[mask]

  # each run can have several codes divided by a |, eg, "100|200|205"
  # need to extract all codes, and then take the set from all runs (ie to remove duplicates)
  codes = as.numeric(unlist(strsplit(categoryCodes,split='\\|')))
  codes = sort(codes[!duplicated(codes)]) # remove duplicates, and then sort resulting list

  cat("Time (seconds):", mean(seconds), "\n")
  cat("Evaluated actions:", mean(dt$evaluatedActions[mask]), "\n")
  cat("Runs: ", length(seconds), "\n")
  cat("Coverage (lines): ", mean(lines), "(", min(lines), "-", max(lines), ") \n")
  cat("\traw values: [",sort(lines),"]\n")
  cat("Faults (avg): ", mean(faults), " , codes: ", codes, "\n")
  cat("\n\n")
}

##################################################################################################


#################################################################################################################
# Given 2 compared algorithms/configurations, create table with rows for each SUT, and statistical analyses.
# Results are for 3 metrics: Line Coverage, Fault Detection, and Number of HTTP calls
tableTwoConfigs <- function(
    dt, # the data frame, containing all experiment data
    projects, #  list of SUT names, to include in the table (in case we want to skip some)
    xMask,  # boolean array representing a mask over 'dt', to identify all data of configuration X, used as 'base'
    yMask,  # same as yMask, but for new configuration Y compared to existing 'base' X
    xName,  # the name of X (eg used for column labels)
    yName,  # the name of Y
    tableFileName  # output file name, typically ending with ".tex"
    ) {

  TABLE = paste(GENERATED_FILES, "/", tableFileName, sep = "")
  unlink(TABLE)
  sink(TABLE, append = TRUE, split = TRUE)

    ### Utility functions for this table ###
  printAverageComparison <- function(data){
      printSummaryComparison(data, mean)
  }

  printSummaryComparison <- function(data, lambda) {
      base = data[,1]
      other = data[,2]
      A=data[,3]
      cat(" & ")
      cat(formatC(lambda(base), digits = 1, format = "f"), sep = "")
      cat(" & ")
      cat(formatC(lambda(other), digits = 1, format = "f"), sep = "")
      cat(" & ")
      cat(formatC(lambda(A), digits = 2, format = "f"), sep = "")
      cat(" & ") # no pvalue
  }

  printComparison <- function(base, other) {
      cat(" & ")
      cat(formatC(mean(base), digits = 1, format = "f"), sep = "")

      cat(" & ")
      cat(formatC(mean(other), digits = 1, format = "f"), sep = "")

      A = measureA(other, base)

      if (length(other) == 0 | length(base) == 0)
        p = 1
      else {
        w = wilcox.test(other, base)
        p = w$p.value
      }
      significant = FALSE

      if (is.nan(p)) {
        p = "1.000"
      } else if (p < 0.001) {
        significant = TRUE
        p = "$< 0.001$"
      } else {
        if(p <= 0.05){
          significant = TRUE
        }
        p = formatC(p, digits = 3, format = "f")
      }

      cat(" & ")
      if(significant)
        cat("{\\bf ",sep="")
      cat(formatC(A, digits = 2, format = "f"), sep = "")
      if(significant)
        cat("}",sep="")

      cat(" & ")
      cat(p)
  }

  getOverhead <- function(base,other){
    # return ( ((mean(base) - mean(other)) / mean(base) * 100) )
    return ( ( mean(other) / mean(base) * 100) )
  }

  printComparisonOverhead <- function(base, other) {
    cat(" & ")
    cat(formatC(mean(base), digits = 0, format = "f"), sep = "")

    cat(" & ")
    cat(formatC(mean(other), digits = 0, format = "f"), sep = "")

    diff =  getOverhead(base,other)

    cat(" & ")
    # if(!is.nan(diff) & diff > 0 ){
    #   cat("+")
    # }
    cat(formatC(diff, digits = 2, format = "f"),"\\\\%", sep = "")
  }

  printSummaryComparisonOverhead <- function(data,lambda) {
    base  = data[,1]
    other = data[,2]
    diff  = data[,3]
    cat(" & ")
    cat(formatC(lambda(base), digits = 0, format = "f"), sep = "")
    cat(" & ")
    cat(formatC(lambda(other), digits = 0, format = "f"), sep = "")
    cat(" & ")
    md = lambda(diff)
    # if(md > 0){
    #   cat("+")
    # }
    cat(formatC(md, digits = 2, format = "f"),"\\\\%", sep = "")
  }


  ##################################


  tryCatch({
      cat("\\begin{tabular}{ l rrrr rrrr rrr}\\\\ \n")
      cat("\\toprule \n")
      cat("SUT & \\multicolumn{4}{c}{Line Coverage \\%} & \\multicolumn{4}{c}{\\# Detected Faults} & \\multicolumn{3}{c}{\\# HTTP Calls} \\\\ \n")
      cat("    & ",xName," & ",yName,"  & $\\hat{A}_{12}$ & p-value  & ",xName," & ",yName, " & $\\hat{A}_{12}$ & p-value & ",xName," & ",yName," & Ratio \\\\% \\\\ \n", sep = "")
      cat("\\midrule \n")

      avgLines = matrix(nrow = length(projects), ncol = 3) # X, Y and A12
      avgFaults = matrix(nrow = length(projects), ncol = 3) # X, Y and A12
      avgOverhead = matrix(nrow = length(projects), ncol = 3) # X, Y and Overhead

      for (i in 1:length(projects)) {
        proj = projects[[i]]
        cat("\\emph{", proj, "}", sep = "")

        projectMask =  dt$id == proj
        baseMask = xMask
        otherMask = yMask

        base = dt$coveredLines[projectMask & baseMask]
        other = dt$coveredLines[projectMask & otherMask]
        tot = max(dt$numberOfLines[dt$id == proj])
        base = 100 * (base / tot)
        other = 100 * (other / tot)
        avgLines[i, 1] = mean(base)
        avgLines[i, 2] = mean(other)
        avgLines[i, 3] = measureA(other, base)
        printComparison(base, other)

        base = dt$potentialFaults[projectMask & baseMask]
        other = dt$potentialFaults[projectMask & otherMask]
        avgFaults[i, 1] = mean(base)
        avgFaults[i, 2] = mean(other)
        avgFaults[i, 3] = measureA(other, base)
        printComparison(base, other)

        base = dt$evaluatedActions[projectMask & baseMask]
        other = dt$evaluatedActions[projectMask & otherMask]
        avgOverhead[i, 1] = mean(base)
        avgOverhead[i, 2] = mean(other)
        avgOverhead[i, 3] = getOverhead(base,other)
        printComparisonOverhead(base, other)

        cat(" \\\\ \n")
    }
    cat("\\midrule \n")
    cat("Average ")
    printAverageComparison(avgLines)
    printAverageComparison(avgFaults)
    printSummaryComparisonOverhead(avgOverhead,mean)
    cat(" \\\\ \n")

    cat("Median ")
    printSummaryComparison(avgLines,median)
    printSummaryComparison(avgFaults,median)
    printSummaryComparisonOverhead(avgOverhead,median)
    cat(" \\\\ \n")

    cat("\\bottomrule \n")
    cat("\\end{tabular} \n")
  }, finally={
    sink()
  }
  #,
#   error=function(cond) {
#     return(NA)
#   }
  )
}
#################################################################################################################
#################################################################################################################


#################################################################################################################


timeGraphs <- function(
  dt, # the snapshot data frame, containing all experiment data
  projects, #  list of SUT names (in case we want to skip some), for each a graph will be created
  criterion, # one of: LINES, BRANCHES, FAULTS
  xMask,  # boolean array representing a mask over 'dt', to identify all data of configuration X, used as 'base'
  yMask,  # same as yMask, but for new configuration Y compared to existing 'base' X
  xName,  # the name of X
  yName,  # the name of Y
  filePrefixName  # prefix output file name
) {

  targets = sort(unique(dt$interval))
  z = length(targets)

  for (i in 1:length(projects)) {
    proj = projects[[i]]
    projectMask = dt$id == proj

    BASE = rep(0, times = z)
    OTHER = rep(0, times = z)

    for (j in 1:z) {
      targetMask = dt$interval == targets[[j]] & projectMask

      if(criterion=="LINES"){
        tot = max(dt$numberOfLines[targetMask])
        BASE[[j]] = 100 * mean(dt$coveredLines[targetMask & xMask]) / tot
        OTHER[[j]] = 100 * mean(dt$coveredLines[targetMask & yMask]) / tot
      } else if(criterion=="BRANCHES"){
        tot = max(dt$numberOfBranches[targetMask])
        BASE[[j]] = 100 * mean(dt$coveredBranches[targetMask & xMask]) / tot
        OTHER[[j]] = 100 * mean(dt$coveredBranches[targetMask & yMask]) / tot
      } else if(criterion=="FAULTS"){
        BASE[[j]] = mean(dt$potentialFaults[targetMask & xMask])
        OTHER[[j]] = mean(dt$potentialFaults[targetMask & yMask])
      } else {
        stop("Not recongized criterion: " + x)
      }
    }

    plot_colors = c("blue", "red")
    line_width = 2

    tryCatch({
      pdf(paste(GENERATED_FILES, "/",filePrefixName,"_",criterion,"_",proj,".pdf", sep = ""))

      yMin = min(BASE, OTHER)
      yMax = max(BASE, OTHER)

      plot(BASE, ylim = c(yMin, yMax), type = "o", col = plot_colors[[1]], pch = 21, lty = 1, lwd = line_width,
         ylab = criterion, xlab = "Budget Percentage", xaxt = "n", main=proj)
      axis(side = 1, labels = targets, at = 1:z)

      lines(OTHER, type = "o", col = plot_colors[[2]], pch = 22, lty = 2, lwd = line_width)

      lx = 15
      ly = yMin + 0.2 * (yMax - yMin)

      legend(lx, ly, c(xName, yName), cex = 1.2, col = plot_colors, pch = 21:22, lty = 1:2)

    }, finally={
      dev.off()
    }
    )
  }
}






#################################################################################################################
#################################################################################################################
# create all tables and figures for this analysis
all <- function(){

  # configuration labels, as present in labelForExperimentConfigs
  xLabel = "CHANGEME"
  yLabel = "CHANGEME"
  # human-readable names
  xName = "CHANGEME"
  yName = "CHANGEME"


  ### 2-algorithms comparison table
  dt <- read.table(gzfile(ZIP_FILE), header = T)
  projects = sort(unique(dt$id))
  xMask = dt$labelForExperimentConfigs == xLabel
  yMask = dt$labelForExperimentConfigs == yLabel
  tableFileName = paste("comparisons",".tex", sep="")
  tableTwoConfigs(dt, projects, xMask, yMask, xName, yName, tableFileName)

  ### timegraph figures for each SUT
  dt <- read.table(gzfile(SNAP_ZIP_FILE), header = T)
  projects = sort(unique(dt$id))
  xMask = dt$labelForExperimentConfigs == xLabel
  yMask = dt$labelForExperimentConfigs == yLabel
  filePrefixName = paste("timegraph",sep="")

  timeGraphs(dt, projects, "LINES", xMask, yMask, xName, yName, filePrefixName)
  timeGraphs(dt, projects, "FAULTS", xMask, yMask, xName, yName, filePrefixName)
}

