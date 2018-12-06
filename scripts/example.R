### Example of data analysis.
### This was based on what actually done for the paper:
### "RESTful API Automated Test Case Generation with EvoMaster", TOSEM'18

# Need to import the util.R file
source("util.R")

# Folder where files like tables and graphs should be saved to
GENERATED_FILES = paste("../generated_files", sep = "")

# Where all the CSV files of the experiments are stored
DATA_DIR = paste("../results", sep = "")

# Zip file of the aggregated data
ZIP_FILE = paste(DATA_DIR, "/", "compressedData.zip", sep = "")


# collect all data and save to zip
init <- function(){
    k = gatherAndSaveData(DATA_DIR, ZIP_FILE, c("maxResponseByteSize"))
}

# check how long the experiments were running
checkTime <- function(){


    dt <- read.table(gzfile(ZIP_FILE), header = T)

    cat("Total runs: ", length(dt$elapsedSeconds), "\n\n")

    budjets = sort(unique(dt$maxActionEvaluations))

    for (bud in budjets) {
        avgmin = mean(dt$elapsedSeconds[dt$maxActionEvaluations==bud]) / 60
        cat("Budget: ", bud, "avg minutes =", avgmin, "\n")
    }

    cat("Total days (not counting per CPU)", sum(dt$elapsedSeconds)/ (60 * 60 * 24),"\n")
    cat("Total days times 3 (ie per CPU): ", 3 * sum(dt$elapsedSeconds) / (60 * 60 * 24),"\n")
}


# print some stats on the data
checkData <- function(name){

    dt <- read.table(gzfile(ZIP_FILE), header = T)

    cat("Total runs: ", length(dt$algorithm), "\n\n")

    projects = sort(unique(dt$id))
    budjets = sort(unique(dt$maxActionEvaluations))
    smart = sort(unique(dt$probOfSmartSampling))

    cat("\n\n-------------------------------------\n")
    for (bud in budjets) {
        cat("Budget: ", bud, "\n\n")

        for (proj in projects) {
            cat("Project", proj, "\n")
            mask = dt$id == proj & dt$maxActionEvaluations == bud

            seconds = dt$elapsedSeconds[mask]
            cat("Time (seconds):", mean(seconds), "\n")
            cat("Runs: ", length(seconds), "\n")

            for (s in smart) {
                smartMask = mask & dt$probOfSmartSampling == s
                cat("Runs for probOfSmartSampling", s, ": ", length(dt$probOfSmartSampling[smartMask]), "\n")
                cat("Coverage: ", mean(dt$coveredTargets[smartMask]), "\n")
                cat("5xx: ",mean(dt$errors5xx[smartMask]), "\n")
            }
            cat("\n\n")
        }

        cat("\n\n-------------------------------------\n")
    }
}


# For a paper we might need to prepare several tables and graphs.
# It is good to have a single function that does call all the others.
allTables <- function(){
    tableFaults()
    tableScore()
    tableScore5xx()
    tableComparison()
    tableRelative()
}


tableFaults <- function(){
    dt <- read.table(gzfile(ZIP_FILE),header=T)

    TABLE = paste(GENERATED_FILES,"/tableFaults.tex",sep="")
    unlink(TABLE)
    sink(TABLE, append=TRUE, split=TRUE)

    cat("\\begin{tabular}{ l r r rr  } \n")
    cat("\\toprule \n")
    cat("SUT & \\#Tests & \\# Codes & \\multicolumn{2}{c}{\\#5xx}  \\\\ \n")
    cat(" & & & Avg. & Max   \\\\ \n")
    cat("\\midrule \n")

    projects = sort(unique(dt$id))
    budget = max(dt$maxActionEvaluations)
    P = 0.6
    tuningMask = dt$maxActionEvaluations == budget & dt$probOfSmartSampling==P

    for(proj in projects){

        cat("\\emph{",proj,"}", sep="")

        mask = dt$id==proj & tuningMask

        cat(" & ")
        cat(formatC(mean(dt$generatedTests[mask]),digits=1,format="f"),sep="")

        cat(" & ")
        cat(formatC(mean(dt$avgReturnCodes[mask]),digits=1,format="f"),sep="")
        # cat(" & ")
        # cat(max(dt$maxReturnCodes[mask]))

        cat(" & ")
        cat(formatC(mean(dt$errors5xx[mask]),digits=1,format="f"),sep="")
        cat(" & ")
        cat(max(dt$errors5xx[mask]))

        cat("\\\\ \n")
    }

    cat("\\bottomrule \n")
    cat("\\end{tabular} \n")

    sink()
}


tableComparison <- function(){

    dt <- read.table(gzfile(ZIP_FILE), header = T)

    TABLE = paste(GENERATED_FILES, "/tableComparison.tex", sep = "")
    unlink(TABLE)
    sink(TABLE, append = TRUE, split = TRUE)

    P = 0.6

    cat("\\begin{tabular}{ l r r r r r }\\\\ \n")
    cat("\\toprule \n")
    cat("SUT & Budget & Base & $P=", P,"$ & $\\hat{A}_{12}$ & p-value \\\\ \n", sep="")

    projects = sort(unique(dt$id))
    budgets = sort(unique(dt$maxActionEvaluations))

    for (proj in projects) {

        projectMask = dt$id == proj

        cat("\\midrule \n")

        for (i in 1 : length(budgets)) {

            if (i == 1) {
                cat("\\emph{", proj, "}", sep = "")
            }

            cat(" & ")
            cat((budgets[i] / 1000), "k", sep="")

            mask = projectMask & dt$maxActionEvaluations == budgets[i]

            base = dt$coveredTargets[mask & dt$probOfSmartSampling==0]
            smart = dt$coveredTargets[mask & dt$probOfSmartSampling==P]

            cat(" & ")
            cat(formatC(mean(base), digits = 1, format = "f"), sep = "")

            cat(" & ")
            cat(formatC(mean(smart), digits = 1, format = "f"), sep = "")

            A = measureA(smart, base)

            cat(" & ")
            cat(formatC(A, digits = 2, format = "f"), sep = "")

            w = wilcox.test(smart,base)
            p = w$p.value

            if(p < 0.001){
                p = "$< 0.001$"
            } else {
                p = formatC(p, digits = 3, format = "f")
            }

            cat(" & ")
            cat(p)

            cat(" \\\\ \n")
        }
    }

    cat("\\bottomrule \n")
    cat("\\end{tabular} \n")

    sink()
}


tableScore <- function(){
    dt <- read.table(gzfile(ZIP_FILE), header = T)

    TABLE = paste(GENERATED_FILES, "/tableScore.tex", sep = "")
    unlink(TABLE)
    sink(TABLE, append = TRUE, split = TRUE)

    smarts = sort(unique(dt$probOfSmartSampling[dt$probOfSmartSampling != 0]))
    smartCol = rep("r", length(smarts))


    cat("\\begin{tabular}{ l r | r | ", smartCol, " }\\\\ \n")
    cat("\\toprule \n")
    cat("SUT & Budget & Base & \\multicolumn{", length(smarts), "}{c}{Smart Sampling Probability}  \\\\ \n")
    cat("    &        &  0   ")
    for(s in smarts ){
        cat(" & ", s)
    }
    cat(" \\\\ \n")

    projects = sort(unique(dt$id))
    budgets = sort(unique(dt$maxActionEvaluations))

    for (proj in projects) {

        projectMask = dt$id == proj

        cat("\\midrule \n")

        for (i in 1 : length(budgets)) {
            if (i == 1) {
                cat("\\emph{", proj, "}", sep = "")
            }

            cat(" & ")
            cat((budgets[i] / 1000), "k", sep="")

            mask = projectMask & dt$maxActionEvaluations == budgets[i]

            base = mean(dt$coveredTargets[mask & dt$probOfSmartSampling==0])
            k = c()
            for(s in smarts){
                cov = mean(dt$coveredTargets[mask & dt$probOfSmartSampling==s])
                k = c(k, cov)
            }

            best = max(base, k)

            cat(" & ")
            if(base == best) cat("{\\bf ")
            cat(formatC(base, digits = 1, format = "f"), sep = "")
            if(base == best) cat("}")

            for(s in smarts){
                cat(" & ")
                cov = mean(dt$coveredTargets[mask & dt$probOfSmartSampling==s])
                if(cov == best) cat("{\\bf ")
                cat(formatC(cov, digits = 1, format = "f"), sep = "")
                if(cov == best) cat("}")
            }

            cat("\\\\ \n")
        }
    }

    cat("\\bottomrule \n")
    cat("\\end{tabular} \n")

    sink()
}

tableScore5xx <- function(){
    dt <- read.table(gzfile(ZIP_FILE), header = T)

    TABLE = paste(GENERATED_FILES, "/tableScore5xx.tex", sep = "")
    unlink(TABLE)
    sink(TABLE, append = TRUE, split = TRUE)

    smarts = sort(unique(dt$probOfSmartSampling[dt$probOfSmartSampling != 0]))
    smartCol = rep("r", length(smarts))


    cat("\\begin{tabular}{ l r | r | ", smartCol, " }\\\\ \n")
    cat("\\toprule \n")
    cat("SUT & Budget & Base & \\multicolumn{", length(smarts), "}{c}{Smart Sampling Probability}  \\\\ \n")
    cat("    &        &  0   ")
    for(s in smarts ){
        cat(" & ", s)
    }
    cat(" \\\\ \n")

    projects = sort(unique(dt$id))
    budgets = sort(unique(dt$maxActionEvaluations))

    for (proj in projects) {

        projectMask = dt$id == proj

        cat("\\midrule \n")

        for (i in 1 : length(budgets)) {
            if (i == 1) {
                cat("\\emph{", proj, "}", sep = "")
            }

            cat(" & ")
            cat((budgets[i] / 1000), "k", sep="")

            mask = projectMask & dt$maxActionEvaluations == budgets[i]

            base = mean(dt$errors5xx[mask & dt$probOfSmartSampling==0])
            k = c()
            for(s in smarts){
                cov = mean(dt$errors5xx[mask & dt$probOfSmartSampling==s])
                k = c(k, cov)
            }

            best = max(base, k)

            cat(" & ")
            if(base == best) cat("{\\bf ")
            cat(formatC(base, digits = 1, format = "f"), sep = "")
            if(base == best) cat("}")

            for(s in smarts){
                cat(" & ")
                cov = mean(dt$errors5xx[mask & dt$probOfSmartSampling==s])
                if(cov == best) cat("{\\bf ")
                cat(formatC(cov, digits = 1, format = "f"), sep = "")
                if(cov == best) cat("}")
            }

            cat("\\\\ \n")
        }
    }

    cat("\\bottomrule \n")
    cat("\\end{tabular} \n")

    sink()
}



tableRelative <- function(){
    dt <- read.table(gzfile(ZIP_FILE), header = T)

    TABLE = paste(GENERATED_FILES, "/tableRelative.tex", sep = "")
    unlink(TABLE)
    sink(TABLE, append = TRUE, split = TRUE)

    smarts = sort(unique(dt$probOfSmartSampling[dt$probOfSmartSampling != 0]))
    smartCol = rep("r", length(smarts))


    cat("\\begin{tabular}{ l r |  ", smartCol, " }\\\\ \n")
    cat("\\toprule \n")
    cat("SUT & Budget & \\multicolumn{", length(smarts), "}{c}{Smart Sampling Probability}  \\\\ \n")
    cat("    &           ")
    for(s in smarts ){
        cat(" & ", s)
    }
    cat(" \\\\ \n")

    projects = sort(unique(dt$id))
    budgets = sort(unique(dt$maxActionEvaluations))

    data = matrix(nrow = (length(budgets)*length(projects)), ncol= length(smarts))
    counter = 1

    for (proj in projects) {

        projectMask = dt$id == proj

        cat("\\midrule \n")

        for (i in 1 : length(budgets)) {
            if (i == 1) {
                cat("\\emph{", proj, "}", sep = "")
            }

            cat(" & ")
            cat((budgets[i] / 1000), "k", sep="")

            mask = projectMask & dt$maxActionEvaluations == budgets[i]


            base = mean(dt$coveredTargets[mask & dt$probOfSmartSampling==0])
            k = c()
            for(s in smarts){
                cov = mean(dt$coveredTargets[mask & dt$probOfSmartSampling==s])
                k = c(k, cov)
            }

            best = max(base, k)

            rels = c()

            for(s in smarts){
                cat(" & ")
                cov = mean(dt$coveredTargets[mask & dt$probOfSmartSampling==s])
                if(cov == best) cat("{\\bf ")
                rel = 100 * (cov - base) / base
                if(rel > 0) cat("+")
                cat(formatC(rel, digits = 1, format = "f"), "\\%", sep = "")
                if(cov == best) cat("}")

                rels = c(rels, rel)
            }

            data[counter, ] = rels
            counter = counter + 1

            cat("\\\\ \n")
        }
    }

    cat("\\midrule \n")
    cat("Average &  ")
    rels = c()
    for(i in 1:length(data[1,])){
        rels[[i]] = mean(data[, i])
    }
    best = max(rels)

    for(rel in rels){
        cat(" & ")
        if(rel == best) cat("{\\bf ")
        if(rel > 0) cat("+")
        cat(formatC(rel, digits = 1, format = "f"), "\\%", sep = "")
        if(rel == best) cat("}")
    }
    cat(" \\\\ \n")

    cat("\\bottomrule \n")
    cat("\\end{tabular} \n")

    sink()
}
